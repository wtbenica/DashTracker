/*
 * Copyright 2022 Wesley T. Benica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.dashTracker.ui.dialog_confirm

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.database.models.FullExpensePurpose
import com.wtb.dashTracker.databinding.DialogFragConfirmEditPurposesBinding
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose.Companion.ARG_PURPOSE_ID
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose.Companion.ARG_PURPOSE_NAME
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose.Companion.RK_ADD_PURPOSE
import com.wtb.dashTracker.ui.dialog_expense.ExpenseViewModel
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class ConfirmationDialogEditPurposes : FullWidthDialogFragment() {

    private lateinit var binding: DialogFragConfirmEditPurposesBinding
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragConfirmEditPurposesBinding.inflate(inflater)

        binding.purposeChipgroup

        binding.noButton.setOnClickListener {
            dismiss()
        }

        setFragmentResultListener(RK_ADD_PURPOSE) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            bundle.getInt(ARG_PURPOSE_ID).let { id ->
                if (result) {
                    bundle.getString(ARG_PURPOSE_NAME)?.let { purposeName ->
                        viewModel.upsert(ExpensePurpose(purposeId = id, name = purposeName))
                    }
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullPurposes.collectLatest { itemList ->
                    binding.purposeChipgroup.removeAllViews()
                    itemList.forEach {
                        binding.purposeChipgroup.addView(object : Chip(context) {
                            private val ep: FullExpensePurpose = it

                            init {
                                text = it.purpose.name

                                setOnClickListener {
                                    ConfirmationDialogAddOrModifyPurpose.newInstance(
                                        ep.purpose.purposeId,
                                        isNew = false
                                    ).show(parentFragmentManager, null)
                                }

                                isCloseIconVisible = it.expenses.isEmpty()

                                setOnCloseIconClickListener {
                                    viewModel.delete(ep.purpose)
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    companion object {

    }
}