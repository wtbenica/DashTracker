package com.wtb.dashTracker.ui.yearly_list

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.repository.Repository
import com.wtb.dashTracker.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class YearlyListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    val allWeeklies: Flow<List<CompleteWeekly>> = repository.allWeeklies

    fun delete(entry: Weekly) = repository.deleteModel(entry)
}