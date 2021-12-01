package com.wtb.dashTracker.ui.daily

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.example.bottomnav.database.DashEntry
import com.wtb.gigtracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class DailyViewModel : ViewModel() {
    fun delete(entry: DashEntry) = repository.deleteEntry(entry)

    protected val repository: Repository = Repository.get()
    val entryList: Flow<PagingData<DashEntry>> = repository.allEntriesPaged
}