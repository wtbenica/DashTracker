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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.databinding.DialogFragConfirmEditPurposesBinding
import com.wtb.dashTracker.ui.dialog_expense.ExpenseViewModel
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class ConfirmationDialogEditPurposes() : FullWidthDialogFragment() {

    private lateinit var binding: DialogFragConfirmEditPurposesBinding
    private val viewModel: ExpenseViewModel by viewModels()
    private var itemList: List<ExpensePurpose>? = null

    private var deleteButtonPressed = false

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
                viewModel.expensePurposes.collectLatest { itemList ->
                    binding.purposeChipgroup.removeAllViews()
                    itemList.forEach {
                        binding.purposeChipgroup.addView(object : Chip(context) {
                            private val ep: ExpensePurpose = it

                            init {
                                text = it.name

                                setOnClickListener {
                                    this.isCloseIconVisible = !this.isCloseIconVisible
                                    this.isChipIconVisible = !this.isChipIconVisible
                                }

                                setOnCloseIconClickListener {
                                    viewModel.delete(ep)
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = APP + "ConfirmDialogAddPurpose"
        const val ARG_CONFIRM = "confirm"
        const val ARG_PURPOSE_NAME = "arg_purpose_name"
        const val ARG_PURPOSE_ID = "arg_purpose_id"
        const val ARG_PREV_PURPOSE = "arg_prev_purpose"
        const val RK_ADD_PURPOSE = "add_purpose"
    }
}