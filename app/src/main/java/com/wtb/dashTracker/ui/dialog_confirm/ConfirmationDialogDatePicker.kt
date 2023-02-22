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

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.os.bundleOf
import com.wtb.dashTracker.databinding.DialogFragConfirmDatePickerBinding
import com.wtb.dashTracker.ui.fragment_trends.FullWidthDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class ConfirmationDialogDatePicker : FullWidthDialogFragment() {

    private lateinit var binding: DialogFragConfirmDatePickerBinding
    private var textViewId: Int? = null
    private var currentText: LocalDate? = null
    private var headerText: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            headerText = it.getString(ARG_DATE_PICKER_HEADER_TEXT)
            currentText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_DATE_CURRENT_TEXT, LocalDate::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_DATE_CURRENT_TEXT) as LocalDate?
            }
            textViewId = it.getInt(ARG_DATE_TEXTVIEW)
        }

        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val date = currentText ?: LocalDate.now()

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragConfirmDatePickerBinding.inflate(inflater)

        binding.dialogDatePickerToolbar.apply {
            if (headerText?.isEmpty() == true) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                title = headerText
            }
        }

        binding.dialogDatePickerDatePicker.apply {
            updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
        }

        binding.noButton.setOnClickListener {
            dismiss()
        }

        binding.yesButton1.setOnClickListener {
            val picker = binding.dialogDatePickerDatePicker
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY_DATE,
                bundleOf(
                    ARG_DATE_PICKER_NEW_YEAR to picker.year,
                    ARG_DATE_PICKER_NEW_MONTH to picker.month + 1,
                    ARG_DATE_PICKER_NEW_DAY to picker.dayOfMonth,
                    ARG_DATE_TEXTVIEW to textViewId
                )
            )

            dismiss()
        }

        return binding.root
    }

    companion object {
        const val ARG_DATE_TEXTVIEW: String = "arg_date_textview"
        private const val ARG_DATE_CURRENT_TEXT = "arg_date_current_text"

        const val REQUEST_KEY_DATE: String = "requestDatePicker"
        const val ARG_DATE_PICKER_NEW_YEAR: String = "year"
        const val ARG_DATE_PICKER_NEW_MONTH: String = "month"
        const val ARG_DATE_PICKER_NEW_DAY: String = "dayOfMonth"
        const val ARG_DATE_PICKER_HEADER_TEXT: String = "arg_time_picker_header_text"

        @JvmStatic
        fun newInstance(
            @IdRes textViewId: Int,
            currentText: LocalDate,
            headerText: String? = null
        ): ConfirmationDialogDatePicker =
            ConfirmationDialogDatePicker().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DATE_TEXTVIEW, textViewId)
                    putSerializable(ARG_DATE_CURRENT_TEXT, currentText)
                    putString(ARG_DATE_PICKER_HEADER_TEXT, headerText)
                }
            }
    }
}