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
         * Extract expense - Extracts a list of expenses from [image].
         *
         * @param image The receipt image.
         * @return A list of [Expense]s.
         */
        suspend fun extractExpense(image: InputImage): Expense? {
            val text: Text = extractTextFromImage(image)

            val values: List<String> = extractPossibleExpenseValuesFromBlocks(text)

            val dates: List<LocalDate> = extractPossibleDatesFromBlocks(text)

            val expenses: Set<Expense>? = lookForPriceGallonAmountTriplets(values, dates)

            return expenses?.first().takeIf { expenses?.count() == 1 }
                ?: expenses?.first().takeIf { expenses?.isNotEmpty() == true }
                ?: getExpense(values)
        }

        /**
         * Extract possible expense values from blocks - Extracts a list of
         * possible expense values from [text].
         *
         * @param text The text from the receipt image.
         * @return A list of possible expense values.
         */
        internal fun extractPossibleExpenseValuesFromBlocks(text: Text): List<String> {
            /** Returns true if [block] contains a substring of the format '0.00(0)' */
            fun containsExpenseLikeValue(block: Text.TextBlock): Boolean =
                block.text.contains(ReceiptValuePattern.ONLY_0_00.regex)

            /**
             * Returns a list of substrings from [text] of the pattern
             * [ReceiptValuePattern.POSSIBLE_RECEIPT_AMOUNT]
             */
            fun extractPossibleExpenseValue(text: String): List<String> {
                return ReceiptValuePattern.POSSIBLE_RECEIPT_AMOUNT.regex.findAll(text).map {
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

        /**
         * Extract possible dates from blocks - Extracts a list of possible dates
         * from [text].
         *
         * @param text The text from the receipt image.
         * @return A list of possible dates.
         */
        fun extractPossibleDatesFromBlocks(text: Text): List<LocalDate> {
            /**
             * Returns true if [block] contains a substring of the format
             * [ReceiptValuePattern.DATE]
             */
            fun containsPossibleDate(block: Text.TextBlock): Boolean =
                block.text.contains(ReceiptValuePattern.DATE.regex)

            /**
             * Returns a list of substrings from [text] of the pattern
             * [ReceiptValuePattern.POSSIBLE_RECEIPT_AMOUNT]
             */
            fun extractDate(text: String): List<LocalDate> {
                return ReceiptValuePattern.DATE.regex.findAll(text).mapNotNull {
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

        /**
         * Extract text from image - Uses [TextRecognizer] to extract text from
         * [image].
         *
         * @param image The image to extract text from.
         * @return The text from [image].
         */
        private suspend fun extractTextFromImage(image: InputImage): Text {
            val recognizer: TextRecognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            return recognizer.process(image).await()
        }


        /**
         * Look for price gallon amount triplets - Looks for price, gallons, amount
         * triplets in [values] and then returns a set of [Expense]s for each
         * combination of date from [dates] and price gallon amount triplet found.
         */
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

        /**
         * Get expense - Returns any [Expense]s from [values] if one can be found.
         *
         * @param values A list of values from the receipt
         */
        internal fun getExpense(values: List<String>): Expense? {
            // these are left as strings in case there is any surrounding text that
            // might indicate what the value is, e.g. "PPG", "Credit Price", etc.
            val ppgs: List<String> = ReceiptValuePattern.POSSIBLE_PPG.filter(values)

            val gallons: List<Float> =
                ReceiptValuePattern.POSSIBLE_GALLONS.filter(values).mapNotNull { it.toFloatOrNull() }

            // these are left as strings in case there is any surrounding text that
            // might indicate what the value is, e.g. "Total", "Amount", etc.
            val totals: List<String> = ReceiptValuePattern.POSSIBLE_RECEIPT_AMOUNT.filter(values)

            val bestTotal: Float? = getBestTotal(totals)

            val bestGallons: Float? = getBestGallons(gallons)

            val bestPpg: Float? = getBestPpg(ppgs)

            return ExpenseFactory.getExpense(bestTotal, bestGallons, bestPpg, gallons, ppgs)
        }

        /**
         * Get best total - Tries to determine the best candidate for the total
         * from [amounts].
         *
         * @param amounts The amounts from the receipt.
         * @return The best total from [amounts] or null if none can be found.
         */
        private fun getBestTotal(amounts: List<String>): Float? {
            fun getRegexScore(str: String): Int {
                return when {
                    ReceiptValuePattern.ONLY_TOTAL_DEBIT_OR_CREDIT.matches(str) -> Int.MAX_VALUE
                    ReceiptValuePattern.ONLY_DOLLAR_SIGN_0_00.matches(str) -> Int.MAX_VALUE - 1
                    ReceiptValuePattern.ONLY_0_00.matches(str) -> Int.MAX_VALUE - 2
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

        /**
         * Get best gallons - Tries to determine the best candidate for the gallons
         * from [gallons].
         *
         * @param gallons The gallons from the receipt.
         * @return The best gallons from [gallons] or null if none can be found.
         */
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

        /**
         * Get best ppg - Tries to determine the best candidate for the price per
         * gallon from [ppgs].
         *
         * @param ppgs The price per gallon from the receipt.
         * @return The best price per gallon from [ppgs] or null if none can be
         *     found.
         */
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

        /**
         * Dollar string to float - Converts a string like "$0.00" to a float like 0.00
         *
         * @param decimals The number of decimals to round to, defaults to 0
         * @return The float value of the string, or null if it could not be
         *     converted
         */
        fun String.dollarStringToFloat(decimals: Int? = null): Float? =
            this.filter { it != '$' && !it.isLetter() }.toFloatOrNull()
                ?.times(10.0.pow(n = decimals ?: 0))?.apply {
                    if (decimals != null) {
                        toInt()
                    }
                }?.toFloat()?.div(10.0.pow(n = decimals ?: 0))?.toFloat()

        /**
         * Receipt value pattern - A list of regex patterns to match against.
         */
        enum class ReceiptValuePattern(private val pattern: String) {
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

        enum class FoundValues {
            NONE, TOTAL, PPG, GALS, TOTAL_PPG, TOTAL_GALS, PPG_GALS, ALL
        }
    }
}