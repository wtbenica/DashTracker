package com.wtb.dashTracker.ui.date_time_pickers

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.wtb.dashTracker.ui.daily.DailyFragment.Companion.dtfTime
import com.wtb.dashTracker.ui.edit_details.toTimeOrNull
import java.time.LocalTime
import java.util.*


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
        timeTextView.setText(
            LocalTime.of(hourOfDay, minute).format(dtfTime)
        )
    }
}