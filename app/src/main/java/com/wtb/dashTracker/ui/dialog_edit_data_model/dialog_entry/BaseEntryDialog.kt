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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.DialogFragEntryBinding
import com.wtb.dashTracker.databinding.DialogListItemButtonsBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.REQUEST_KEY_DATE
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogTimePicker
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogTimePicker.Companion.REQUEST_KEY_TIME
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogUseTrackedMiles
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogUseTrackedMiles.Companion.REQ_KEY_DIALOG_USE_TRACKED_MILES
import com.wtb.dashTracker.ui.dialog_confirm.SimpleConfirmationDialog.Companion.ARG_IS_CONFIRMED
import com.wtb.dashTracker.ui.dialog_confirm.SimpleConfirmationDialog.Companion.ARG_IS_CONFIRMED_2
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalCoroutinesApi
abstract class BaseEntryDialog : EditDataModelDialog<DashEntry, DialogFragEntryBinding>() {

    abstract val titleText: String
    protected open fun onFirstRun() {
        updateTotalMileageFields()
    }

    override var item: DashEntry? = null
    protected var fullEntry: FullEntry? = null

    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragEntryBinding
    override val buttonBinding: DialogListItemButtonsBinding?
        get() = binding.bottomButtonBar

    override val itemType: String
        get() = "Entry"

    private var startTimeChanged: Boolean = false

    private val distance: Int
        get() = fullEntry?.trackedDistance?.roundToInt() ?: 0

    private val currStartOdo: Int?
        get() = binding.fragEntryStartMileage.text.toIntOrNull()

    private val currEndOdo: Int?
        get() = binding.fragEntryEndMileage.text.toIntOrNull()

