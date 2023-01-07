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
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment.Companion.REQUEST_KEY_TIME
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
    var fullEntry: FullEntry? = null
    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragEntryBinding

    private var startTimeChanged = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullDash.collectLatest {
                    fullEntry = it
                    updateUI()
                }
            }
        }
    }

    override fun getViewBinding(inflater: LayoutInflater): DialogFragEntryBinding =
        DialogFragEntryBinding.inflate(layoutInflater).apply {
            fragEntryDate.apply {
                setOnClickListener {
                    DatePickerFragment.newInstance(
                        R.id.frag_entry_date,
                        this.text.toString(),
                        DatePickerFragment.REQUEST_KEY_DATE
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
                    TimePickerFragment.newInstance(
                        R.id.frag_entry_end_time,
                        this.text.toString(),
                        REQUEST_KEY_TIME
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

    override fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            val tempEntry = item
            if (tempEntry != null) {
                binding.apply {
                    fragEntryDate.text = tempEntry.date.format(dtfDate)

                    tempEntry.startTime?.let { st: LocalTime ->
                        fragEntryStartTime.text = st.format(dtfTime)
                        fragEntryStartTime.tag = st
                    }

                    tempEntry.endTime?.let { et: LocalTime ->
                        fragEntryEndTime.text = et.format(dtfTime)
                        fragEntryEndTime.tag = et
                    }

                    fragEntryCheckEndsNextDay.isChecked =
                        tempEntry.endDate.minusDays(1L).equals(tempEntry.date)

                    tempEntry.startOdometer?.let { so ->
                        fragEntryStartMileage.setText(
                            getString(R.string.odometer_fmt, so)
                        )
                    }

                    fragEntryEndMileage.setText(
                        getString(
                            R.string.odometer_fmt,
                            tempEntry.endOdometer ?: ((tempEntry.startOdometer ?: 0f) +
                                    (tempEntry.mileage ?: fullEntry?.distance?.toFloat()
                                    ?: 0f))
                        )
                    )

                    fragEntryTotalMileage.text = getString(
                        R.string.odometer_fmt,
                        if (tempEntry.mileage != null)
                            tempEntry.mileage
                        else
                            fullEntry?.distance?.toFloat()
                    )

                    tempEntry.pay?.let { p -> fragEntryPay.setText(p.toCurrencyString()) }
                    tempEntry.otherPay?.let { op -> fragEntryPayOther.setText(op.toCurrencyString()) }
                    tempEntry.cashTips?.let { ct -> fragEntryCashTips.setText(ct.toCurrencyString()) }
                    tempEntry.numDeliveries?.let { nd -> fragEntryNumDeliveries.setText(nd.toString()) }
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
        val endTime = binding.fragEntryEndTime.tag as LocalTime?

        val e = DashEntry(
            entryId = item?.entryId ?: AUTO_ID,
            date = currDate ?: LocalDate.now(),
            endDate = (if (binding.fragEntryCheckEndsNextDay.isChecked) currDate?.plusDays(1) else currDate)
                ?: LocalDate.now(),
            startTime = binding.fragEntryStartTime.tag as LocalTime?,
            endTime = endTime,
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
                    fullEntry?.distance?.let { dist ->
                        getString(R.string.odometer_fmt, so + dist)
                    }
                } ?: ""
            )
            fragEntryTotalMileage.text = (fullEntry?.distance ?: 0).toString()
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

        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _, bundle ->
            val year = bundle.getInt(DatePickerFragment.ARG_NEW_YEAR)
            val month = bundle.getInt(DatePickerFragment.ARG_NEW_MONTH)
            val dayOfMonth = bundle.getInt(DatePickerFragment.ARG_NEW_DAY)
            when (bundle.getInt(DatePickerFragment.ARG_DATE_TEXTVIEW)) {
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
            val dialogTime = LocalTime.of(hour, minute)

            when (bundle.getInt(TimePickerFragment.ARG_TIME_TEXTVIEW)) {
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