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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.wtb.dashTracker.databinding.DialogFragConfirmTimePickerBinding
import com.wtb.dashTracker.extensions.dtfTime
import com.wtb.dashTracker.ui.fragment_trends.FullWidthDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalTime
import java.time.format.DateTimeParseException

// TODO: selecting a purpose to edit and then cancelling deletes the purpose
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class ConfirmationDialogTimePicker : FullWidthDialogFragment() {

    private lateinit var binding: DialogFragConfirmTimePickerBinding
    private var textViewId: Int? = null
    private var currentText: String? = null
    private var headerText: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            headerText = it.getString(ARG_TIME_PICKER_HEADER_TEXT)
            currentText = it.getString(ARG_TIME_CURRENT_TEXT)
            textViewId = it.getInt(ARG_TIME_TEXTVIEW)
        }

        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragConfirmTimePickerBinding.inflate(inflater)

        binding.dialogTimePickerToolbar.apply {
            if (headerText?.isEmpty() == true) {
                visibility = GONE
            } else {
                visibility = VISIBLE
                title = headerText
            }
        }

        binding.dialogTimePickerTimePicker.apply {
            val time: LocalTime = try {
                LocalTime.parse(currentText, dtfTime)
            } catch (e: DateTimeParseException) {
                LocalTime.now()
            }

            hour = time.hour
            minute = time.minute
        }

        binding.noButton.setOnClickListener {
            dismiss()
        }

        binding.yesButton1.setOnClickListener {
            val picker = binding.dialogTimePickerTimePicker
            setFragmentResult(
                REQUEST_KEY_TIME,
                bundleOf(
                    ARG_TIME_NEW_HOUR to picker.hour,
                    ARG_TIME_NEW_MINUTE to picker.minute,
                    ARG_TIME_TEXTVIEW to textViewId
                )
            )
            dismiss()
        }

        return binding.root
    }

    companion object {
        const val ARG_TIME_TEXTVIEW: String = "arg_time_textview"
        private const val ARG_TIME_CURRENT_TEXT = "arg_time_current_text"

        const val REQUEST_KEY_TIME: String = "requestTimePicker"
        const val ARG_TIME_NEW_HOUR: String = "arg_time_hour"
        const val ARG_TIME_NEW_MINUTE: String = "arg_time_minute"
        const val ARG_TIME_PICKER_HEADER_TEXT = "arg_time_picker_header_text"

        @JvmStatic
        fun newInstance(
            @IdRes textViewId: Int,
            currentText: String,
            headerText: String? = null
        ): ConfirmationDialogTimePicker =
            ConfirmationDialogTimePicker().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TIME_TEXTVIEW, textViewId)
                    putString(ARG_TIME_CURRENT_TEXT, currentText)
                    putString(ARG_TIME_PICKER_HEADER_TEXT, headerText)
                }
            }
    }
}