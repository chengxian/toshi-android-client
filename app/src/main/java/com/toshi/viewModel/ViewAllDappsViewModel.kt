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

package com.toshi.viewModel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Intent
import com.toshi.R
import com.toshi.model.network.dapp.Dapps
import com.toshi.util.SingleLiveEvent
import com.toshi.view.BaseApplication
import com.toshi.view.activity.ViewAllDappsActivity.Companion.ALL
import com.toshi.view.activity.ViewAllDappsActivity.Companion.CATEGORY
import com.toshi.view.activity.ViewAllDappsActivity.Companion.CATEGORY_ID
import com.toshi.view.activity.ViewAllDappsActivity.Companion.VIEW_TYPE
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class ViewAllDappsViewModel(private val intent: Intent) : ViewModel() {

    private val dappsManager by lazy { BaseApplication.get().dappManager }
    private val subscriptions by lazy { CompositeSubscription() }

    val dapps by lazy { MutableLiveData<Dapps>() }
    val categoryName by lazy { MutableLiveData<String>() }
    val dappsError by lazy { SingleLiveEvent<Int>() }

    init {
        if (getViewType() == CATEGORY) getAllDappsInCategory(getCategoryId())
        else getAllDapps()
    }

    private fun getViewType() = intent.getIntExtra(VIEW_TYPE, ALL)
    private fun getCategoryId() = intent.getIntExtra(CATEGORY_ID, -1)

    private fun getAllDapps() {
        val sub = dappsManager
                .getAllDapps()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { setCategoryName(it, getCategoryId()) }
                .subscribe(
                        { dapps.value = it },
                        { dappsError.value = R.string.error_fetching_dapps }
                )

        subscriptions.add(sub)
    }

    private fun getAllDappsInCategory(categoryId: Int) {
        val sub = dappsManager
                .getAllDappsInCategory(categoryId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { setCategoryName(it, getCategoryId()) }
                .subscribe(
                        { dapps.value = it },
                        { dappsError.value = R.string.error_fetching_dapps }
                )

        subscriptions.add(sub)
    }

    private fun setCategoryName(dapps: Dapps, categoryId: Int) {
        val categoryName = when {
            getViewType() == ALL -> BaseApplication.get().getString(R.string.all_dapps)
            categoryId == -1 -> BaseApplication.get().getString(R.string.other)
            else -> dapps.categories[categoryId] ?: BaseApplication.get().getString(R.string.other)
        }
        this.categoryName.value = categoryName
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }
}