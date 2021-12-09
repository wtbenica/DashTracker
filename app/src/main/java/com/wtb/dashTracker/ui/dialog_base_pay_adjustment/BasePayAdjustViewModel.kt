package com.wtb.dashTracker.ui.dialog_base_pay_adjustment

import com.wtb.dashTracker.database.models.BasePayAdjustment
import com.wtb.dashTracker.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class BasePayAdjustViewModel : BaseViewModel<BasePayAdjustment>() {
    override fun getItemFlowById(id: Int): Flow<BasePayAdjustment?> =
        repository.getBasePayAdjustFlowById(id)
}
