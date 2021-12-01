package com.wtb.dashTracker.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.DashEntry
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@ExperimentalCoroutinesApi
class MainActivityViewModel : ViewModel() {

    val repository = Repository.get()

    private val _hourly = MutableLiveData(0f)
    val hourly: LiveData<Float>
        get() = _hourly

    private val _thisWeek = MutableLiveData(0f)
    val thisWeek: LiveData<Float>
        get() = _thisWeek

    init {
        viewModelScope.launch {
            repository.allEntries.collectLatest {
                Log.d(TAG, "NUM_ENTRIES_A: ${it.size}")
                _hourly.value = getHourly(it)
            }
        }

        viewModelScope.launch {
            val (startDate, endDate) = getThisWeeksDateRange()
            repository.getEntriesByDate(startDate, endDate).collectLatest {
                Log.d(TAG, "NUM_ENTRIES_B: ${it.size}")
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

        fun getTotalPay(entries: List<DashEntry>): Float {
            val map = entries.map { (it.pay ?: 0f) + (it.otherPay ?: 0f) }
            val reduce: Float = if (map.isNotEmpty())
                map.reduce { acc: Float?, fl: Float? -> (acc ?: 0f) + (fl ?: 0f) } ?: 0f
            else 0f

            Log.d(TAG, "ENTRIES: ${entries.size} $map $reduce")

            return reduce
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
