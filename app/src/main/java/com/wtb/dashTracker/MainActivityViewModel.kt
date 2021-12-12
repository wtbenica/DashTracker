package com.wtb.dashTracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.models.DashEntry
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
            repository.allEntries.collectLatest {
                _hourly.value = getHourly(it)
            }
        }

        viewModelScope.launch {
            repository.allEntries.collectLatest { entries ->
                _thisWeek.value = getTotalPay(entries.filter { it.isXWeeksAgo(0) })
                _lastWeek.value = getTotalPay(entries.filter { it.isXWeeksAgo(1) })
            }
            val (startDate, endDate) = getThisWeeksDateRange()
            repository.getEntriesByDate(startDate, endDate).collectLatest {
                _thisWeek.value = getTotalPay(it)
            }
        }
    }


    companion object {
        private const val TAG = APP + "MainActivityViewModel"

        fun getThisWeeksDateRange(): Pair<LocalDate, LocalDate> {
            val todayIs = LocalDate.now().dayOfWeek
            val weekEndsOn = DayOfWeek.SUNDAY
            val daysLeft = (weekEndsOn.value - todayIs.value + 7) % 7L
            val endDate = LocalDate.now().plusDays(daysLeft)
            val startDate = endDate.minusDays(6L)
            return Pair(startDate, endDate)
        }

        fun getTotalPay(entries: List<DashEntry>): Float? {
            val map: List<Float?> = entries.map { entry ->
                entry.pay?.let { pay -> pay + (entry.otherPay ?: 0f) + (entry.cashTips ?: 0f) } }

            return if (map.isNotEmpty() && !map.contains(null)) {
                map.reduce { acc: Float?, fl: Float? -> (acc ?: 0f) + (fl ?: 0f) }
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
            val map = entries.map { kProperty1(it) }

            return if (map.isNotEmpty() && !map.contains(null)) {
                map.reduce { acc: Float?, fl: Float? -> (acc ?: 0f) + (fl ?: 0f) }
            } else {
                null
            }
        }

        fun getHourly(entries: List<DashEntry>): Float {
            val hours: Float = entries.map { entry -> entry.totalHours }
                .reduce { acc: Float?, fl: Float? -> (acc ?: 0f) + (fl ?: 0f) } ?: 0f
            val pay: Float = entries.mapNotNull { entry -> entry.totalEarned }
                .reduce { acc: Float, fl: Float -> acc + fl }

            return pay / hours
        }
    }
}
