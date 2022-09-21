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

package com.wtb.dashTracker.ui.fragment_list_item_base.fragment_yearlies

import androidx.lifecycle.ViewModel
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.Month

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

    internal val yearlies: Flow<List<Yearly>> =
        combine(
            yearlyBasePayAdjustments,
            allEntries
        ) { bpas: MutableMap<Int, Float>, entries: List<DashEntry> ->
            val bpaList = bpas

            val yearlies = mutableListOf<Yearly>()

            yearlies.forEach {
                it.basePayAdjustment = bpaList[it.year] ?: 0f
            }

            var numChecked = 0

            var year: Int =
                entries.map { it.date.year }.maxOrNull() ?: LocalDate.now().year

            while (numChecked < entries.size) {
                val thisYears: List<DashEntry> = entries.mapNotNull { entry: DashEntry ->
                    if (entry.date.year == year) entry else null
                }
                numChecked += thisYears.size
                val res = Yearly(year).apply {
                    basePayAdjustment = bpaList[year] ?: 0f
                }

                if (thisYears.isNotEmpty()) {
                    thisYears.forEach { entry: DashEntry ->
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

class Yearly(val year: Int) : ListItemType {
    val monthlies = mutableMapOf<Month, Monthly>().apply {
        Month.values().forEach { this[it] = Monthly() }
    }

    operator fun get(month: Month): Monthly? = monthlies[month]

    var basePayAdjustment: Float = 0f

    // TODO: Need to still add in bpa
    // TODO: Figure out what the above TODO means
    val reportedPay: Float
        get() = monthlies.values.fold(0f) { acc, monthly -> acc + monthly.reportedPay } + basePayAdjustment

    val cashTips: Float
        get() = monthlies.values.fold(0f) { acc, monthly -> acc + monthly.cashTips }

    val hourly: Float
        get() = totalPay / hours

    internal val totalPay: Float
        get() = reportedPay + cashTips

    val hours: Float
        get() = monthlies.values.fold(0f) { acc, monthly -> acc + monthly.hours }

    val mileage: Float
        get() = monthlies.values.fold(0f) { acc, monthly -> acc + monthly.mileage }

    fun addEntry(entry: DashEntry) {
        monthlies[entry.date.month]?.addEntry(entry)
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Yearly

        if (year != other.year) return false

        return true
    }

    override fun hashCode(): Int {
        return year
    }
}

data class Monthly(
    var mileage: Float = 0f,
    var pay: Float = 0f,
    var otherPay: Float = 0f,
    var cashTips: Float = 0f,
    var hours: Float = 0f
) : ListItemType {
    val reportedPay: Float
        get() = pay + otherPay

    internal val totalPay: Float
        get() = reportedPay + cashTips

    val hourly: Float
        get() = totalPay / hours

    fun getExpenses(costPerMile: Float): Float = mileage * costPerMile

    fun getNet(cpm: Float): Float = totalPay - getExpenses(cpm)

    fun getHourly(cpm: Float): Float = getNet(cpm) / hours

    fun addEntry(entry: DashEntry) {
        mileage += entry.mileage ?: 0f
        pay += entry.pay ?: 0f
        otherPay += entry.otherPay ?: 0f
        cashTips += entry.cashTips ?: 0f
        hours += entry.totalHours ?: 0f
    }
}
