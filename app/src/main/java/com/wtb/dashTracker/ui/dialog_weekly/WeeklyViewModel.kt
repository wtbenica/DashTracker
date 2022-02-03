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

package com.wtb.dashTracker.ui.dialog_weekly

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate

@ExperimentalCoroutinesApi
class WeeklyViewModel : BaseViewModel<Weekly>() {
    override fun getItemFlowById(id: Int): Flow<Weekly?> =
        repository.getBasePayAdjustFlowById(id)

    private val _date = MutableStateFlow(LocalDate.now().endOfWeek.minusDays(7))
    val date: StateFlow<LocalDate>
        get() = _date

    val weekly: LiveData<CompleteWeekly?> = date.flatMapLatest {
        repository.getWeeklyByDate(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    ).asLiveData()

    fun loadDate(date: LocalDate) {
        _date.value = date
    }

    val allWeekliesPaged: Flow<PagingData<CompleteWeekly>> = repository.allWeekliesPaged
}