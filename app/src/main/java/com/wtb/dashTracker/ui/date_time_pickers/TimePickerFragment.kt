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

package com.wtb.dashTracker.ui.date_time_pickers

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.wtb.dashTracker.extensions.dtfTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalTime
import java.time.format.DateTimeParseException


@ExperimentalCoroutinesApi
class TimePickerFragment : DialogFragment(),
    TimePickerDialog.OnTimeSetListener {

    @IdRes
    private var textViewId: Int? = null
    private var currentText: String? = null
    private var requestKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            textViewId = it.getInt(ARG_TIME_TEXTVIEW)
            currentText = it.getString(ARG_CURRENT_TEXT)
            requestKey = it.getString(ARG_REQUEST_KEY)
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val time: LocalTime = try {
            LocalTime.parse(currentText, dtfTime)
        } catch (e: DateTimeParseException) {
            LocalTime.now()
        }

        return TimePickerDialog(
            requireContext(),
            this,
            time.hour,
            time.minute,
            DateFormat.is24HourFormat(activity)
        )
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_TIME,
            bundleOf(
                ARG_NEW_HOUR to hourOfDay,
                ARG_NEW_MINUTE to minute,
                ARG_TIME_TEXTVIEW to textViewId
            )
        )
    }

    companion object {
        const val ARG_TIME_TEXTVIEW: String = "param1"
        private const val ARG_CURRENT_TEXT = "param2"
        private const val ARG_REQUEST_KEY = "param3"

        const val REQUEST_KEY_TIME: String = "requestTimePicker"
        const val ARG_NEW_HOUR: String = "hour"
        const val ARG_NEW_MINUTE: String = "minute"

        @JvmStatic
        fun newInstance(@IdRes textViewId: Int, currentText: String, requestKey: String): TimePickerFragment =
            TimePickerFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TIME_TEXTVIEW, textViewId)
                    putString(ARG_CURRENT_TEXT, currentText)
                    putString(ARG_REQUEST_KEY, requestKey)
                }
            }

    }
}