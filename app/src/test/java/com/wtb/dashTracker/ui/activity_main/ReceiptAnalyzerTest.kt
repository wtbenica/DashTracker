/*
 * Copyright 2023 Wesley T. Benica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.dashTracker.ui.activity_main

import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.getBestPpg
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.getExpense
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.lookForPriceGallonAmountTriplets
import org.junit.Assert.assertEquals
import org.junit.Test


internal class ReceiptAnalyzerTest {
//    private var receiptAnalyzer: ReceiptAnalyzer? = null
//    private var mockedInputImage: InputImage? = null
//    private var mockedText: Text? = null
//
//    @Before
//    fun setup() {
//        receiptAnalyzer = ReceiptAnalyzer()
//        mockedInputImage = mock(InputImage::class.java)
//        mockedText = mock(Text::class.java)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testExtractExpenseWithValidText() {
//        val expectedExpense = Expense(amount = 10.0f, pricePerGal = 2.0f)
//        `when`(mockedText!!.textBlocks).thenReturn(getMockedTextBlocks())
//        `when`(ReceiptAnalyzer.extractTextFromImage(any(InputImage::class.java))).thenReturn(
//            mockedText
//        )
//        val actualExpense: Expense = ReceiptAnalyzer.extractExpense(mockedInputImage)
//        assertEquals(expectedExpense, actualExpense)
//    }

    @Test
    fun getGasExpenseTest() {
        val testList = listOf("4.204", "DEBIT $20.36", "$4.759", "$20.01", "$0.35", "$20.36")
        val res = getExpense(testList)
        assertEquals(20.36f, res?.amount)
        assertEquals(4.759f, res?.pricePerGal)
    }

    @Test
    fun findThreeTest() {
        // TODO: This only tests for when there is one combination
        val testList = listOf("4.204", "DEBIT $20.36", "$4.759", "$20.01", "$0.35", "$20.36")
        val res = lookForPriceGallonAmountTriplets(testList, listOf())?.firstOrNull()
        assertEquals(20.01f, res?.amount)
        assertEquals(4.759f, res?.pricePerGal)
    }

    @Test
    fun getBestPpgTest() {
        val testList = listOf("4.329", "4.208", "$4.899", "$0.36", "8.101")
        assertEquals(4.899f, getBestPpg(testList))
    }

//    private fun getMockedTextBlocks(): List<TextBlock?>? {
//        val textBlocks: List<TextBlock> = ArrayList()
//        val block = mock(TextBlock::class.java)
//        `when`(block.text).thenReturn("Total: $20.00")
//        textBlocks.add(block)
//        return textBlocks
//    }
}