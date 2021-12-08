package com.wtb.dashTracker.ui.insight_daily_stats

import androidx.lifecycle.ViewModel
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class DailyStatsViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    internal val entryList: Flow<List<DashEntry>> = repository.allEntries
}