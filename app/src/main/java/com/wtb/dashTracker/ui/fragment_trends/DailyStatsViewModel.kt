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

package com.wtb.dashTracker.ui.fragment_trends

import androidx.lifecycle.ViewModel
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
val Any.TAG: String
    get() = APP + this::class.simpleName

@ExperimentalCoroutinesApi
class DailyStatsViewModel : ViewModel() {
    companion object {
        const val MIN_NUM_WEEKS = 8
    }

    private val repository: Repository = Repository.get()

    internal val entryList: Flow<List<DashEntry>> = repository.allEntries
    internal val weeklyList: Flow<List<FullWeekly>> = repository.allWeeklies

    suspend fun getExpensesAndCostPerMile(
        compWeekly: FullWeekly,
        deductionType: DeductionType
    ): Pair<Float, Float> =
        CoroutineScope(Dispatchers.Default).async {
            var expenses = 0f
            withContext(Dispatchers.Default) {
                compWeekly.entries.forEach { entry ->
                    withContext(Dispatchers.Default) {
                        repository.getCostPerMile(entry.date, deductionType)
                    }.let { cpm: Float? ->
                        expenses += entry.getExpenses(cpm ?: 0f)
                    }
                }.let {
                    return@let Pair(expenses, expenses / compWeekly.miles)
                }
            }
        }.await()
}