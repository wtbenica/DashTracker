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

package com.wtb.dashTracker.ui.fragment_list_item_base.aggregate_list_items

import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Month

@ExperimentalCoroutinesApi
class Yearly(val year: Int) : ListItemType {
    val monthlies: MutableMap<Month, Monthly> = mutableMapOf<Month, Monthly>().apply {
        Month.values().forEach { this[it] = Monthly() }
    }

    operator fun get(month: Month): Monthly? = monthlies[month]

    var basePayAdjustment: Float = 0f

    var startOdometer: Float? = null

    var endOdometer: Float? = null

    var expenses: Map<ExpensePurpose, Float>? = null

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

    val totalMiles: Float
        get() {
            val e = endOdometer
            val s = startOdometer
            return if (e != null && s != null) {
                e - s
            } else {
                0f
            }
        }

    val nonBusinessMiles: Float
        get() = totalMiles - mileage

    val businessMileagePercent: Float
        get() = mileage / totalMiles

    fun addEntry(entry: DashEntry) {
        monthlies[entry.date.month]?.addEntry(entry)
        val entrySo = entry.startOdometer
        val entryEo = entry.endOdometer
        val so = startOdometer
        val eo = endOdometer
        if (entrySo != 0f && entryEo != 0f) {
            if (entry.startOdometer != null && (so == null || so > entry.startOdometer)
            ) {
                startOdometer = entry.startOdometer
            }
            if (entry.endOdometer != null && (eo == null || eo < entry.endOdometer)
            ) {
                endOdometer = entry.endOdometer
            }
        }
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

