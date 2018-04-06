/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.manager

import com.toshi.crypto.HDWallet
import com.toshi.manager.network.IdInterface
import com.toshi.manager.network.IdService
import com.toshi.model.local.User
import com.toshi.model.network.ServerTime
import com.toshi.model.network.UserDetails
import com.toshi.util.logging.LogUtil
import com.toshi.util.sharedPrefs.AppPrefs
import com.toshi.util.sharedPrefs.UserPrefs
import com.toshi.util.sharedPrefs.UserPrefsInterface
import com.toshi.util.uploader.FileUploader
import com.toshi.view.BaseApplication
import retrofit2.HttpException
import rx.Completable
import rx.Observable
import rx.Scheduler
import rx.Single
import rx.Subscription
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription
import java.io.File
import java.util.concurrent.TimeUnit

class UserManager(
        private val idService: IdInterface = IdService.getApi(),
        private val fileUploader: FileUploader = FileUploader(idService),
        private val userPrefs: UserPrefsInterface = UserPrefs(),
        private val scheduler: Scheduler = Schedulers.io()
) {

    private val recipientManager by lazy { BaseApplication.get().recipientManager }
    private val isConnectedSubject by lazy { BaseApplication.get().isConnectedSubject }

    private val subscriptions by lazy { CompositeSubscription() }
    private val userSubject by lazy { BehaviorSubject.create<User>() }
    private var connectivitySub: Subscription? = null
    private lateinit var wallet: HDWallet

    fun init(wallet: HDWallet): Completable {
        this.wallet = wallet
        attachConnectivityListener()
        return initUser()
    }

    private fun attachConnectivityListener() {
        // Whenever the network changes init the user.
        // This is dumb and potentially inefficient but it shouldn't have
        // any adverse effects and it is easy to improve later.
        clearConnectivitySubscription()
        connectivitySub = isConnectedSubject
                .skip(1) // Skip the cached value in the subject.
                .subscribe(
                        { handleConnectivity(it) },
                        { LogUtil.exception("Error while initiating user $it") }
                )
    }

    private fun handleConnectivity(isConnected: Boolean) {
        if (isConnected) initUser()
                .subscribeOn(scheduler)
                .subscribe({}, {})
    }

    private fun initUser(): Completable {
        return when {
            userNeedsToRegister() -> registerNewUser()
            userNeedsToMigrate() -> migrateUser()
            AppPrefs.shouldForceUserUpdate() -> forceUpdateUser()
            else -> fetchAndUpdateUser()
        }
    }

    private fun userNeedsToRegister(): Boolean {
        val oldUserId = userPrefs.getOldUserId()
        val newUserId = userPrefs.getUserId()
        val expectedAddress = wallet.ownerAddress
        val userId = newUserId ?: oldUserId
        return userId == null || userId != expectedAddress
    }

    private fun userNeedsToMigrate(): Boolean {
        val userId = userPrefs.getUserId()
        val expectedAddress = wallet.ownerAddress
        return userId == null || userId != expectedAddress
    }

    private fun registerNewUser(): Completable {
        return getTimestamp()
                .flatMap { registerNewUserWithTimestamp(it) }
                .onErrorResumeNext { handleUserRegistrationFailed(it) } // If the user is already registered, the server will give a 400 response.
                .doOnSuccess { updateCurrentUser(it) }
                .doOnError { LogUtil.exception("Error while registering user with timestamp") }
                .toCompletable()
    }

    private fun registerNewUserWithTimestamp(serverTime: ServerTime): Single<User> {
        if (!::wallet.isInitialized) {
            return Single.error(IllegalStateException("Wallet is null while registerNewUserWithTimestamp"))
        }
        val userDetails = UserDetails().setPaymentAddress(wallet.paymentAddress)
        AppPrefs.setForceUserUpdate(false)
        return idService.registerUser(userDetails, serverTime.get())
    }

    private fun handleUserRegistrationFailed(throwable: Throwable): Single<User> {
        return if (throwable is HttpException && throwable.code() == 400) forceFetchUserFromNetworkSingle()
        else Single.error(throwable)
    }

    fun getCurrentUserObservable(): Observable<User> {
        forceFetchUserFromNetwork()
        return userSubject.asObservable()
    }

    private fun forceFetchUserFromNetworkSingle(): Single<User> {
        return getWallet()
                .flatMap { idService.forceGetUser(it.ownerAddress) }
                .doOnSuccess { updateCurrentUser(it) }
                .doOnError { LogUtil.exception("Error while fetching user from network $it") }
    }

    private fun forceFetchUserFromNetwork() {
        val sub = getWallet()
                .flatMap { idService.forceGetUser(it.ownerAddress) }
                .subscribe(
                        { updateCurrentUser(it) },
                        { LogUtil.exception("Error while fetching user from network $it") }
                )

        subscriptions.add(sub)
    }

    private fun fetchAndUpdateUser(): Completable {
        return getWallet()
                .flatMap { recipientManager.getUserFromPaymentAddress(it.paymentAddress) }
                .doOnSuccess { updateCurrentUser(it) }
                .doOnError { LogUtil.exception("Error while fetching user from network $it") }
                .toCompletable()
                .onErrorComplete()
    }

    private fun updateCurrentUser(user: User) {
        userPrefs.setUserId(user.toshiId)
        userSubject.onNext(user)
        recipientManager.cacheUser(user)
    }

    private fun migrateUser(): Completable {
        AppPrefs.setWasMigrated(true)
        return forceUpdateUser()
    }

    private fun forceUpdateUser(): Completable {
        val ud = UserDetails().setPaymentAddress(wallet.paymentAddress)
        return updateUser(ud)
                .doOnSuccess { AppPrefs.setForceUserUpdate(false) }
                .doOnError { LogUtil.exception("Error while updating user while initiating $it") }
                .toCompletable()
                .onErrorComplete()
    }

    fun updateUser(userDetails: UserDetails): Single<User> {
        return getTimestamp()
                .flatMap { updateUserWithTimestamp(userDetails, it) }
                .subscribeOn(scheduler)
    }

    private fun updateUserWithTimestamp(userDetails: UserDetails, serverTime: ServerTime): Single<User> {
        return getWallet()
                .flatMap { idService.updateUser(it.ownerAddress, userDetails, serverTime.get()) }
                .subscribeOn(scheduler)
                .doOnSuccess { updateCurrentUser(it) }
    }

    fun uploadAvatar(file: File): Single<User> {
        return fileUploader
                .uploadAvatar(file)
                .subscribeOn(scheduler)
                .doOnSuccess { userSubject.onNext(it) }
    }

    private fun getTimestamp() = idService.timestamp

    fun webLogin(loginToken: String): Completable {
        return getTimestamp()
                .flatMapCompletable { webLoginWithTimestamp(loginToken, it) }
                .subscribeOn(scheduler)
    }

    private fun webLoginWithTimestamp(loginToken: String, serverTime: ServerTime?): Completable {
        if (serverTime == null) throw IllegalStateException("ServerTime was null")
        return idService.webLogin(loginToken, serverTime.get())
    }

    fun getUserObservable(): Observable<User> = userSubject.asObservable()

    fun getCurrentUser(): Single<User?> {
        return userSubject
                .first()
                .toSingle()
                .timeout(20, TimeUnit.SECONDS)
                .doOnError { LogUtil.exception("getCurrentUser $it") }
                .onErrorReturn { null }
    }

    private fun getWallet(): Single<HDWallet> {
        return Single.fromCallable {
            while (!::wallet.isInitialized) Thread.sleep(100)
            wallet
        }
        .subscribeOn(scheduler)
        .timeout(20, TimeUnit.SECONDS)
    }

    fun clear() {
        subscriptions.clear()
        clearConnectivitySubscription()
        userPrefs.clear()
        userSubject.onNext(null)
    }

    private fun clearConnectivitySubscription() = connectivitySub?.unsubscribe()
}