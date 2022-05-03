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

package com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.databinding.DialogFragEntryBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment.Companion.REQUEST_KEY_DATE
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment.Companion.REQUEST_KEY_TIME
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalCoroutinesApi
class EntryDialog : EditDataModelDialog<DashEntry, DialogFragEntryBinding>() {

    override var item: DashEntry? = null
    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragEntryBinding

    private var startTimeChanged = false

    override fun getViewBinding(inflater: LayoutInflater): DialogFragEntryBinding =
        DialogFragEntryBinding.inflate(layoutInflater).apply {

            fragEntryDate.apply {
                setOnClickListener {
                    DatePickerFragment.newInstance(
                        R.id.frag_entry_date,
                        this.text.toString(),
                        REQUEST_KEY_DATE
                    ).show(parentFragmentManager, "entry_date_picker")
                }
            }

            fragEntryStartTime.apply {
                setOnClickListener {
                    TimePickerFragment.newInstance(
                        R.id.frag_entry_start_time,
                        this.text.toString(),
                        REQUEST_KEY_TIME
                    ).show(childFragmentManager, "time_picker_start")
                    startTimeChanged = true
                }
            }

            fragEntryEndTime.apply {
                setOnClickListener {
                    TimePickerFragment.newInstance(
                        R.id.frag_entry_end_time,
                        this.text.toString(),
                        REQUEST_KEY_TIME
                    ).show(childFragmentManager, "time_picker_start")
                }
            }

            fragEntryStartMileage.apply {
                doOnTextChanged { text, start, before, count ->
                    val endMileage = fragEntryEndMileage.text?.toFloatOrNull() ?: 0f
                    this.onTextChangeUpdateTotal(fragEntryTotalMileage, endMileage) { other, self ->
                        other - self
                    }
                }
            }

            fragEntryEndMileage.apply {
                doOnTextChanged { text, start, before, count ->
                    val startMileage = fragEntryStartMileage.text?.toFloatOrNull() ?: 0f
                    this.onTextChangeUpdateTotal(
                        fragEntryTotalMileage,
                        startMileage
                    ) { other, self ->
                        self - other
                    }
                }
            }

            fragEntryBtnDelete.apply {
                setOnDeletePressed()
            }

            fragEntryBtnCancel.apply {
                setOnResetPressed()
            }

            fragEntryBtnSave.apply {
                setOnSavePressed()
            }
        }

