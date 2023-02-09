/*
 * Copyright 2023 Wesley T. Benica
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
import android.widget.Toast
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
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogTimePicker
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogUseTrackedMiles
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.roundToInt

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalCoroutinesApi
abstract class BaseEntryDialog : EditDataModelDialog<DashEntry, DialogFragEntryBinding>() {

    abstract val titleText: String
    override var item: DashEntry? = null
    protected var fullEntry: FullEntry? = null

    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragEntryBinding

    protected var startTimeChanged = false

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
                    val distance: Int =
                        (fullEntry?.trackedDistance?.toFloat()
                            ?: fullEntry?.entry?.mileage)?.roundToInt() ?: 0

                    val currStartOdo = fragEntryStartMileage.text.toString()
                    val savedStartOdo =
                        getStringOrElse(R.string.odometer_fmt, "", item?.startOdometer)
                    val startOdometerChanged = currStartOdo != savedStartOdo

                    val currEndOdo = fragEntryEndMileage.text.toString()
                    val savedEndOdo = getStringOrElse(R.string.odometer_fmt, "", item?.endOdometer)
                    val endOdometerChanged = currEndOdo != savedEndOdo

                    val alreadySet = currStartOdo.toIntOrNull()
                        ?.let { it + distance == currEndOdo.toIntOrNull() } ?: false

                    when {
                        alreadySet -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_base_entry_dialog_matches_tracked_mileage),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        startOdometerChanged -> {
                            val endOdo: Int =
                                currStartOdo.toIntOrNull()
                                    ?.let { it + distance }
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

                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_base_entry_dialog_end_mileage_set_to, endOdo),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        endOdometerChanged -> {
                            val startOdo: Int =
                                currEndOdo.toFloatOrNull()
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

                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_base_entry_dialog_start_mileage_set_to, startOdo),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            ConfirmationDialogUseTrackedMiles.newInstance(
                                currStartOdo.toIntOrNull(),
                                currEndOdo.toIntOrNull(),
                                distance
                            ).show(childFragmentManager, "dialog_use_tracked_mileage")
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

            fragEntryToolbar.title = titleText
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

    override fun setDialogListeners() {
        super.setDialogListeners()

        setFragmentResultListener(ConfirmationDialogDatePicker.REQUEST_KEY_DATE) { _, bundle ->
            val year = bundle.getInt(ConfirmationDialogDatePicker.ARG_DATE_PICKER_NEW_YEAR)
            val month = bundle.getInt(ConfirmationDialogDatePicker.ARG_DATE_PICKER_NEW_MONTH)
            val dayOfMonth = bundle.getInt(ConfirmationDialogDatePicker.ARG_DATE_PICKER_NEW_DAY)
            when (bundle.getInt(ConfirmationDialogDatePicker.ARG_DATE_TEXTVIEW)) {
                R.id.frag_entry_date -> {
                    val date = LocalDate.of(year, month, dayOfMonth)
                    binding.fragEntryDate.text =
                        date.format(dtfDate).toString()
                    binding.fragEntryDate.tag = date
                }
            }
        }

        childFragmentManager.setFragmentResultListener(
            ConfirmationDialogTimePicker.REQUEST_KEY_TIME,
            this
        ) { _, bundle ->
            val hour = bundle.getInt(ConfirmationDialogTimePicker.ARG_TIME_NEW_HOUR)
            val minute = bundle.getInt(ConfirmationDialogTimePicker.ARG_TIME_NEW_MINUTE)
            val dialogTime = LocalTime.of(hour, minute)

            when (bundle.getInt(ConfirmationDialogTimePicker.ARG_TIME_TEXTVIEW)) {
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
}