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

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.extensions.equalsDelta
import com.wtb.dashTracker.extensions.toFloatOrNull
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.pow

class ReceiptAnalyzer {
    companion object {
        /**
         * Extracts expenses from a receipt.
         *
         * @param image The receipt image.
         * @return A list of expenses.
         */
        suspend fun extractExpense(image: InputImage): Expense? {
            val text: Text = extractTextFromImage(image)

            val values: List<String> = extractPossibleExpenseValuesFromBlocks(text)

            val dates: List<LocalDate> = extractPossibleDatesFromBlocks(text)

            val expenses: Set<Expense>? = lookForPriceGallonAmountTriplets(values, dates)

            return expenses?.first().takeIf { expenses?.count() == 1 }
                ?: expenses?.first().takeIf { expenses?.isNotEmpty() == true }
                ?: getGasExpense(values)
        }

        /**
         * Returns any text from [text] that contains a value of
         * [Pattern.ONLY_0_00]
         */
        internal fun extractPossibleExpenseValuesFromBlocks(text: Text): List<String> {
            /** Returns true if [block] contains a substring of the format '0.00(0)' */
            fun containsExpenseLikeValue(block: Text.TextBlock): Boolean =
                block.text.contains(Pattern.ONLY_0_00.regex)

            /**
             * Returns a list of substrings from [text] of the pattern
             * [Pattern.POSSIBLE_RECEIPT_AMOUNT]
             */
            fun extractPossibleExpenseValue(text: String): List<String> {
                return Pattern.POSSIBLE_RECEIPT_AMOUNT.regex.findAll(text).map {
                    it.value
                }.toList()
            }

            return text.textBlocks.filter { block ->
                containsExpenseLikeValue(block)
            }.flatMap { block ->
                block.lines
            }.flatMap { line ->
                extractPossibleExpenseValue(line.text)
            }
        }

        fun extractPossibleDatesFromBlocks(text: Text): List<LocalDate> {
            /**
             * Returns true if [block] contains a substring of the format
             * [Pattern.DATE]
             */
            fun containsPossibleDate(block: Text.TextBlock): Boolean =
                block.text.contains(Pattern.DATE.regex)

            /**
             * Returns a list of substrings from [text] of the pattern
             * [Pattern.POSSIBLE_RECEIPT_AMOUNT]
             */
            fun extractDate(text: String): List<LocalDate> {
                return Pattern.DATE.regex.findAll(text).mapNotNull {
                    var res: LocalDate? = null
                    dfPatterns.forEach { df ->
                        try {
                            res = LocalDate.parse(
                                it.value, DateTimeFormatter.ofPattern(df)
                            )
                        } catch (_: Exception) {
                            // Do Nothing
                        }
                    }
                    res
                }.toList()
            }

            return text.textBlocks.filter { block ->
                containsPossibleDate(block)
            }.flatMap { block ->
                block.lines
            }.flatMap { line ->
                extractDate(line.text)
            }
        }

        /** Extracts text from image */
        private suspend fun extractTextFromImage(image: InputImage): Text {
            val recognizer: TextRecognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            return recognizer.process(image).await()
        }

        /** Looks for any ppg * gallon = amount from a list of values */
        internal fun lookForPriceGallonAmountTriplets(
            values: List<String>,
            dates: List<LocalDate>
        ): Set<Expense>? {
            fun checkForCandidates(
                a: Float,
                b: Float,
                amount: Float,
                dates: List<LocalDate>
            ): Set<Expense> {
                val res = mutableSetOf<Expense>()
                val mDates = dates.takeIf { it.isNotEmpty() } ?: listOf(LocalDate.now())

                if ((a * b).equalsDelta(amount, .001f)) {
                    val pEndsWithNine = a.toString().last() == '9'
                    val gEndsWithNine = b.toString().last() == '9'
                    mDates.forEach { d ->
                        when {
                            pEndsWithNine && !gEndsWithNine -> {
                                res.add(Expense(date = d, amount = amount, pricePerGal = a))
                            }
                            gEndsWithNine && !pEndsWithNine -> {
                                res.add(Expense(date = d, amount = amount, pricePerGal = b))
                            }
                            else -> {
                                res.addAll(
                                    listOf(
                                        Expense(date = d, amount = amount, pricePerGal = a),
                                        Expense(date = d, amount = amount, pricePerGal = b)
                                    )
                                )
                            }
                        }
                    }
                }

                return res
            }

            val nums: List<Float> =
                values.mapNotNull { it.dollarStringToFloat() }.sortedDescending()

            val candidates = mutableSetOf<Expense>()

            for (num in nums) {
                val others: List<Float> = nums.minus(num).toList()

                others.forEachIndexed { index, a ->
                    others.subList(index + 1, others.count()).forEach { g ->
                        others.subList(index + 1, others.count()).forEach { p ->
                            candidates.addAll(checkForCandidates(p, g, a, dates))
                        }
                    }
                }
            }

            val nines = candidates.filter {
                it.pricePerGal.toString().last() == '9'
            }.toSet()

            return nines.takeIf { it.isNotEmpty() } ?: candidates.takeIf { it.isNotEmpty() }
        }

        internal fun getGasExpense(values: List<String>): Expense? {
            val ppgCandidates: List<String> = Pattern.POSSIBLE_PPG.filter(values)

            val galCandidates: List<Float> =
                Pattern.POSSIBLE_GALLONS.filter(values).mapNotNull { it.toFloatOrNull() }

            val amountCandidates: List<String> = Pattern.POSSIBLE_RECEIPT_AMOUNT.filter(values)

            return getBestGuessExpense(ppgCandidates, galCandidates, amountCandidates)
        }

        private fun getBestGuessExpense(
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
            }
        }

