package com.wtb.dashTracker.ui.daily_stats

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.wtb.dashTracker.database.DashEntry
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import java.time.DayOfWeek

@ExperimentalCoroutinesApi
class DailyStatsViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    internal val entryList: Flow<List<DashEntry>> = repository.allEntries
}