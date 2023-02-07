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
import android.view.View
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.DialogFragEntryBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.ARG_DATE_PICKER_NEW_DAY
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.ARG_DATE_PICKER_NEW_MONTH
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.ARG_DATE_PICKER_NEW_YEAR
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.ARG_DATE_TEXTVIEW
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.REQUEST_KEY_DATE
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogTimePicker
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogTimePicker.Companion.ARG_TIME_NEW_HOUR
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogTimePicker.Companion.ARG_TIME_NEW_MINUTE
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogTimePicker.Companion.ARG_TIME_TEXTVIEW
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogTimePicker.Companion.REQUEST_KEY_TIME
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.max

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class EndDashDialog : EditDataModelDialog<DashEntry, DialogFragEntryBinding>() {
    override var item: DashEntry? = null
    private var fullEntry: FullEntry? = null

    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragEntryBinding

    private var startTimeChanged = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullDash.collectLatest {
                    val firstRun = fullEntry == null
                    fullEntry = it
                    updateUI(firstRun)
                }
            }
        }
    }

    override fun getViewBinding(inflater: LayoutInflater): DialogFragEntryBinding =
        DialogFragEntryBinding.inflate(layoutInflater).apply {
            fragEntryDate.apply {
                setOnClickListener {
                    ConfirmationDialogDatePicker.newInstance(
                        R.id.frag_entry_date,
                        this.text.toString(),
                        getString(R.string.lbl_date)
                    ).show(parentFragmentManager, "entry_date_picker")
                }
            }

            fragEntryStartTime.apply {
                setOnClickListener {
                    ConfirmationDialogTimePicker.newInstance(
                        textViewId = R.id.frag_entry_start_time,
                        currentText = this.text.toString(),
                        headerText = getString(R.string.lbl_start_time)
                    ).show(childFragmentManager, "time_picker_start")
                    startTimeChanged = true
                }
            }

            fragEntryStartMileage.apply {
                doOnTextChanged { _, _, _, _ ->
                    val endMileage = fragEntryEndMileage.text?.toFloatOrNull() ?: 0f
                    this.onTextChangeUpdateTotal(
                        updateView = fragEntryTotalMileage,
                        otherValue = endMileage,
                        stringFormat = R.string.odometer_fmt
                    ) { other, self -> max(other - self, 0f) }
                }
            }

            fragEntryEndTime.apply {
                setOnClickListener {
                    ConfirmationDialogTimePicker.newInstance(
                        textViewId = R.id.frag_entry_end_time,
                        currentText = this.text.toString(),
                        headerText = getString(R.string.lbl_end_time)
                    ).show(childFragmentManager, "time_picker_end")
                }
            }

            fragEntryEndMileage.apply {
                doOnTextChanged { _, _, _, _ ->
                    val startMileage = fragEntryStartMileage.text?.toFloatOrNull() ?: 0f
                    this.onTextChangeUpdateTotal(
                        updateView = fragEntryTotalMileage,
                        otherValue = startMileage,
                        stringFormat = R.string.odometer_fmt
                    ) { other, self -> max(self - other, 0f) }
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

    override fun updateUI(firstRun: Boolean) {
        (context as MainActivity?)?.runOnUiThread {
            val tempEntry = item
            if (tempEntry != null) {
                binding.apply {
                    fragEntryDate.text = tempEntry.date.format(dtfDate)
                    fragEntryDate.tag = tempEntry.date

                    tempEntry.startTime.let { st: LocalTime? ->
                        fragEntryStartTime.text = st?.format(dtfTime) ?: ""
                        fragEntryStartTime.tag = st
                    }

                    tempEntry.startOdometer.let { so: Float? ->
                        fragEntryStartMileage.setText(
                            getStringOrElse(R.string.odometer_fmt, "", so)
                        )
                    }

                    val distance: Float = tempEntry.mileage ?: fullEntry?.trackedDistance?.toFloat() ?: 0f
                    val calculatedEnd: Float = (tempEntry.startOdometer ?: 0f) + distance
                    val endOdometer: Float = tempEntry.endOdometer ?: calculatedEnd

                    fragEntryEndMileage.setText(getString(R.string.odometer_fmt, endOdometer))

                    fragEntryTotalMileage.text = getString(R.string.odometer_fmt, distance)

                    tempEntry.pay.let { p ->
                        fragEntryPay.setText(p?.toCurrencyString() ?: "")
                    }

                    tempEntry.otherPay.let { op ->
                        fragEntryPayOther.setText(op?.toCurrencyString() ?: "")

                    }

                    tempEntry.cashTips.let { ct ->
                        fragEntryCashTips.setText(ct?.toCurrencyString() ?: "")
                    }

                    tempEntry.numDeliveries.let { nd ->
                        fragEntryNumDeliveries.setText(nd?.toString() ?: "")
                    }

                    if (firstRun) {
                        fragEntryCheckEndsNextDay.isChecked =
                            LocalDate.now().minusDays(1L).equals(tempEntry.date)

                        tempEntry.endTime.let { et: LocalTime? ->
                            val time = et ?: LocalTime.now()
                            fragEntryEndTime.text = time.format(dtfTime)
                            fragEntryEndTime.tag = time
                        }

                        saveValues()
                    } else {
                        fragEntryCheckEndsNextDay.isChecked =
                            tempEntry.endDate.minusDays(1L).equals(tempEntry.date)

                        tempEntry.endTime.let { et: LocalTime? ->
                            val time = et ?: LocalTime.now()
                            fragEntryEndTime.text = time.format(dtfTime)
                            fragEntryEndTime.tag = time
                        }
                    }
                }
            } else {
                clearFields()
            }
        }
    }

    override fun saveValues() {
        val currDate = binding.fragEntryDate.tag as LocalDate?

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
            startTime = binding.fragEntryStartTime.tag as LocalTime?,
            endTime = binding.fragEntryEndTime.tag as LocalTime?,
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

    override fun clearFields() {
        binding.apply {
            fragEntryDate.text = item?.date?.format(dtfDate) ?: LocalDate.now().format(dtfDate)
            fragEntryStartTime.text =
                item?.startTime?.format(dtfTime) ?: LocalDateTime.now().format(dtfTime)
            fragEntryStartTime.tag = item?.startTime ?: LocalTime.now()
            fragEntryStartMileage.setText(
                item?.startOdometer?.let {
                    getString(R.string.odometer_fmt, it)
                } ?: ""
            )
            fragEntryEndTime.text = LocalDateTime.now().format(dtfTime)
            fragEntryEndTime.tag = LocalTime.now()
            fragEntryEndMileage.setText(
                item?.startOdometer?.let { so ->
                    fullEntry?.trackedDistance?.let { dist ->
                        getString(R.string.odometer_fmt, so + dist)
                    }
                } ?: ""
            )
            fragEntryTotalMileage.text = (fullEntry?.trackedDistance ?: 0).toString()
            fragEntryPay.text.clear()
            fragEntryPayOther.text.clear()
            fragEntryCashTips.text.clear()
            fragEntryNumDeliveries.text.clear()
        }
    }

    override fun isEmpty(): Boolean = false

    override fun setDialogListeners() {
        super.setDialogListeners()

        setFragmentResultListener(REQUEST_KEY_DATE) { _, bundle ->
            val year = bundle.getInt(ARG_DATE_PICKER_NEW_YEAR)
            val month = bundle.getInt(ARG_DATE_PICKER_NEW_MONTH)
            val dayOfMonth = bundle.getInt(ARG_DATE_PICKER_NEW_DAY)
            when (bundle.getInt(ARG_DATE_TEXTVIEW)) {
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
            val hour = bundle.getInt(ARG_TIME_NEW_HOUR)
            val minute = bundle.getInt(ARG_TIME_NEW_MINUTE)
            val dialogTime = LocalTime.of(hour, minute)

            when (bundle.getInt(ARG_TIME_TEXTVIEW)) {
                R.id.frag_entry_start_time -> {
                    binding.fragEntryStartTime.text =
                        dialogTime.format(dtfTime).toString()
                    binding.fragEntryStartTime.tag = dialogTime
                }
                R.id.frag_entry_end_time -> {
                    binding.fragEntryEndTime.text =
                        dialogTime.format(dtfTime).toString()
                    binding.fragEntryEndTime.tag = dialogTime
                }
            }
        }
    }

    companion object {
        fun newInstance(entryId: Long): EndDashDialog =
            EndDashDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, entryId)
                }
            }
    }
}