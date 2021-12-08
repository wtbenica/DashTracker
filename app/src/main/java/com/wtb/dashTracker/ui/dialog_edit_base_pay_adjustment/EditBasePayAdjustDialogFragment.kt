package com.wtb.dashTracker.ui.dialog_edit_base_pay_adjustment

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.database.models.BasePayAdjustment
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class EditBasePayAdjustDialogFragment(
    private var basePayAdjustment: BasePayAdjustment? = null
): DialogFragment() {
    private val viewModel: BasePayAdjustViewModel by viewModels()
}