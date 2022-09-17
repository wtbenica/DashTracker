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
import com.wtb.dashTracker.databinding.DialogFragEndDashBinding
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

@ExperimentalCoroutinesApi
class EndDashDialog : EditDataModelDialog<DashEntry, DialogFragEndDashBinding>() {
    override var item: DashEntry? = null
    var fullEntry: FullEntry? = null
    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragEndDashBinding

    private var startTimeChanged = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullDash.collectLatest {
                    fullEntry = it
                    Log.d(
                        "AGOOO", "fullEntry? ${fullEntry != null} | endTime? " +
                                "${fullEntry?.entry?.endDateTime != null}"
                    )
                    fullEntry?.let { fe ->
                        (fe.entry.endDateTime
                            ?: ((binding.fragEndDashEndTime.tag as LocalTime?)?.let {
                                LocalDateTime.of(fe.entry.endDate, it)
                            }))?.let { edt ->
                            fe.pauses.forEach { p ->
                                Log.d("AGOOO", "pause end? ${p.end == null}")
                                if (p.end == null) {
                                    p.end = edt
                                    viewModel.upsert(p)
                                }
                            }
                        }
                    }
                    updateUI()
                }
            }
        }
    }

    override fun getViewBinding(inflater: LayoutInflater): DialogFragEndDashBinding =
        DialogFragEndDashBinding.inflate(layoutInflater).apply {
            fragEndDashDate.apply {
                setOnClickListener {
                    DatePickerFragment.newInstance(
                        R.id.frag_entry_date,
                        this.text.toString(),
                        DatePickerFragment.REQUEST_KEY_DATE
                    ).show(parentFragmentManager, "entry_date_picker")
                }
            }

            fragEndDashStartTime.apply {
                setOnClickListener {
                    TimePickerFragment.newInstance(
                        R.id.frag_entry_start_time,
                        this.text.toString(),
                        REQUEST_KEY_TIME
                    ).show(childFragmentManager, "time_picker_start")
                    startTimeChanged = true
                }
            }

            fragEndDashStartMileage.apply {
                doOnTextChanged { _, _, _, _ ->
                    val endMileage = fragEndDashEndMileage.text?.toFloatOrNull() ?: 0f
                    this.onTextChangeUpdateTotal(
                        updateView = fragEndDashTotalMileage,
                        otherValue = endMileage,
                        stringFormat = R.string.odometer_fmt
                    ) { other, self -> max(other - self, 0f) }
                }
            }

            fragEndDashEndTime.apply {
                setOnClickListener {
                    TimePickerFragment.newInstance(
                        R.id.frag_entry_end_time,
                        this.text.toString(),
                        REQUEST_KEY_TIME
                    ).show(childFragmentManager, "time_picker_end")
                }
            }

            fragEndDashEndMileage.apply {
                doOnTextChanged { _, _, _, _ ->
                    val startMileage = fragEndDashStartMileage.text?.toFloatOrNull() ?: 0f
                    this.onTextChangeUpdateTotal(
                        updateView = fragEndDashTotalMileage,
                        otherValue = startMileage,
                        stringFormat = R.string.odometer_fmt
                    ) { other, self -> max(self - other, 0f) }
                }
            }

            fragEndDashBtnDelete.apply {
                setOnDeletePressed()
            }

            fragEndDashBtnCancel.apply {
                setOnResetPressed()
            }

            fragEndDashBtnSave.apply {
                setOnSavePressed()
            }
        }

    override fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            val tempEntry = item
            if (tempEntry != null) {
                binding.apply {
                    fragEndDashDate.text = tempEntry.date.format(dtfDate)

                    tempEntry.startTime?.let { st: LocalTime ->
                        fragEndDashStartTime.text = st.format(dtfTime)
                        fragEndDashStartTime.tag = st
                    }

                    fragEndDashTrackedMileage.text = fullEntry?.distance?.let {
                        getString(R.string.mileage_fmt, it)
                    } ?: "0.0"

                    tempEntry.endTime?.let { et: LocalTime ->
                        fragEndDashEndTime.text = et.format(dtfTime)
                        fragEndDashEndTime.tag = et
                    }

                    fragEndDashCheckEndsNextDay.isChecked =
                        tempEntry.endDate.minusDays(1L).equals(tempEntry.date)

                    tempEntry.startOdometer?.let { so ->
                        fragEndDashStartMileage.setText(
                            getString(R.string.odometer_fmt, so)
                        )
                    }

                    fragEndDashEndMileage.setText(
                        getString(
                            R.string.odometer_fmt,
                            tempEntry.endOdometer ?: ((tempEntry.startOdometer ?: 0f) +
                                    (tempEntry.mileage ?: fullEntry?.totalDistance?.toFloat()
                                    ?: 0f))
                        )
                    )

                    fragEndDashTotalMileage.setText(
                        getString(
                            R.string.mileage_fmt,
                            if (tempEntry.mileage != null)
                                tempEntry.mileage
                            else
                                fullEntry?.distance?.toFloat()
                        )
                    )

                    tempEntry.pay?.let { p -> fragEndDashPay.setText(p.toString()) }
                    tempEntry.otherPay?.let { op -> fragEndDashPayOther.setText(op.toString()) }
                    tempEntry.cashTips?.let { ct -> fragEndDashCashTips.setText(ct.toString()) }
                    tempEntry.numDeliveries?.let { nd -> fragEndDashNumDeliveries.setText(nd.toString()) }
                }
            } else {
                clearFields()
            }
        }
    }

    override fun saveValues() {
        val currDate = binding.fragEndDashDate.text.toDateOrNull()
        val totalMileage =
            if (binding.fragEndDashStartMileage.text.isEmpty() && binding.fragEndDashEndMileage.text.isEmpty()) {
                binding.fragEndDashTotalMileage.text.toFloatOrNull()
            } else {
                null
            }
        val e = DashEntry(
            entryId = item?.entryId ?: AUTO_ID,
            date = currDate ?: LocalDate.now(),
            endDate = (if (binding.fragEndDashCheckEndsNextDay.isChecked) currDate?.plusDays(1) else currDate)
                ?: LocalDate.now(),
            startTime = binding.fragEndDashStartTime.tag as LocalTime?,
            endTime = binding.fragEndDashEndTime.tag as LocalTime?,
            startOdometer = binding.fragEndDashStartMileage.text.toFloatOrNull(),
            endOdometer = binding.fragEndDashEndMileage.text.toFloatOrNull(),
            totalMileage = totalMileage,
            pay = binding.fragEndDashPay.text.toFloatOrNull(),
            otherPay = binding.fragEndDashPayOther.text.toFloatOrNull(),
            cashTips = binding.fragEndDashCashTips.text.toFloatOrNull(),
            numDeliveries = binding.fragEndDashNumDeliveries.text.toIntOrNull(),
        )
        viewModel.upsert(e)
    }

    override fun clearFields() {
        binding.apply {
            fragEndDashDate.text = item?.date?.format(dtfDate) ?: LocalDate.now().format(dtfDate)
            fragEndDashStartTime.text =
                item?.startTime?.format(dtfTime) ?: LocalDateTime.now().format(dtfTime)
            fragEndDashStartTime.tag = item?.startTime ?: LocalTime.now()
            fragEndDashStartMileage.setText(
                item?.startOdometer?.let {
                    getString(R.string.odometer_fmt, it)
                } ?: ""
            )
            fragEndDashTrackedMileage.text = fullEntry?.distance?.let {
                getString(R.string.mileage_fmt, it)
            } ?: ""
            fragEndDashEndTime.text = LocalDateTime.now().format(dtfTime)
            fragEndDashEndTime.tag = LocalTime.now()
            fragEndDashEndMileage.setText(
                item?.startOdometer?.let { so ->
                    fullEntry?.distance?.let { dist ->
                        getString(R.string.odometer_fmt, so + dist)
                    }
                } ?: ""
            )
            fragEndDashTotalMileage.setText(
                fullEntry?.distance?.let {
                    getString(R.string.odometer_fmt, it)
                } ?: ""
            )
            fragEndDashPay.text.clear()
            fragEndDashPayOther.text.clear()
            fragEndDashCashTips.text.clear()
            fragEndDashNumDeliveries.text.clear()
        }
    }

    override fun isEmpty(): Boolean {
        val isTodaysDate = binding.fragEndDashDate.text == LocalDate.now().format(dtfDate)
        return isTodaysDate &&
                !startTimeChanged &&
                binding.fragEndDashEndTime.text.isBlank() &&
                binding.fragEndDashStartMileage.text.isBlank() &&
                binding.fragEndDashEndMileage.text.isBlank() &&
                binding.fragEndDashPay.text.isBlank() &&
                binding.fragEndDashPayOther.text.isBlank() &&
                binding.fragEndDashCashTips.text.isBlank() &&
                binding.fragEndDashNumDeliveries.text.isBlank()
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
                    binding.fragEndDashDate.text =
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

            when (bundle.getInt(TimePickerFragment.ARG_TIME_TEXTVIEW)) {
                R.id.frag_entry_start_time -> {
                    binding.fragEndDashStartTime.text =
                        LocalTime.of(hour, minute).format(dtfTime).toString()
                }
                R.id.frag_entry_end_time -> {
                    binding.fragEndDashEndTime.text =
                        LocalTime.of(hour, minute).format(dtfTime).toString()
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