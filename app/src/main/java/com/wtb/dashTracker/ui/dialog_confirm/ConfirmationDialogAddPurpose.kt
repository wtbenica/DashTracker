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
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResult
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DialogFragConfirmAddPurposeBinding
import com.wtb.dashTracker.views.FullWidthDialogFragment


class ConfirmationDialogAddPurpose(
    private val purposeId: Int,
    private val prevPurpose: Int? = null,
    private val currentName: String? = null
) : FullWidthDialogFragment() {

    private lateinit var binding: DialogFragConfirmAddPurposeBinding
    private var deleteButtonPressed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragConfirmAddPurposeBinding.inflate(inflater)

        currentName?.let {
            binding.newExpenseToolbar.title = getString(R.string.dialog_title_edit_expense_type)
            binding.dialogPurposeEditText.setText(it)
            binding.theQuestion.setText(R.string.message_edit_expense)
        }

        binding.dialogPurposeEditText.doOnTextChanged { text: CharSequence?, _, _, _ ->
            binding.yesButton1.isEnabled = !text.isNullOrBlank()
        }

        binding.noButton.setOnClickListener {
            deleteButtonPressed = true
            dismiss()
        }

        binding.yesButton1.apply {
            setOnClickListener {
                dismiss()
                setFragmentResult(
                    RK_ADD_PURPOSE,
                    bundleOf(
                        ARG_CONFIRM to true,
                        ARG_PURPOSE_NAME to binding.dialogPurposeEditText.text.toString()
                            .replaceFirstChar { it.uppercase() },
                        ARG_PURPOSE_ID to purposeId,
                    )
                )
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        val text: CharSequence? = binding.dialogPurposeEditText.text
        val deleteOnExit = text.isNullOrBlank() || deleteButtonPressed
        if (deleteOnExit) {
            setFragmentResult(
                RK_ADD_PURPOSE,
                bundleOf(
                    ARG_CONFIRM to false,
                    ARG_PURPOSE_ID to purposeId,
                    ARG_PREV_PURPOSE to prevPurpose,
                )
            )
        } else {
            setFragmentResult(
                RK_ADD_PURPOSE,
                bundleOf(
                    ARG_CONFIRM to true,
                    ARG_PURPOSE_NAME to text.toString()
                        .replaceFirstChar { it.uppercase() },
                    ARG_PURPOSE_ID to purposeId,
                )
            )
        }
        super.onDestroy()
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