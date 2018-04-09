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

package com.toshi.util

import com.toshi.manager.dappInjection.DappsInjector
import com.toshi.model.network.dapp.CoinbaseDapp
import com.toshi.model.network.dapp.Dapp
import com.toshi.model.network.dapp.DappSearchResult
import com.toshi.model.network.dapp.DappSection
import com.toshi.model.network.dapp.DappSections
import com.toshi.model.network.dapp.Dapps

class DappMocker {

    private val dappsWithoutCoinbaseDapp by lazy {
        mutableListOf<Dapp>(
                Dapp(
                        dappId = 1L,
                        name = "Dapp1",
                        url = "www.dapp1.com",
                        categories = listOf(1),
                        description = null,
                        icon = null,
                        cover = null
                ),
                Dapp(
                        dappId = 4L,
                        name = "Dapp2",
                        url = "www.dapp2.com",
                        categories = listOf(2),
                        description = null,
                        icon = null,
                        cover = null
                ),
                Dapp(
                        dappId = 4L,
                        name = "Dapp3",
                        url = "www.dapp3.com",
                        categories = listOf(3),
                        description = null,
                        icon = null,
                        cover = null
                ),
                Dapp(
                        dappId = 4L,
                        name = "Dapp4",
                        url = "www.dapp4.com",
                        categories = listOf(4),
                        description = null,
                        icon = null,
                        cover = null
                )
        )
    }

    fun buildDappsWithoutCoinbaseDapp() = dappsWithoutCoinbaseDapp

    fun buildDappsWithCoinbaseDapp(): List<Dapp> {
        val dapps = mutableListOf<Dapp>()
        dapps.addAll(dappsWithoutCoinbaseDapp)
        dapps.add(CoinbaseDapp())
        return dapps
    }

    fun buildDappSearchResult(): DappSearchResult {
        val dapps = Dapps(
                dapps = dappsWithoutCoinbaseDapp,
                categories = mutableMapOf()
        )
        return DappSearchResult(results = dapps)
    }

    fun buildDappSections(): DappSections {
        val sections = mutableListOf(
                DappSection(
                        categoryId = 10,
                        dapps = dappsWithoutCoinbaseDapp
                ),
                DappSection(
                        categoryId = DappsInjector.MARKETPLACE_ID,
                        dapps = dappsWithoutCoinbaseDapp
                ),
                DappSection(
                        categoryId = 30,
                        dapps = dappsWithoutCoinbaseDapp
                )
        )
        return DappSections(sections = sections)
    }

    fun buildDappSectionsWithoutMarketplaceSection(): DappSections {
        val sections = mutableListOf(
                DappSection(
                        categoryId = 10,
                        dapps = dappsWithoutCoinbaseDapp
                ),
                DappSection(
                        categoryId = 30,
                        dapps = dappsWithoutCoinbaseDapp
                )
        )
        return DappSections(sections = sections)
    }
}