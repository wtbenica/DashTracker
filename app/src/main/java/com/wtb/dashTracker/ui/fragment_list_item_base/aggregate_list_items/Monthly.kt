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
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
data class Monthly(
    var mileage: Float = 0f,
    var pay: Float = 0f,
    var otherPay: Float = 0f,
    var cashTips: Float = 0f,
    var hours: Float = 0f
) : ListItemType {
    val reportedPay: Float
        get() = pay + otherPay

    private val totalPay: Float
        get() = reportedPay + cashTips

    val hourly: Float
        get() = totalPay / hours

    private fun getExpenses(costPerMile: Float): Float = mileage * costPerMile

    private fun getNet(cpm: Float): Float = totalPay - getExpenses(cpm)

    fun getHourly(cpm: Float): Float = getNet(cpm) / hours

    fun addEntry(entry: DashEntry) {
        mileage += entry.mileage ?: 0f
        pay += entry.pay ?: 0f
        otherPay += entry.otherPay ?: 0f
        cashTips += entry.cashTips ?: 0f
        hours += entry.totalHours ?: 0f
    }
}