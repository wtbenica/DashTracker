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
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.wtb.dashTracker.extensions.dtfTime
import com.wtb.dashTracker.extensions.toTimeOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalTime
import java.util.*


@ExperimentalCoroutinesApi
class TimePickerFragment(private val timeTextView: TextView) : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()

        val currValue = timeTextView.text.toTimeOrNull()
        val currHour = currValue?.hour
        val currMinute = currValue?.minute

        val hour = currHour ?: c.get(Calendar.HOUR_OF_DAY)
        val minute = currMinute ?: c.get(Calendar.MINUTE)
        return TimePickerDialog(activity, this, hour, minute, DateFormat.is24HourFormat(activity))
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        timeTextView.text = LocalTime.of(hourOfDay, minute).format(dtfTime)
    }
}