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
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class DappsInjectorTests {

    private val dappsInjector by lazy { DappsInjector() }
    private val dappMocker by lazy { DappMocker() }
    private val dappsSearchResult by lazy { dappMocker.buildDappSearchResult() }
    private val dappSections by lazy { dappMocker.buildDappSections() }
    private val dappSectionsWithoutMarketplaceSection by lazy { dappMocker.buildDappSectionsWithoutMarketplaceSection() }
    private val dappsWithoutCoinbaseDapp by lazy { dappMocker.buildDappsWithoutCoinbaseDapp() }
    private val dappsWithCoinbaseDapp by lazy { dappMocker.buildDappsWithCoinbaseDapp() }

    @Test
    fun `test that the coinbase dapp is added to the result list`() {
        val searchResultWithCoinbaseDapp = dappsInjector.addCoinbaseDappToSearchResult(dappsSearchResult, dappsWithoutCoinbaseDapp)
        val expectedResult = dappsSearchResult.results.dapps
        val actualResult = searchResultWithCoinbaseDapp.results.dapps
        assertEquals(expectedResult.size, actualResult.size)
        assertTrue(expectedResult.contains(CoinbaseDapp()))
    }

    @Test
    fun `test that the coinbase dapp is added to the section`() {
        val dappSectionsWithCoinbaseDapp = dappsInjector.addCoinbaseDappToMarketplaceSection(dappSections)
        val expectedResult = dappSections.sections
        val actualResult = dappSectionsWithCoinbaseDapp.sections
        assertEquals(expectedResult.size, actualResult.size)
        assertEquals(expectedResult[1].dapps.size, actualResult[1].dapps.size)
        assertTrue(actualResult[1].dapps.contains(CoinbaseDapp()))
    }

    @Test
    fun `test that the coinbase dapp is added to the section list when the marketplace section doesn't exist`() {
        val dappSectionsWithCoinbaseDapp = dappsInjector.addCoinbaseDappToMarketplaceSection(dappSectionsWithoutMarketplaceSection)
        val expectedResult = dappSectionsWithoutMarketplaceSection.sections
        val actualResult = dappSectionsWithCoinbaseDapp.sections
        assertEquals(expectedResult.size, actualResult.size)
        assertEquals(expectedResult[1].dapps.size, actualResult[1].dapps.size)
        assertTrue(actualResult[2].dapps.contains(CoinbaseDapp()))
    }

    @Test
    fun `test getting offset without coinbase dapp`() {
        val offset = dappsInjector.getDappsOffset(dappsWithoutCoinbaseDapp)
        assertEquals(offset, dappsWithoutCoinbaseDapp.size)
    }

    @Test
    fun `test getting offset with coinbase dapp`() {
        val offset = dappsInjector.getDappsOffset(dappsWithCoinbaseDapp)
        assertEquals(dappsWithCoinbaseDapp.size - 1, offset)
    }
}