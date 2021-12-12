package com.wtb.dashTracker.ui.dialog_entry

import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class EntryViewModel : BaseViewModel<DashEntry>() {
    override fun getItemFlowById(id: Int): Flow<DashEntry?> =
        repository.getEntryFlowById(id)

    companion object {
        private const val TAG = APP + "EntryViewModel"
    }
}