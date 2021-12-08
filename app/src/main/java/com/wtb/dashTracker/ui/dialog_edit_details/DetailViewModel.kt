package com.wtb.dashTracker.ui.dialog_edit_details

import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class DetailViewModel : BaseViewModel<DashEntry>() {
    override fun getItemFlowById(id: Int): Flow<DashEntry?> =
        repository.getEntryFlowById(id)
}