    private val totalMileageMatchesTrackedMileage: Boolean
        get() {
            val end = currEndOdo
            val start = currStartOdo
            val tracked = fullEntry?.trackedDistance?.roundToInt()

            return (tracked == null)
                    || ((end != null)
                    && (start != null)
                    && (tracked == (end - start)))
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullDash.collectLatest {
                    val firstRun = fullEntry == null
                    fullEntry = it
                    if (firstRun) {
                        onFirstRun()
                    }
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
                        currentText = this.tag as LocalDate? ?: LocalDate.now(),
                        headerText = getString(R.string.lbl_date)
                    ).show(childFragmentManager, "entry_date_picker")
                }
            }

            fragEntryStartTime.apply {
                setOnClickListener {
                    ConfirmationDialogTimePicker.newInstance(
                        textViewId = R.id.frag_entry_start_time,
                        currentText = this.tag as LocalTime?,
                        headerText = getString(R.string.lbl_start_time)
                    ).show(childFragmentManager, "time_picker_start")
                    startTimeChanged = true
                }
            }

            fragEntryEndTime.apply {
                setOnClickListener {
                    ConfirmationDialogTimePicker.newInstance(
                        textViewId = R.id.frag_entry_end_time,
                        currentText = this.tag as LocalTime?,
                        headerText = getString(R.string.lbl_end_time)
                    ).show(childFragmentManager, "time_picker_end")
                }
            }

            fragEntryCheckEndsNextDay.setOnCheckedChangeListener { buttonView, isChecked ->
                binding.updateTotalHours()
            }

            fragEntryStartMileage.apply {
                doOnTextChanged { _, _, _, _ ->
                    val endMileage = fragEntryEndMileage.text?.toFloatOrNull() ?: 0f
                    this.onTextChangeUpdateTotal(
                        updateView = fragEntryTotalMileage,
                        otherValue = endMileage,
                        stringFormat = R.string.odometer_fmt
                    ) { other, self ->
                        setUpdateMileageButtonVisibility()

                        max(other - self, 0f)
                    }
                }
            }

            fragEntryEndMileage.apply {
                doOnTextChanged { _, _, _, _ ->
                    val startMileage = fragEntryStartMileage.text?.toFloatOrNull() ?: 0f
                    this.onTextChangeUpdateTotal(
                        updateView = fragEntryTotalMileage,
                        otherValue = startMileage,
                        stringFormat = R.string.odometer_fmt
                    ) { other, self ->
                        setUpdateMileageButtonVisibility()

                        max(self - other, 0f)
                    }
                }
            }

            fragEntryCheckboxUseTrackedMileage.apply {
                setOnClickListener { _ ->
                    val alreadySet =
                        currStartOdo?.let { it + distance == currEndOdo } ?: false

                    when {
                        alreadySet -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_base_entry_dialog_matches_tracked_mileage),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            ConfirmationDialogUseTrackedMiles.newInstance(
                                currStartOdo,
                                currEndOdo,
                                distance
                            ).show(childFragmentManager, "dialog_use_tracked_mileage")
                        }
                    }
                }
            }

            fragEntryPay.doOnTextChanged { text, start, before, count ->
                updateTotalPay()
            }

            fragEntryPayOther.doOnTextChanged { text, start, before, count ->
                updateTotalPay()
            }

            fragEntryCashTips.doOnTextChanged { text, start, before, count ->
                updateTotalPay()
            }

            fragEntryToolbar.title = titleText
        }

    protected fun DialogFragEntryBinding.updateTotalPay() {
        val total =
            (fragEntryPay.text.toFloatOrNull() ?: 0f) + (fragEntryPayOther.text.toFloatOrNull()
                ?: 0f) + (fragEntryCashTips.text.toFloatOrNull() ?: 0f)
        fragEntryTotalEarned.text = getCurrencyString(total)
    }

    protected fun DialogFragEntryBinding.updateTotalHours() {
        val startDate = fragEntryDate.tag as LocalDate?
        val startTime = fragEntryStartTime.tag as LocalTime?
        val startDateTime =
            startDate?.let { sd -> startTime?.let { st -> LocalDateTime.of(sd, st) } }

        val endDate = if (fragEntryCheckEndsNextDay.isChecked) {
            startDate?.plusDays(1L)
        } else {
            startDate
        }
        val endTime = fragEntryEndTime.tag as LocalTime?
        val endDateTime =
            endDate?.let { ed -> endTime?.let { et -> LocalDateTime.of(ed, et) } }

        val elapsedTime = startDateTime?.let { sdt ->
            endDateTime?.let { edt ->
                ChronoUnit.SECONDS.between(sdt, edt)
            }
        }

        fragEntryTotalHours.text = getElapsedHours(elapsedTime)
    }

    override fun setDialogListeners() {
        super.setDialogListeners()

        childFragmentManager.apply {
            setFragmentResultListener(REQUEST_KEY_DATE, this@BaseEntryDialog) { _, bundle ->
                val year =
                    bundle.getInt(ConfirmationDialogDatePicker.ARG_DATE_PICKER_NEW_YEAR)
                val month =
                    bundle.getInt(ConfirmationDialogDatePicker.ARG_DATE_PICKER_NEW_MONTH)
                val dayOfMonth =
                    bundle.getInt(ConfirmationDialogDatePicker.ARG_DATE_PICKER_NEW_DAY)
                when (bundle.getInt(ConfirmationDialogDatePicker.ARG_DATE_TEXTVIEW)) {
                    R.id.frag_entry_date -> {
                        val date = LocalDate.of(year, month, dayOfMonth)
                        binding.fragEntryDate.text = date.format(dtfFullDate)
                        binding.fragEntryDate.tag = date
                    }
                }
            }

            setFragmentResultListener(REQUEST_KEY_TIME, this@BaseEntryDialog) { _, bundle ->
                val hour = bundle.getInt(ConfirmationDialogTimePicker.ARG_TIME_NEW_HOUR)
                val minute = bundle.getInt(ConfirmationDialogTimePicker.ARG_TIME_NEW_MINUTE)
                val dialogTime = LocalTime.of(hour, minute)

                when (bundle.getInt(ConfirmationDialogTimePicker.ARG_TIME_TEXTVIEW)) {
                    R.id.frag_entry_start_time -> {
                        binding.fragEntryStartTime.text = dialogTime.format(dtfTime)
                        binding.fragEntryStartTime.tag = dialogTime
                    }
                    R.id.frag_entry_end_time -> {
                        binding.fragEntryEndTime.text = dialogTime.format(dtfTime)
                        binding.fragEntryEndTime.tag = dialogTime
                    }
                }

                binding.updateTotalHours()
            }

            setFragmentResultListener(
                REQ_KEY_DIALOG_USE_TRACKED_MILES,
                this@BaseEntryDialog
            ) { _, bundle ->
                val keepStartMileage = bundle.getBoolean(ARG_IS_CONFIRMED)
                val keepEndMileage = bundle.getBoolean(ARG_IS_CONFIRMED_2)

                when {
                    keepStartMileage -> {
                        binding.fragEntryEndMileage.setText(
                            @Suppress("SetTextI18n")
                            ((currStartOdo ?: 0) + distance).toString()
                        )
                    }
                    keepEndMileage -> {
                        val newEnd = maxOf(currEndOdo ?: 0, distance)
                        val newStart = newEnd - distance

                        @Suppress("SetTextI18n")
                        binding.fragEntryStartMileage.setText(newStart.toString())
                        binding.fragEntryEndMileage.setText(newEnd.toString())
                    }
                }
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

    protected fun updateTotalMileageFields() {
        item?.let { entry ->
            val trackedDistance = fullEntry?.trackedDistance?.toFloat()
            val distance: Float? =
                entry.mileage ?: trackedDistance

            val startOdometer = entry.startOdometer ?: if (distance != null) 0f else null
            val endOdometer = entry.endOdometer ?: if (distance != null) trackedDistance else null

            binding.fragEntryStartMileage.setText(
                getStringOrElse(
                    R.string.odometer_fmt,
                    "",
                    startOdometer
                )
            )
            binding.fragEntryEndMileage.setText(
                getStringOrElse(
                    R.string.odometer_fmt,
                    "",
                    endOdometer
                )
            )
            binding.fragEntryTotalMileage.text =
                getStringOrElse(R.string.odometer_fmt, "", distance)

//            binding.fragEntryTotalMileageRow.revealIfTrue(distance != null)

            setUpdateMileageButtonVisibility()
        }
    }

    protected fun setUpdateMileageButtonVisibility() {
        binding.apply {
            fragEntryTotalMileageRow.revealIfTrue(fragEntryStartMileage.text.isNotBlank() && fragEntryEndMileage.text.isNotBlank())

            fragEntryCheckboxUseTrackedMileage.revealIfTrue(!totalMileageMatchesTrackedMileage)

            fragEntrySpace.setVisibleIfTrue(totalMileageMatchesTrackedMileage)
        }
    }
}