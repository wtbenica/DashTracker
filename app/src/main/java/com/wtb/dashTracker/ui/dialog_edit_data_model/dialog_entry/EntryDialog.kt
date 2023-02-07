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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
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
import com.wtb.dashTracker.ui.activity_main.TAG
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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class EntryDialog : EditDataModelDialog<DashEntry, DialogFragEntryBinding>() {

    override var item: DashEntry? = null
    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragEntryBinding
    private var fullEntry: FullEntry? = null

    private var startTimeChanged = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullDash.collectLatest {
                    fullEntry = it
                }
            }
        }
    }

    override fun getViewBinding(inflater: LayoutInflater): DialogFragEntryBinding =
        DialogFragEntryBinding.inflate(layoutInflater).apply {
            fragEntryDate.apply {
                setOnClickListener {
                    ConfirmationDialogDatePicker.newInstance(
                        textViewId = R.id.frag_entry_date,
                        currentText = this.text.toString(),
                        headerText = getString(R.string.lbl_date)
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

            fragEntryEndTime.apply {
                setOnClickListener {
                    ConfirmationDialogTimePicker.newInstance(
                        textViewId = R.id.frag_entry_end_time,
                        currentText = this.text.toString(),
                        headerText = getString(R.string.lbl_end_time)
                    ).show(childFragmentManager, "time_picker_end")
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

            fragEntryCheckboxUseTrackedMileage.apply {
                setOnClickListener { _ ->
                    val distance: Float =
                        fullEntry?.trackedDistance?.toFloat() ?: fullEntry?.entry?.mileage ?: 0f

                    val currStart = fragEntryStartMileage.text.toString()
                    val itemStart = getStringOrElse(R.string.odometer_fmt, "", item?.startOdometer)
                    val startOdometerChanged = currStart != itemStart
                    val currentEnd = fragEntryEndMileage.text.toString()
                    val itemEnd = getStringOrElse(R.string.odometer_fmt, "", item?.endOdometer)
                    val endOdometerChanged = currentEnd != itemEnd
                    Log.d(
                        TAG,
                        "START | curr: $currStart | item: $itemStart | $startOdometerChanged | END | curr: $currentEnd | item: $itemEnd | $endOdometerChanged\n" +
                                "dist: $distance | tracked: ${
                                    getString(
                                        R.string.float_fmt,
                                        fullEntry?.trackedDistance
                                    )
                                }"
                    )
                    when {
                        startOdometerChanged && endOdometerChanged -> {
                            // show dialog
                        }
                        startOdometerChanged -> {
                            val endOdo: Int =
                                currStart.toIntOrNull()
                                    ?.let { it + distance.roundToInt() }
                                    ?: 0

                            fragEntryEndMileage.setText(endOdo.toString())

                            item?.let {
                                viewModel.updateEntry(
                                    entry = it,
                                    startOdometer = fragEntryStartMileage.text.toIntOrNull()
                                        ?.toFloat(),
                                    endOdometer = endOdo.toFloat()
                                )
                            }
                        }
                        endOdometerChanged -> {
                            val startOdo: Int =
                                currentEnd.toFloatOrNull()
                                    ?.let { (it - distance).roundToInt() }
                                    ?: 0

                            fragEntryStartMileage.setText(startOdo.toString())

                            item?.let {
                                viewModel.updateEntry(
                                    entry = it,
                                    startOdometer = startOdo.toFloat(),
                                    endOdometer = fragEntryEndMileage.text.toIntOrNull()?.toFloat()
                                )
                            }
                        }
                        else -> {
                            // show dialog
                        }
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

            fragEntryToolbar.title = getString(R.string.dialog_title_dash_entry)
        }

    override fun updateUI(firstRun: Boolean) {
        (context as MainActivity?)?.runOnUiThread {
            val tempEntry = item
            if (tempEntry != null) {
                binding.fragEntryDate.text = tempEntry.date.format(dtfDate)

                tempEntry.startTime.let { st ->
                    binding.fragEntryStartTime.text = st?.format(dtfTime) ?: ""
                    binding.fragEntryStartTime.tag = st
                }
                tempEntry.endTime.let { et ->
                    binding.fragEntryEndTime.text = et?.format(dtfTime) ?: ""
                    binding.fragEntryEndTime.tag = et
                }

                binding.fragEntryCheckEndsNextDay.isChecked =
                    tempEntry.endDate.minusDays(1L).equals(tempEntry.date)

                tempEntry.startOdometer.let { so ->
                    binding.fragEntryStartMileage.setText(
                        getStringOrElse(R.string.odometer_fmt, "", so)
                    )
                }

                tempEntry.endOdometer.let { eo ->
                    binding.fragEntryEndMileage.setText(
                        getStringOrElse(R.string.odometer_fmt, "", eo)
                    )
                }

                tempEntry.mileage.let { m ->
                    binding.fragEntryTotalMileage.text =
                        getStringOrElse(R.string.odometer_fmt, "", m)
                }

                tempEntry.pay.let { p ->
                    binding.fragEntryPay.setText(p?.toCurrencyString() ?: "")
                }
                tempEntry.otherPay.let { op ->
                    binding.fragEntryPayOther.setText(op?.toCurrencyString() ?: "")
                }
                tempEntry.cashTips.let { ct ->
                    binding.fragEntryCashTips.setText(ct?.toCurrencyString() ?: "")
                }
                tempEntry.numDeliveries.let { nd ->
                    binding.fragEntryNumDeliveries.setText(nd?.toString() ?: "")
                }
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

    override fun clearFields() {
        binding.apply {
            fragEntryDate.text = LocalDate.now().format(dtfDate)
            fragEntryStartTime.text = LocalDateTime.now().format(dtfTime)
            fragEntryEndTime.text = ""
            fragEntryStartMileage.text.clear()
            fragEntryEndMileage.text.clear()
            fragEntryTotalMileage.text = ""
            fragEntryPay.text.clear()
            fragEntryPayOther.text.clear()
            fragEntryCashTips.text.clear()
            fragEntryNumDeliveries.text.clear()
        }
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
            when (bundle.getInt(ARG_TIME_TEXTVIEW)) {
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

    companion object {
        fun newInstance(entryId: Long): EntryDialog =
            EntryDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, entryId)
                }
            }
    }
}

fun EditText.onTextChangeUpdateTotal(
    updateView: TextView,
    otherValue: Float?,
    @StringRes stringFormat: Int? = null,
    operation: (Float, Float) -> Float
) {
    val self: Float = text?.toFloatOrNull() ?: 0f
    val newTotal = operation(otherValue ?: 0f, self)
    updateView.text = if (newTotal == 0f) {
        null
    } else {
        if (stringFormat != null) {
            context.getString(stringFormat, newTotal)
        } else {
            context.getFloatString(newTotal).dropLast(1)
        }
    }
}

/**
 * @return 2.83234f -> "2.83", 2f -> "2", 2.0f -> "2", "2.1f" -> "2.10"
 */
fun Float.toCurrencyString(): String =
    if (this != floor(this)) {
        "%.2f".format(this)
    } else {
        "%.0f".format(this)
    }