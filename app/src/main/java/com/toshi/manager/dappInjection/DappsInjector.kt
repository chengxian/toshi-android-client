/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 *  This program is free software: you can redistribute it and/or modify
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

package com.toshi.manager.dappInjection

import android.content.Context
import com.toshi.R
import com.toshi.model.network.dapp.CoinbaseDapp
import com.toshi.model.network.dapp.Dapp
import com.toshi.model.network.dapp.DappSearchResult
import com.toshi.model.network.dapp.DappSection
import com.toshi.model.network.dapp.DappSections

class DappsInjector(
        private val context: Context
) {

    companion object {
        const val MARKETPLACE_ID = 2
    }

    fun addCoinbaseDappToMarketplaceSection(dappSections: DappSections, paymentAddress: String): DappSections {
        val marketPlaceSection = findMarketplaceSection(dappSections)
        if (marketPlaceSection == null) addCoinbaseDappAndMarketplaceSection(dappSections, paymentAddress)
        else marketPlaceSection.dapps.add(getCoinbaseDapp(paymentAddress))
        return dappSections
    }

    private fun addCoinbaseDappAndMarketplaceSection(dappSections: DappSections, paymentAddress: String) {
        val coinbaseDapp = getCoinbaseDapp(paymentAddress)
        val dappSection = DappSection(MARKETPLACE_ID, mutableListOf(coinbaseDapp))
        dappSections.sections.add(dappSection)
    }

    private fun findMarketplaceSection(dappSections: DappSections): DappSection? {
        return try {
            dappSections.sections.first { it.categoryId == MARKETPLACE_ID }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    fun addCoinbaseDappToSearchResult(searchResult: DappSearchResult,
                                      currentDapps: List<Dapp>,
                                      paymentAddress: String): DappSearchResult {
        val containsCoinbaseDapp = containsCoinbaseDapp(currentDapps)
        if (containsCoinbaseDapp) return searchResult
        else addCoinbaseDappToSearchResult(searchResult, paymentAddress)
        return searchResult
    }

    private fun addCoinbaseDappToSearchResult(searchResult: DappSearchResult, paymentAddress: String) {
        val coinbaseDapp = getCoinbaseDapp(paymentAddress)
        searchResult.results.dapps.add(coinbaseDapp)
    }

    private fun getCoinbaseDapp(paymentAddress: String): CoinbaseDapp {
        return CoinbaseDapp(
                url = context.getString(R.string.coinbase_buy_widget_url, paymentAddress),
                description = context.getString(R.string.coinbase_dapp_description)
        )
    }

    fun getDappsOffset(currentDapps: List<Dapp>): Int {
        val containsCoinbaseDapp = containsCoinbaseDapp(currentDapps)
        return if (containsCoinbaseDapp) currentDapps.size - 1
        else currentDapps.size
    }

    private fun containsCoinbaseDapp(currentDapps: List<Dapp>): Boolean {
        return try {
            currentDapps.first { it is CoinbaseDapp }
            return true
        } catch (e: NoSuchElementException) {
            false
        }
    }
}