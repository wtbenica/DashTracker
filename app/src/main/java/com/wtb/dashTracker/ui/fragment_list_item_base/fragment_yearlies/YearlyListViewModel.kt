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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@ExperimentalCoroutinesApi
class YearlyListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    val yearlyBasePayAdjustments: Flow<MutableMap<Int, Float>> =
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

    val allEntries: Flow<List<DashEntry>> = repository.allEntries

    suspend fun getAnnualCostPerMile(year: Int, purpose: DeductionType): Float =
        repository.getAnnualCostPerMile(year, purpose)

    fun standardMileageDeductionTable() = repository.standardMileageDeductionTable
}