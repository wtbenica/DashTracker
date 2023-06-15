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

import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.ui.activity_main.ReceiptAnalyzer.Companion.dollarStringToFloat

class ExpenseFactory {
    companion object {
        fun getExpense(
            total: Float? = null,
            gallons: Float? = null,
            ppg: Float? = null,
            gallonsList: List<Float> = emptyList(),
            ppgList: List<String> = emptyList()
        ): Expense? {
            val pEmpty = ppgList.isEmpty()
            val gEmpty = gallonsList.isEmpty()
            val aEmpty = total == null

            return when {
                aEmpty && pEmpty -> null
                pEmpty -> getExpenseFromTotalsAndGallons(gallons = gallons, total = total)
                gEmpty -> Expense(amount = total, pricePerGal = ppg)
                aEmpty -> getExpenseFromPG(gallons = gallons, ppg = ppg)
                else -> getExpenseFromTPG(
                    total = total,
                    gallons = gallonsList,
                    ppgs = ppgList,
                    ppg = ppg
                )
            }
        }

        /**
         * @return [Expense] with [Expense.pricePerGal] set to the calculated value of
         * [total]/[gallons].
         */
        private fun getExpenseFromTotalsAndGallons(
            gallons: Float?,
            total: Float?
        ): Expense? {
            val ppg = gallons?.let { g -> total?.let { a -> a / g } }
            return ppg?.let { Expense(pricePerGal = ppg) }
        }

        /**
         * @return [Expense] with [Expense.amount] set to the calculated value of
         * [gallons] * [ppg].
         */
        private fun getExpenseFromPG(
            gallons: Float?,
            ppg: Float?
        ): Expense? {
            val amount = gallons?.let { g -> ppg?.let { p -> g * p } }
            return amount?.let { Expense(amount = amount, pricePerGal = ppg) }
        }

        /**
         * Get expense from APG - Returns an [Expense] based on the best guess
         * combination of ppg and gallons from [ppgs] and [gallons] that work with
         * [total]. If no combination can be found, then [total] and [ppg] are used
         * to create the [Expense].
         *
         * @return
         */
        private fun getExpenseFromTPG(
            total: Float?,
            gallons: List<Float>,
            ppgs: List<String>,
            ppg: Float?
        ): Expense {
            val mPpg: Float?
            val amount: Float? = total

            if (listOf(gallons, ppgs).all { it.count() == 1 }) {
                mPpg = ppg
            } else { // multiple ppgs or gallons candidates
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

                mPpg = if (candidates.isNotEmpty()) {
                    // TODO: add logic to pick the best candidate
                    candidates.first().first?.dollarStringToFloat(3)
                } else {
                    ppg
                }
            }
            return Expense(amount = amount, pricePerGal = mPpg)
        }
    }
}