    override fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            val tempEntry = item
            if (tempEntry != null) {
                binding.fragEntryDate.text = tempEntry.date.format(dtfDate)
                tempEntry.startTime?.let { st ->
                    binding.fragEntryStartTime.text = st.format(dtfTime)
                }
                tempEntry.endTime?.let { et -> binding.fragEntryEndTime.text = et.format(dtfTime) }
                binding.fragEntryCheckEndsNextDay.isChecked =
                    tempEntry.endDate.minusDays(1L).equals(tempEntry.date)
                tempEntry.startOdometer?.let { so -> binding.fragEntryStartMileage.setText(so.toString()) }
                tempEntry.endOdometer?.let { eo -> binding.fragEntryEndMileage.setText(eo.toString()) }
                tempEntry.mileage?.let { m -> binding.fragEntryTotalMileage.setText(m.toString()) }
                tempEntry.pay?.let { p -> binding.fragEntryPay.setText(p.toString()) }
                tempEntry.otherPay?.let { op -> binding.fragEntryPayOther.setText(op.toString()) }
                tempEntry.cashTips?.let { ct -> binding.fragEntryCashTips.setText(ct.toString()) }
                tempEntry.numDeliveries?.let { nd -> binding.fragEntryNumDeliveries.setText(nd.toString()) }
            } else {
                clearFields()
            }
        }
    }

    override fun saveValues() {
        val currDate = binding.fragEntryDate.text.toDateOrNull()
        val totalMileage =
            if (binding.fragEntryStartMileage.text.isEmpty() && binding.fragEntryEndMileage.text.isEmpty()) {
                binding.fragEntryTotalMileage.text.toFloatOrNull()
            } else {
                null
            }
        val e = DashEntry(
            entryId = item?.entryId ?: AUTO_ID,
            date = currDate ?: LocalDate.now(),
            endDate = (if (binding.fragEntryCheckEndsNextDay.isChecked) currDate?.plusDays(1) else currDate)
                ?: LocalDate.now(),
            startTime = binding.fragEntryStartTime.text.toTimeOrNull(),
            endTime = binding.fragEntryEndTime.text.toTimeOrNull(),
            startOdometer = binding.fragEntryStartMileage.text.toFloatOrNull(),
            endOdometer = binding.fragEntryEndMileage.text.toFloatOrNull(),
            totalMileage = totalMileage,
            pay = binding.fragEntryPay.text.toFloatOrNull(),
            otherPay = binding.fragEntryPayOther.text.toFloatOrNull(),
            cashTips = binding.fragEntryCashTips.text.toFloatOrNull(),
            numDeliveries = binding.fragEntryNumDeliveries.text.toIntOrNull(),
        )

        viewModel.upsert(e)
    }

    override fun isEmpty(): Boolean {
        val isTodaysDate = binding.fragEntryDate.text == LocalDate.now().format(dtfDate)
        return isTodaysDate &&
                !startTimeChanged &&
                binding.fragEntryEndTime.text.isBlank() &&
                binding.fragEntryStartMileage.text.isBlank() &&
                binding.fragEntryEndMileage.text.isBlank() &&
                binding.fragEntryPay.text.isBlank() &&
                binding.fragEntryPayOther.text.isBlank() &&
                binding.fragEntryCashTips.text.isBlank() &&
                binding.fragEntryNumDeliveries.text.isBlank()
    }

    override fun setDialogListeners() {
        super.setDialogListeners()

        setFragmentResultListener(
            REQUEST_KEY_DATE
        ) { _, bundle ->
            val year = bundle.getInt(DatePickerFragment.ARG_NEW_YEAR)
            val month = bundle.getInt(DatePickerFragment.ARG_NEW_MONTH)
            val dayOfMonth = bundle.getInt(DatePickerFragment.ARG_NEW_DAY)
            val textViewId = bundle.getInt(DatePickerFragment.ARG_DATE_TEXTVIEW)
            when (textViewId) {
                R.id.frag_entry_date -> {
                    binding.fragEntryDate.text =
                        LocalDate.of(year, month, dayOfMonth).format(dtfDate).toString()
                }
            }
        }

        childFragmentManager.setFragmentResultListener(
            REQUEST_KEY_TIME,
            this
        ) { _, bundle ->
            val hour = bundle.getInt(TimePickerFragment.ARG_NEW_HOUR)
            val minute = bundle.getInt(TimePickerFragment.ARG_NEW_MINUTE)
            val textViewId = bundle.getInt(TimePickerFragment.ARG_TIME_TEXTVIEW)
            when (textViewId) {
                R.id.frag_entry_start_time -> {
                    binding.fragEntryStartTime.text =
                        LocalTime.of(hour, minute).format(dtfTime).toString()
                }
                R.id.frag_entry_end_time -> {
                    binding.fragEntryEndTime.text =
                        LocalTime.of(hour, minute).format(dtfTime).toString()
                }
            }
        }
    }

    override fun clearFields() {
        binding.apply {
            fragEntryDate.text = LocalDate.now().format(dtfDate)
            fragEntryStartTime.text = LocalDateTime.now().format(dtfTime)
            fragEntryEndTime.text = ""
            fragEntryStartMileage.text.clear()
            fragEntryEndMileage.text.clear()
            fragEntryTotalMileage.text.clear()
            fragEntryPay.text.clear()
            fragEntryPayOther.text.clear()
            fragEntryCashTips.text.clear()
            fragEntryNumDeliveries.text.clear()
        }
    }

    companion object {

        fun newInstance(entryId: Int) =
            EntryDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ITEM_ID, entryId)
                }
            }

    }
}

fun EditText.onTextChangeUpdateTotal(
    otherView: TextView,
    baseValue: Float?,
    funny: (Float, Float) -> Float
) {
    val adjustAmount: Float = text?.toFloatOrNull() ?: 0f
    val newTotal = funny(baseValue ?: 0f, adjustAmount)
    otherView.text = if (newTotal == 0f) {
        null
    } else {
        context.getFloatString(newTotal).dropLast(1)
    }
}