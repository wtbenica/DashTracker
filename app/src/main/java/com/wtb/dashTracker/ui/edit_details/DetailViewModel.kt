package com.wtb.gigtracker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.database.AUTO_ID
import com.wtb.dashTracker.database.DashEntry
import com.wtb.dashTracker.database.DataModel
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@ExperimentalCoroutinesApi
class DetailViewModel: ViewModel() {
    private val repository = Repository.get()

    private val _entryId = MutableStateFlow(AUTO_ID)
    val entryId: StateFlow<Int>
        get() = _entryId

    internal val entry: StateFlow<DashEntry?> = entryId.flatMapLatest { id ->
        repository.getEntryFlowById(id)
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = null
    )

    fun loadEntry(entryId: Int?) {
        _entryId.value = entryId ?: AUTO_ID
    }

    fun insert(dataModel: DataModel) {
        when (dataModel) {
            is DashEntry -> repository.saveEntry(dataModel)
        }
    }

    fun upsert(dataModel: DataModel) {
        when (dataModel) {
            is DashEntry -> repository.upsertEntry(dataModel)
        }
    }

    fun delete(dataModel: DataModel) {
        when (dataModel) {
            is DashEntry -> repository.deleteEntry(dataModel)
        }
    }

    fun clearEntry() {
        _entryId.value = AUTO_ID
    }
}