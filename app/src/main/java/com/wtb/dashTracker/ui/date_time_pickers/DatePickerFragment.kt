package com.wtb.dashTracker.ui.date_time_pickers

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.wtb.dashTracker.ui.entry_list.EntryListFragment.Companion.dtfDate
import com.wtb.dashTracker.ui.dialog_edit_details.toDateOrNull
import java.time.LocalDate
import java.util.*

class DatePickerFragment(private val dateTextView: TextView) : DialogFragment(),
    DatePickerDialog.OnDateSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ld = dateTextView.text.toDateOrNull()
        LocalDate.parse(dateTextView.text, dtfDate)
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(requireContext(), this, year, month, day)
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        dateTextView.setText(
            LocalDate.of(year, month + 1, dayOfMonth).format(dtfDate).toString()
        )
    }
}