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

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.extensions.equalsDelta
import com.wtb.dashTracker.extensions.toFloatOrNull
import kotlinx.coroutines.tasks.await
import kotlin.math.pow

class ReceiptAnalyzer {
    companion object {
        private suspend fun analyze(image: InputImage): Text {
            val recognizer: TextRecognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            return recognizer.process(image).await()
        }

        internal suspend fun extractExpense(image: InputImage): Expense? {
            val text = analyze(image)
            val values = text.textBlocks.filter { block ->
                containsExpenseLikeValue(block.text)
            }.flatMap { block ->
                block.lines.map { line -> line.text }
            }.flatMap { str: String ->
                Log.d(TAG, "str: $str")
                extractPossibleExpenseValue(str)
            }
            Log.d(TAG, "Dollars: $values")
            val expenses = findThree(values)

            if (expenses?.count() == 1) {
                return expenses.first()
            } else if (expenses?.isNotEmpty() == true) {
                // TODO: need to set up some sort of picker here
                return expenses.first()
            }
            // TODO: This might be incorporated into findThree?
            return getGasExpense(values)
        }

        internal fun findThree(values: List<String>): Set<Expense>? {
            val nums: List<Float> =
                values.mapNotNull { it.dollarStringToFloat() }.sortedDescending()
            val candidates = mutableSetOf<Expense>()

            for (num in nums) {
                val others: List<Float> = nums.minus(num).toList()

                others.forEachIndexed { index, a ->
                    others.subList(index + 1, others.count()).forEach { g ->
                        others.subList(index + 1, others.count()).forEach { p ->
                            candidates.addAll(checkForCandidates(p, g, a))
                        }
                    }
                }
            }

            val nines = candidates.filter {
                it.pricePerGal.toString().last() == '9'
            }.toSet()

            return if (nines.isNotEmpty()) {
                nines
            } else if (candidates.isNotEmpty()) {
                candidates
            } else {
                null
            }
        }

        private fun checkForCandidates(
            a: Float,
            b: Float,
            amount: Float,
        ): Set<Expense> {
            return if ((a * b).equalsDelta(amount, .001f)) {
                val pEndsWithNine = a.toString().last() == '9'
                val gEndsWithNine = b.toString().last() == '9'
                when {
                    pEndsWithNine && !gEndsWithNine -> {
                        setOf(Expense(amount = amount, pricePerGal = a))
                    }
                    gEndsWithNine && !pEndsWithNine -> {
                        setOf(Expense(amount = amount, pricePerGal = b))
                    }
                    else -> {
                        setOf(
                            Expense(amount = amount, pricePerGal = a),
                            Expense(amount = amount, pricePerGal = b)
                        )
                    }
                }
            } else {
                setOf()
            }
        }

        /** returns true if [text] contains a substring of the format '0.00(0)' */
        internal fun containsExpenseLikeValue(text: String): Boolean =
            text.contains(Regex(".*\\d\\.\\d{2}.*"))

        /** Returns a list of substrings from [text] of the pattern ($)0*.00(0) */
        internal fun extractPossibleExpenseValue(text: String): List<String> {
            return reMatchExpenseValue.findAll(text).map {
                it.value
            }.toList()
        }

        /** Matches '($)0*.00(0) * */
        private val reMatchExpenseValue =
            Regex("(?i)((debit|credit|total)\\s)?\\$?\\d+\\.\\d{2}\\d?")

        internal fun getGasExpense(values: List<String>): Expense? {
            val ppgCandidates: List<String> = filterPpgCandidates(values)

            val galCandidates: List<Float> = filterGallonCandidates(values)

            val amountCandidates: List<String> = filterTotalCandidates(values)

            return getSomething(ppgCandidates, galCandidates, amountCandidates)
        }

        private fun getSomething(
            ppgs: List<String>,
            gallons: List<Float>,
            amounts: List<String>
        ): Expense? {
            val combos = getCombos(ppgs, gallons, amounts)

            val bestAmount: Float? = getBestAmount(amounts)

            val bestGallons: Float? = getBestGallons(gallons)

            val bestPpg: Float? = getBestPpg(ppgs)

            val res: Expense? = when (combos) {
                C.NONE,
                C.G -> null
                C.A_G -> getExpenseFromAG(bestGallons, bestAmount)
                C.P_G -> getExpenseFromPG(bestGallons, bestPpg)
                C.A_P_G -> getExpenseFromAPG(bestAmount, gallons, ppgs, bestPpg)
                else -> Expense(amount = bestAmount, pricePerGal = bestPpg)
            }

            return res
        }

        private fun getExpenseFromPG(
            bestGallons: Float?,
            bestPpg: Float?
        ): Expense? {
            val amount = bestGallons?.let { g -> bestPpg?.let { p -> g * p } }
            return amount?.let { Expense(amount = amount, pricePerGal = bestPpg) }
        }

        private fun getExpenseFromAG(
            bestGallons: Float?,
            bestAmount: Float?
        ): Expense? {
            val ppg = bestGallons?.let { g -> bestAmount?.let { a -> a / g } }
            return ppg?.let { Expense(pricePerGal = ppg) }
        }

        private fun getExpenseFromAPG(
            bestAmount: Float?,
            gallons: List<Float>,
            ppgs: List<String>,
            bestPpg: Float?
        ): Expense {
            val ppg: Float?
            val amount: Float? = bestAmount

            if (listOf(gallons, ppgs).all { it.count() == 1 }) {
                ppg = bestPpg
            } else { // 1 a, and multiple p & g
                val candidates = mutableListOf<Pair<String?, Float?>>()

                for (p in ppgs) {
                    for (g in gallons) {
                        val strippedP =
                            p.dollarStringToFloat()
                        if (strippedP?.let { it * g } == amount) {
                            candidates.add(Pair(p, g))
                        }
                    }
                }

                ppg = if (candidates.isNotEmpty()) {
                    candidates.first().first?.dollarStringToFloat(3)
                } else {
                    bestPpg
                }
            }

            return Expense(amount = amount, pricePerGal = ppg)
        }

        internal fun getBestPpg(ppgs: List<String>): Float? {
            fun pickBestPpg(): String? {
                // TODO: This might be improved
                return ppgs.sortedWith { p0, p1 ->
                    val p0isDollar = p0.first() == '$'
                    val p1isDollar = p1.first() == '$'
                    val p0isNine = p0.last() == '9'
                    val p1isNine = p1.last() == '9'
                    when {
                        p0isNine && !p1isNine && p0isDollar && !p1isDollar -> -2
                        !p0isNine && p1isNine && !p0isDollar && p1isDollar -> 2
                        p0isNine && p0isDollar && (!p1isNine || !p1isDollar) -> -1
                        p1isNine && p1isDollar && (!p0isNine || !p0isDollar) -> 1
                        else -> p1.dollarStringToFloat()
                            ?.let { p0.dollarStringToFloat()?.compareTo(it) }
                            ?: p0.compareTo(p1)
                    }
                }.firstOrNull()
            }

            val res: String? = if (ppgs.isEmpty()) {
                null
            } else if (ppgs.count() == 1) {
                ppgs.first()
            } else {
                pickBestPpg()
            }

            return res?.dollarStringToFloat(3)
        }

        private fun getBestGallons(gallons: List<Float>): Float? {
            fun pickBestGallons(): Float {
                // TODO: This might be improved
                return gallons.random()
            }

            val res: Float? = if (gallons.isEmpty()) {
                null
            } else if (gallons.count() == 1) {
                gallons.first()
            } else {
                pickBestGallons()
            }

            return res
        }

        private fun String.dollarStringToFloat(decimals: Int? = null): Float? =
            this.filter { it != '$' && !it.isLetter() }.toFloatOrNull()
                ?.times(10.0.pow(n = decimals ?: 0))?.apply {
                    if (decimals != null) {
                        toInt()
                    }
                }?.toFloat()?.div(10.0.pow(n = decimals ?: 0))?.toFloat()

        private fun getBestAmount(amounts: List<String>): Float? {
            fun rankAmounts(): List<String> {
                fun getRegexScore(str: String): Int {
                    return when {
                        Regex("(?i)(total|debit|credit)\\s\\$\\d+\\.\\d{2}").matches(str) -> 7
                        Regex("\\$\\d+\\.\\d{2}").matches(str) -> 3
                        Regex("\\d+\\.\\d{2}").matches(str) -> 1
                        else -> 0
                    }
                }

                return amounts.sortedWith { a, b ->
                    val reScore = getRegexScore(b) - getRegexScore(a)
                    if (reScore != 0) {
                        reScore
                    } else {
                        a.dollarStringToFloat(2)?.let { aFloat ->
                            b.dollarStringToFloat(2)?.let { bFloat -> aFloat - bFloat }
                        }?.toInt() ?: 0
                    }
                }
            }

            // ideal: Total|Debit|Credit $0.00, good: $0.00, okay: 0.00
            fun pickBestAmount(): String = rankAmounts()[0]

            val res: String? = if (amounts.isEmpty()) {
                null
            } else if (amounts.count() == 1) {
                amounts.first()
            } else {
                pickBestAmount()
            }

            return res?.dollarStringToFloat(2)
        }

        private fun getCombos(
            ppgs: List<String>,
            gallons: List<Float>,
            amounts: List<String>
        ): C {
            val pEmpty = ppgs.isEmpty()
            val gEmpty = gallons.isEmpty()
            val aEmpty = amounts.isEmpty()

            return when {
                aEmpty && pEmpty && gEmpty -> C.NONE
                aEmpty && pEmpty -> C.G
                aEmpty && gEmpty -> C.P
                pEmpty && gEmpty -> C.A
                pEmpty -> C.A_G
                gEmpty -> C.A_P
                aEmpty -> C.P_G
                else -> C.A_P_G
//                ppgs.isEmpty() -> when {
//                    gallons.isEmpty() -> when {
//                        amounts.isEmpty() -> C.NONE
//                        else -> C.A
//                    }
//                    else -> when {
//                        amounts.isEmpty() -> C.G
//                        else -> C.A_G
//                    }
//                }
//                else -> when {
//                    gallons.isEmpty() -> when {
//                        amounts.isEmpty() -> C.P
//                        else -> C.A_P
//                    }
//                    else -> when {
//                        amounts.isEmpty() -> C.P_G
//                        else -> C.A_P_G
//                    }
//                }
            }
        }

        enum class C {
            NONE, A, P, G, A_P, A_G, P_G, A_P_G
        }

        /** Matches '$?0*.00' * */
        private val reMatchTotal = Regex("((?i)(debit|credit|cash|total)\\s)?[$]?\\d*\\.\\d{2}")

        internal fun filterTotalCandidates(values: List<String>): List<String> {
            return values.filter { str ->
                str.matches(reMatchTotal)
            }
        }

        /** Matches '[^$]0*.00' */
        private val regexGallons = Regex("[^$]\\d*\\.\\d{2,4}")

        internal fun filterGallonCandidates(values: List<String>): List<Float> {
            return values.filter { str ->
                str.matches(regexGallons)
            }.mapNotNull { str ->
                str.toFloatOrNull()
            }.toList()
        }

        /** Matches ($)0*.000 */
        private val regexPpg = Regex("[$]?\\d*\\.\\d{3}")

        internal fun filterPpgCandidates(values: List<String>): List<String> {
            return values.filter { str ->
                str.matches(regexPpg)
            }
        }
    }
}