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

package com.wtb.dashTracker.ui.fragment_expenses.fragment_monthlies

import androidx.lifecycle.ViewModel
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.database.models.FullExpense
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

@ExperimentalCoroutinesApi
class MonthlyExpenseListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    private val allExpenses: Flow<List<FullExpense>> = repository.allExpenses

    private val allEntries: Flow<List<DashEntry>> = repository.allEntries

    internal val expensePurposes: Flow<List<ExpensePurpose>> = allExpenses.flatMapLatest {
        flow {
            emit(
                it.fold(mutableSetOf<ExpensePurpose>()) { acc, fullExpense ->
                    acc.add(fullExpense.purpose)
                    acc
                }.toList()
            )
        }
    }

    internal val monthlies: Flow<List<MonthlyExpenses>> =
        combine(allExpenses, allEntries) { expenses: List<FullExpense>, entries: List<DashEntry> ->
            // Collect all expenses
            val monthlyMap: MutableMap<LocalDate, MonthlyExpenses> =
                expenses.fold(mutableMapOf()) { acc, fullExpense ->
                    val firstOfMonth = fullExpense.expense.date.withDayOfMonth(1)
                    acc.getOrPut(firstOfMonth) { MonthlyExpenses(firstOfMonth) }
                        .addExpense(fullExpense.purpose, fullExpense.expense.amount ?: 0f)
                    acc
                }

            // collect mileage from monthly expenses
            entries.forEach {
                val firstOfMonth = it.date.withDayOfMonth(1)
                val monthlyExpenses = monthlyMap.getOrPut(firstOfMonth) { MonthlyExpenses(firstOfMonth) }
                monthlyExpenses.addEntry(it)
            }

            // get average between beginnings and ends of months;
            monthlyMap.values.fix()
        }
}