package com.wtb.dashTracker.ui.dialog_weekly

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.wtb.dashTracker.database.models.DashEntry
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

    val weekly: LiveData<Weekly?> = date.flatMapLatest {
        repository.getWeeklyByDate(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    ).asLiveData()

    fun loadDate(date: LocalDate) {
        _date.value = date
    }

    fun getEntriesByDate(startDate: LocalDate, endDate: LocalDate): LiveData<List<DashEntry>> =
        repository.getEntriesByDate(startDate, endDate).asLiveData()

    fun getWeeklyFlowByDate(date: LocalDate): Flow<Weekly?> =
        repository.getWeeklyByDate(date)

    val entriesByWeek: LiveData<List<DashEntry>?> = date.flatMapLatest {
        repository.getEntriesByWeek(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    ).asLiveData()

    val allWeekliesPaged: Flow<PagingData<Weekly>> = repository.allWeekliesPaged
}