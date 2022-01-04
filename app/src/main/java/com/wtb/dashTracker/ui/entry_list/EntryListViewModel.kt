package com.wtb.dashTracker.ui.entry_list

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class EntryListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    val entryList: Flow<PagingData<DashEntry>> = repository.allEntriesPaged

    fun delete(entry: DashEntry) = repository.deleteModel(entry)

    fun deleteEntryById(id: Int) = repository.deleteEntryById(id)
}