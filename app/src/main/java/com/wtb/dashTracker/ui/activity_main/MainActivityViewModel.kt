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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.database.models.*
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@ExperimentalCoroutinesApi
class MainActivityViewModel : ViewModel() {
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

    private val _hourly = MutableLiveData(0f)
    val hourly: LiveData<Float>
        get() = _hourly

    private val _thisWeek = MutableLiveData(0f)
    val thisWeek: LiveData<Float>
        get() = _thisWeek

    private val _lastWeek = MutableLiveData(0f)
    val lastWeek: LiveData<Float>
        get() = _lastWeek

    private val _cpm = MutableLiveData(0f)
    val cpm: LiveData<Float>
        get() = _cpm

    init {
        viewModelScope.launch {
            repository.allWeeklies.collectLatest {
                _hourly.value = getHourlyFromWeeklies(it)
            }
        }

        viewModelScope.launch {
            repository.getWeeklyByDate(LocalDate.now().endOfWeek).collectLatest { tw ->
                _thisWeek.value = tw?.totalPay
            }
        }

        viewModelScope.launch {
            repository.getWeeklyByDate(LocalDate.now().endOfWeek.minusDays(7)).collectLatest { lw ->
                _lastWeek.value = lw?.totalPay
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
        withContext(Dispatchers.Default) {
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
