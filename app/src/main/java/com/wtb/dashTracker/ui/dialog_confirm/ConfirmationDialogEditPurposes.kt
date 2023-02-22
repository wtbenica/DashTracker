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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.wtb.dashTracker.database.models.FullExpensePurpose
import com.wtb.dashTracker.databinding.DialogFragConfirmEditPurposesBinding
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_expense.ExpenseViewModel
import com.wtb.dashTracker.ui.fragment_trends.FullWidthDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
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
}