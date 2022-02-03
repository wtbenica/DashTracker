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

package com.wtb.dashTracker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalCoroutinesApi
class MainActivityViewModel : ViewModel() {
    private val repository = Repository.get()

    private val _hourly = MutableLiveData(0f)
    val hourly: LiveData<Float>
        get() = _hourly

    private val _thisWeek = MutableLiveData(0f)
    val thisWeek: LiveData<Float>
        get() = _thisWeek

    private val _lastWeek = MutableLiveData(0f)
    val lastWeek: LiveData<Float>
        get() = _lastWeek

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
    }


    fun export(ctx: Context) = repository.export(ctx)

    fun import(ctx: Context) = repository.import(ctx)

    companion object {

        fun getHourlyFromWeeklies(list: List<CompleteWeekly>): Float {
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
