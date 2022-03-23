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
import androidx.fragment.app.setFragmentResult
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.databinding.DialogFragConfirmAddPurposeBinding
import com.wtb.dashTracker.views.FullWidthDialogFragment


open class ConfirmationDialogAddPurpose() : FullWidthDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val binding = DialogFragConfirmAddPurposeBinding.inflate(inflater)

        binding.noButton.setOnClickListener {
            dismiss()
            setFragmentResult(
                RK_ADD_PURPOSE,
                bundleOf(ARG_CONFIRM to false,)
            )
        }

        binding.yesButton1.apply {
            setOnClickListener {
                dismiss()
                setFragmentResult(
                    RK_ADD_PURPOSE,
                    bundleOf(
                        ARG_CONFIRM to true,
                        ARG_PURPOSE_NAME to binding.dialogPurposeEditText.text.toString(),
                    )
                )
            }
        }

        return binding.root
    }

    companion object {
        const val TAG = APP + "ConfirmDialogAddPurpose"
        const val ARG_CONFIRM = "confirm"
        const val ARG_PURPOSE_NAME = "arg_purpose_name"
        const val ARG_PURPOSE_ID = "arg_purpose_id"
        const val RK_ADD_PURPOSE = "add_purpose"
    }
}