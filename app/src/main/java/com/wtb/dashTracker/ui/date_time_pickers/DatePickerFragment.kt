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

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.wtb.dashTracker.extensions.dtfDate
import java.time.LocalDate

class DatePickerFragment : DialogFragment(),
    DatePickerDialog.OnDateSetListener {

    @IdRes
    private var textViewId: Int? = null
    private var currentText: String? = null
    private var requestKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            textViewId = it.getInt(ARG_DATE_TEXTVIEW)
            currentText = it.getString(ARG_CURRENT_TEXT)
            requestKey = it.getString(ARG_REQUEST_KEY)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = LocalDate.parse(currentText, dtfDate)

        return DatePickerDialog(
            requireContext(),
            this,
            date.year,
            date.month.value - 1,
            date.dayOfMonth
        )
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        setFragmentResult(
            requestKey ?: REQUEST_KEY_DATE,
            bundleOf(
                ARG_NEW_YEAR to year,
                ARG_NEW_MONTH to month + 1,
                ARG_NEW_DAY to dayOfMonth,
                ARG_DATE_TEXTVIEW to textViewId
            )
        )
    }

    companion object {
        const val ARG_DATE_TEXTVIEW = "param1"
        private const val ARG_CURRENT_TEXT = "param2"
        private const val ARG_REQUEST_KEY = "param3"

        const val REQUEST_KEY_DATE = "requestDatePicker"
        const val ARG_NEW_YEAR = "year"
        const val ARG_NEW_MONTH = "month"
        const val ARG_NEW_DAY = "dayOfMonth"

        @JvmStatic
        fun newInstance(@IdRes textViewId: Int, currentText: String, requestKey: String) =
            DatePickerFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DATE_TEXTVIEW, textViewId)
                    putString(ARG_CURRENT_TEXT, currentText)
                    putString(ARG_REQUEST_KEY, requestKey)
                }
            }
    }
}