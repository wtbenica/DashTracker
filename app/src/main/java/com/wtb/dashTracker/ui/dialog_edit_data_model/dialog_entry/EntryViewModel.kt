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

package com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry

import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalTime

@ExperimentalCoroutinesApi
class EntryViewModel : ListItemViewModel<DashEntry>() {
    internal val fullDash: StateFlow<FullEntry?> = id.flatMapLatest { id ->
        repository.getFullEntryFlowById(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    override fun getItemFlowById(id: Long): Flow<DashEntry?> =
        repository.getEntryFlowById(id)

    fun updateEntry(
        entry: DashEntry,
        date: LocalDate? = null,
        endDate: LocalDate? = null,
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
        startOdometer: Float? = null,
        endOdometer: Float? = null,
        pay: Float? = null,
        otherPay: Float? = null,
        cashTips: Float? = null,
        numDeliveries: Int? = null
    ) {
        val mDate = date ?: entry.date
        val mEndDate = endDate ?: entry.endDate
        val mStartTime = startTime ?: entry.startTime
        val mEndTime = endTime ?: entry.endTime
        val mStartOdometer = startOdometer ?: entry.startOdometer
        val mEndOdometer = endOdometer ?: entry.endOdometer
        val mPay = pay ?: entry.pay
        val mOtherPay = otherPay ?: entry.otherPay
        val mCashTips = cashTips ?: entry.cashTips
        val mNumDeliveries = numDeliveries ?: entry.numDeliveries

        @Suppress("DEPRECATION")
        val newEntry = DashEntry(
            entryId = entry.entryId,
            date = mDate,
            endDate = mEndDate,
            startTime = mStartTime,
            endTime = mEndTime,
            startOdometer = mStartOdometer,
            endOdometer = mEndOdometer,
            totalMileage = entry.totalMileage,
            pay = mPay,
            otherPay = mOtherPay,
            cashTips = mCashTips,
            numDeliveries = mNumDeliveries,
            week = entry.week
        )

        upsert(newEntry)
    }
}