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

package com.wtb.dashTracker.ui.fragment_expenses.fragment_monthlies

import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.extensions.isPreviousMonth
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

@ExperimentalCoroutinesApi
fun MutableCollection<MonthlyExpenses>.fix(): List<MonthlyExpenses> {
    val res = this.sortedByDescending { it.date }

    res.forEachIndexed { index, monthlyExpenses ->
        val prevMonthlyExpense = res.getOrNull(index + 1)
        monthlyExpenses.adjustEndStartOdometers(prevMonthlyExpense)
    }

    return res
}

@ExperimentalCoroutinesApi
class MonthlyExpenses(val date: LocalDate) : ListItemType {
    private val _expenses: MutableMap<ExpensePurpose, Float> = mutableMapOf()
    val expenses: Map<ExpensePurpose, Float>
        get() = _expenses

    internal var startOdometer: Float? = null
    internal var endOdometer: Float? = null
    internal var workMiles: Float = 0f

    internal val totalMiles: Int
        get() = endOdometer?.let { eo -> startOdometer?.let { so -> eo - so } }?.toInt() ?: 0

    private val workMilePercent: Float
        get() = if (totalMiles != 0) {
            workMiles / totalMiles
        } else {
            0f
        }

    internal val workMilePercentDisplay: Int
        get() = (workMilePercent * 100).toInt()

    internal val workCost: Float
        get() = workMilePercent * total

    val total: Float
        get() = expenses.values.reduceOrNull { acc, v ->
            acc + v
        } ?: 0f

    val showInList: Boolean
        get() = total > 0f || workMiles > 0f

    fun addExpense(purpose: ExpensePurpose, amount: Float) {
        val current = _expenses.getOrDefault(purpose, 0f)
        _expenses[purpose] = current + amount
    }

    fun addEntry(entry: DashEntry) {
        val newStart: Float? = entry.startOdometer
        val oldStart: Float? = startOdometer

        startOdometer =
            if (newStart != null && oldStart != null) {
                java.lang.Float.min(newStart, oldStart)
            } else newStart ?: oldStart

        val newEnd: Float? = entry.endOdometer
        val oldEnd: Float? = endOdometer

        endOdometer =
            if (newEnd != null && oldEnd != null) {
                java.lang.Float.max(newEnd, oldEnd)
            } else newEnd ?: oldEnd

        val addedMiles = entry.mileage ?: 0f
        workMiles += addedMiles
    }

    fun getAmount(purpose: ExpensePurpose): Float = expenses.getOrDefault(purpose, 0f)

    fun adjustEndStartOdometers(other: MonthlyExpenses?) {
        if (other != null && this.date.isPreviousMonth(other.date)) {
            val currStart = this.startOdometer
            val prevEnd = other.endOdometer
            when {
                currStart == null && prevEnd != null -> {
                    this.startOdometer = prevEnd
                }
                currStart != null && prevEnd == null -> {
                    other.endOdometer = currStart
                }
                currStart != null &&
                        prevEnd != null &&
                        currStart > prevEnd -> {
                    val mid: Float = (currStart + prevEnd) / 2
                    this.startOdometer = mid
                    other.endOdometer = mid
                }
            }
        }
    }
}