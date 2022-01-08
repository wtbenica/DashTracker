package com.wtb.dashTracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.reflect.KProperty1

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


    fun export() {
        repository.export()
    }

    companion object {
        private const val TAG = APP + "MainActivityViewModel"

        fun getTotalPay(entries: List<DashEntry>): Float? {
            val map: List<Float> = entries.mapNotNull { entry -> entry.totalEarned }

            return if (map.isNotEmpty()) {
                map.reduce { acc, fl -> acc + fl }
            } else {
                null
            }
        }

        fun getTotalByField(
            entries: List<DashEntry>,
            kProperty1: KProperty1<DashEntry, Int?>
        ): Int? {
            val map = entries.map { kProperty1(it) }

            return if (map.isNotEmpty() && !map.contains(null)) {
                map.reduce { acc: Int?, fl: Int? -> (acc ?: 0) + (fl ?: 0) }
            } else {
                null
            }
        }

        fun getTotalByField(
            entries: List<DashEntry>,
            kProperty1: KProperty1<DashEntry, Float?>
        ): Float? {
            val map = entries.mapNotNull(kProperty1)

            return if (map.isNotEmpty()) {
                map.reduce { acc: Float, fl: Float -> acc + fl }
            } else {
                null
            }
        }

        fun getHourlyFromWeeklies(list: List<CompleteWeekly>): Float {
            val hours = list.map { w -> w.hours }.reduce { acc, fl -> acc + fl }
            val pay = list.map { w -> w.pay }.reduce { acc, fl -> acc + fl }
            val adjusts = list.map { it.weekly.basePayAdjustment }
                .reduce { acc, fl -> (acc ?: 0f) + (fl ?: 0f) } ?: 0f

            return (pay + adjusts) / hours
        }

    }
}