        private fun getBestAmount(amounts: List<String>): Float? {
            fun getRegexScore(str: String): Int {
                return when {
                    Pattern.ONLY_TOTAL_DEBIT_OR_CREDIT.matches(str) -> Int.MAX_VALUE
                    Pattern.ONLY_DOLLAR_SIGN_0_00.matches(str) -> Int.MAX_VALUE - 1
                    Pattern.ONLY_0_00.matches(str) -> Int.MAX_VALUE - 2
                    else -> ((str.dollarStringToFloat(2) ?: 0f) * 100f).toInt()
                }
            }

            // ideal: Total|Debit|Credit $0.00, good: $0.00, okay: 0.00
            val res: String? = if (amounts.isEmpty()) {
                null
            } else if (amounts.count() == 1) {
                amounts.first()
            } else {
                amounts.maxByOrNull { getRegexScore(it) }
            }

            return res?.dollarStringToFloat(2)
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

        private fun getExpenseFromAG(
            bestGallons: Float?,
            bestAmount: Float?
        ): Expense? {
            val ppg = bestGallons?.let { g -> bestAmount?.let { a -> a / g } }
            return ppg?.let { Expense(pricePerGal = ppg) }
        }

        private fun getExpenseFromPG(
            bestGallons: Float?,
            bestPpg: Float?
        ): Expense? {
            val amount = bestGallons?.let { g -> bestPpg?.let { p -> g * p } }
            return amount?.let { Expense(amount = amount, pricePerGal = bestPpg) }
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

        private fun String.dollarStringToFloat(decimals: Int? = null): Float? =
            this.filter { it != '$' && !it.isLetter() }.toFloatOrNull()
                ?.times(10.0.pow(n = decimals ?: 0))?.apply {
                    if (decimals != null) {
                        toInt()
                    }
                }?.toFloat()?.div(10.0.pow(n = decimals ?: 0))?.toFloat()

        enum class Pattern(private val pattern: String) {
            DATE("(?i)(?:(?:\\d{1,2}[.\\/-]\\d{1,2}[.\\/-]\\d{2}(?:\\d{2})?))|(?:(?:\\d{1,2}\\s+)?(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|July|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+(?:\\d{1,2}(?:,)?\\s+)?\\d{2}(?:\\d{2})?)"),
            ONLY_0_00("\\d+\\.\\d{2}"),
            ONLY_DOLLAR_SIGN_0_00("\\$${ONLY_0_00}"),
            ONLY_TOTAL_DEBIT_OR_CREDIT("(?i)(total|debit|credit)\\s\\$?${ONLY_0_00}"),
            POSSIBLE_PPG("[$]?${ONLY_0_00}\\d"),
            POSSIBLE_GALLONS("[^$]\\d+\\.\\d{2,4}"),
            POSSIBLE_RECEIPT_AMOUNT("(?i)((debit|credit|total)\\s)?\\$?${ONLY_0_00}\\d?");

            val regex: Regex
                get() = Regex(pattern)

            fun matches(str: String): Boolean = regex.matches(str)

            fun filter(values: List<String>): List<String> {
                return values.filter { str ->
                    matches(str)
                }
            }

            override fun toString(): String = pattern
        }

        private val dfPatterns = listOf(
            "M.d.yy",
            "M.d.yyyy",
            "M/d/yy",
            "M/d/yyyy",
            "MMMM d, yyyy",
            "MMM d yyyy",
            "MMM d, yyyy",
            "d MMM yyyy",
            "d MMM yy",
            "dd MMM yy"
        )

        enum class C {
            NONE, A, P, G, A_P, A_G, P_G, A_P_G
        }
    }
}