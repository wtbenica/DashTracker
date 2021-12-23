package com.wtb.dashTracker.ui.weekly_list

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class WeeklyListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    val entryList: Flow<PagingData<Weekly>> = repository.allWeekliesPaged

    fun delete(entry: Weekly) = repository.deleteModel(entry)
}