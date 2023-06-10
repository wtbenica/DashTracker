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

package com.wtb.dashTracker.ui.activity_main

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.database.models.*
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class MainActivityViewModel(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) : ViewModel() {
    private val repository = Repository.get()

    private val _activeEntryId = MutableStateFlow(AUTO_ID)
    private val activeEntryId: StateFlow<Long>
        get() = _activeEntryId

    fun loadActiveEntry(id: Long?) {
        _activeEntryId.value = id ?: AUTO_ID
    }

    internal val activeEntry: StateFlow<FullEntry?> = activeEntryId.flatMapLatest { id ->
        repository.getFullEntryFlowById(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _thisWeekEarnings = MutableLiveData(0f)
    val thisWeekEarnings: LiveData<Float>
        get() = _thisWeekEarnings

    private val _lastWeekEarnings = MutableLiveData(0f)
    val lastWeekEarnings: LiveData<Float>
        get() = _lastWeekEarnings

    private val _thisWeekHourly = MutableLiveData(0f)
    val thisWeekHourly: LiveData<Float>
        get() = _thisWeekHourly

    private val _lastWeekHourly = MutableLiveData(0f)
    val lastWeekHourly: LiveData<Float>
        get() = _lastWeekHourly

    private val _cpm = MutableLiveData(0f)
    val cpm: LiveData<Float>
        get() = _cpm

    init {
        viewModelScope.launch {
            repository.getWeeklyByDate(LocalDate.now().endOfWeek).collectLatest { tw ->
                _thisWeekEarnings.value = tw?.totalPay
                _thisWeekHourly.value = tw?.hourly
            }
        }

        viewModelScope.launch {
            repository.getWeeklyByDate(LocalDate.now().endOfWeek.minusDays(7)).collectLatest { lw ->
                _lastWeekEarnings.value = lw?.totalPay
                _lastWeekHourly.value = lw?.hourly
            }
        }

        viewModelScope.launch {
            repository.getCostPerMileFlow(LocalDate.now(), DeductionType.ALL_EXPENSES)
                .collectLatest {
                    _cpm.value = it ?: 0f
                }
        }
    }

    fun deleteEntry(id: Long) {
        repository.deleteEntryById(id)
    }

    suspend fun insertSus(dataModel: DataModel): Long = repository.saveModelSus(dataModel)

    suspend fun upsertAsync(dataModel: DataModel): Long =
        withContext(dispatcher) {
            repository.upsertModel(dataModel)
        }

    fun export(): Unit = repository.export()

    fun import(activityResultLauncher: ActivityResultLauncher<String>): Unit =
        repository.import(activityResultLauncher)

    fun insertOrReplace(
        entries: List<DashEntry>? = null,
        weeklies: List<Weekly>? = null,
        expenses: List<Expense>? = null,
        purposes: List<ExpensePurpose>? = null,
        locationData: List<LocationData>? = null
    ) {
        repository.insertOrReplace(entries, weeklies, expenses, purposes, locationData)
    }

    fun checkForDuplicateExpense(expense: Expense): Boolean = repository.checkForDuplicate(expense)

    companion object {
        fun getHourlyFromWeeklies(list: List<FullWeekly>): Float {
            return if (list.isNotEmpty()) {
                val hours = list.map { w -> w.hours }.reduce { acc, fl -> acc + fl }
                val pay = list.map { w -> w.pay }.reduce { acc, fl -> acc + fl }
                val adjusts = list.map { it.weekly.basePayAdjustment }
                    .reduce { acc, fl -> (acc ?: 0f) + (fl ?: 0f) } ?: 0f

                (pay + adjusts) / hours
            } else {
                0f
            }
        }
    }
}
