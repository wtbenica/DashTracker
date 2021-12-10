package com.wtb.dashTracker.ui.dialog_adjust

import com.wtb.dashTracker.database.models.BasePayAdjustment
import com.wtb.dashTracker.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class AdjustViewModel : BaseViewModel<BasePayAdjustment>() {
    override fun getItemFlowById(id: Int): Flow<BasePayAdjustment?> =
        repository.getBasePayAdjustFlowById(id)
}
