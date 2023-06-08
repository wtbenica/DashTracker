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

import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.containsExpenseLikeValue
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.extractPossibleExpenseValue
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.filterGallonCandidates
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.filterPpgCandidates
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.filterTotalCandidates
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.findThree
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.getBestPpg
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.getGasExpense
import org.junit.*
import org.junit.Assert.*

internal class ReceiptAnalyzerTest {

    @Test
    fun containsExpenseLikeValueTestValid() {
        val test1 = "Test 1: .94.19. or $9 ll00"
        assertTrue(containsExpenseLikeValue(test1))
    }

    @Test
    fun containsExpenseLikeValueTestStartOfString() {
        val test1 = "2.0839090"
        assertTrue(containsExpenseLikeValue(test1))
    }

    @Test
    fun containsExpenseLikeValueTestNoLeadingDigit() {
        val test1 = "Test 1: ..19 or $9 ll00"
        assertFalse(containsExpenseLikeValue(test1))
    }

    @Test
    fun containsExpenseLikeValueTestNoTrailingDigit() {
        val test1 = "Test 1: 4. or $9. ll00"
        assertFalse(containsExpenseLikeValue(test1))
    }

    @Test
    fun containsExpenseLikeValueTestNoDecimal() {
        val test1 = "Test 1: 19 or $9 l.l00"
        assertFalse(containsExpenseLikeValue(test1))
    }

    @Test
    fun extractPossibleExpenseValuesTest() {
        val str = "DEBIT $20.32 APPROVED"
        assertArrayEquals(arrayOf("DEBIT $20.32"), extractPossibleExpenseValue(str).toTypedArray())
    }

    @Test
    fun extractPossibleExpenseValuesTestMultiples() {
        val str = "$4.059 4.18 $17.23"
        assertArrayEquals(
            arrayOf("$4.059", "4.18", "$17.23"),
            extractPossibleExpenseValue(str).toTypedArray()
        )
    }

    @Test
    fun extractPossibleExpenseValuesTestAdjacentText() {
        val str = "DEBIT$20.32APPROVED"
        assertArrayEquals(arrayOf("$20.32"), extractPossibleExpenseValue(str).toTypedArray())
    }

    @Test
    fun extractPossibleExpenseValuesTestDecoyNumbers() {
        val str = "DEBIT $20.32 APPROVED 4 4. .4 0.0 4.333 432.1890002"
        assertArrayEquals(
            arrayOf("DEBIT $20.32", "4.333", "432.189"),
            extractPossibleExpenseValue(str).toTypedArray()
        )
    }

    @Test
    fun filterTotalCandidatesTest() {
        val testList = listOf("$23.23", "$4.234", "4.324", "1024")
        assertArrayEquals(arrayOf("$23.23"), filterTotalCandidates(testList).toTypedArray())
    }

    @Test
    fun filterGallonCandidatesTest() {
        val testList: List<String> = listOf("$20.32", "$4.333", "4.18", "$4.18", "3.144")
        val expected: Array<Float> = arrayOf(4.18f, 3.144f)
        val actual: Array<Float> = filterGallonCandidates(testList).toTypedArray()
        assertArrayEquals(expected, actual)
    }

    @Test
    fun filterPpgCandidatesTest() {
        val testList = listOf("$20.32", "$4.333", "4.18", "4.333")
        assertArrayEquals(arrayOf("$4.333", "4.333"), filterPpgCandidates(testList).toTypedArray())
    }

    @Test
    fun getGasExpenseTest() {
        val testList = listOf("4.204", "DEBIT $20.36", "$4.759", "$20.01", "$0.35", "$20.36")
        val res = getGasExpense(testList)
        assertEquals(20.36f, res?.amount)
        assertEquals(4.759f, res?.pricePerGal)
    }

    @Test
    fun findThreeTest() {
        // TODO: This only tests for when there is one combination
        val testList = listOf("4.204", "DEBIT $20.36", "$4.759", "$20.01", "$0.35", "$20.36")
        val res = findThree(testList)?.firstOrNull()
        assertEquals(20.01f, res?.amount)
        assertEquals(4.759f, res?.pricePerGal)
    }

    @Test
    fun getBestPpgTest() {
        val testList = listOf("4.329", "4.208", "$4.899", "$0.36", "8.101")
        assertEquals(4.899f, getBestPpg(testList))
    }
}