/*
 * Copyright 2022 Wesley T. Benica
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

package com.wtb.dashTracker.ui.fragment_income.fragment_yearlies

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.lifecycle.ViewModel
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.database.models.FullExpense
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.fragment_list_item_base.aggregate_list_items.Yearly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
class YearlyListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    private val yearlyBasePayAdjustments: Flow<MutableMap<Int, Float>> =
        repository.allWeeklies.flatMapLatest { weeklies: List<FullWeekly> ->
            flow {
                emit(
                    mutableMapOf<Int, Float>().apply {
                        weeklies.forEach {
                            this[it.weekly.date.year] = (this[it.weekly.date.year] ?: 0f) +
                                    (it.weekly.basePayAdjustment ?: 0f)
                        }
                    }
                )
            }
        }

    private val allEntries: Flow<List<DashEntry>> = repository.allEntries

    private val allExpenses: Flow<List<FullExpense>> = repository.allExpenses

    internal val yearlies: Flow<List<Yearly>> =
        combine(
            yearlyBasePayAdjustments,
            allEntries,
            allExpenses
        ) { bpas: MutableMap<Int, Float>, entries: List<DashEntry>, expenses: List<FullExpense> ->

            val yearlies = mutableListOf<Yearly>()

            var numChecked = 0

            var year: Int =
                entries.maxOfOrNull { it.date.year } ?: LocalDate.now().year

            while (numChecked < entries.size) {
                val thisYearsEntries: List<DashEntry> = entries.mapNotNull { entry: DashEntry ->
                    if (entry.date.year == year) entry else null
                }
                numChecked += thisYearsEntries.size

                val thisYearsExpenses: Map<ExpensePurpose, Float> =
                    expenses.mapNotNull { entry: FullExpense ->
                        if (entry.expense.date.year == year) entry else null
                    }.fold(mutableMapOf()) { res, fe ->
                        res[fe.purpose] =
                            res.getOrDefault(fe.purpose, 0f) + (fe.expense.amount ?: 0f)
                        res
                    }

                val res = Yearly(year).apply {
                    basePayAdjustment = bpas[year] ?: 0f
                    this.expenses = thisYearsExpenses
                }

                if (thisYearsEntries.isNotEmpty()) {
                    thisYearsEntries.forEach { entry: DashEntry ->
                        res.addEntry(entry)
                    }
                    yearlies.add(res)
                }
                year -= 1
            }

            yearlies
        }

    internal suspend fun getAnnualCostPerMile(year: Int, purpose: DeductionType): Map<Int, Float>? =
        repository.getAnnualCostPerMile(year, purpose)

    internal fun standardMileageDeductionTable() = repository.standardMileageDeductionTable
}