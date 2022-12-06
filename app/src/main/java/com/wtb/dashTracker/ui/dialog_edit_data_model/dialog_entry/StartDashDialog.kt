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
import android.provider.ContactsContract.Directory.PACKAGE_NAME
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.BuildConfig
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.databinding.DialogFragStartDashBinding
import com.wtb.dashTracker.extensions.dtfDate
import com.wtb.dashTracker.extensions.dtfTime
import com.wtb.dashTracker.extensions.toDateOrNull
import com.wtb.dashTracker.extensions.toFloatOrNull
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalCoroutinesApi
class StartDashDialog : EditDataModelDialog<DashEntry, DialogFragStartDashBinding>() {
    override var item: DashEntry? = null
    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragStartDashBinding

    private var startTimeChanged = false

    override fun getViewBinding(inflater: LayoutInflater): DialogFragStartDashBinding =
        DialogFragStartDashBinding.inflate(layoutInflater).apply {

            fragStartDashDate.apply {
                setOnClickListener {
                    DatePickerFragment.newInstance(
                        R.id.frag_entry_date,
                        this.text.toString(),
                        DatePickerFragment.REQUEST_KEY_DATE
                    ).show(parentFragmentManager, "entry_date_picker")
                }
            }

            fragStartDashStartTime.apply {
                setOnClickListener {
                    TimePickerFragment.newInstance(
                        R.id.frag_entry_start_time,
                        this.text.toString(),
                        TimePickerFragment.REQUEST_KEY_TIME
                    ).show(childFragmentManager, "time_picker_start")
                    startTimeChanged = true
                }
            }

            fragStartDashBtnDelete.apply {
                setOnDeletePressed()
            }

            fragStartDashBtnCancel.apply {
                setOnResetPressed()
            }

            fragStartDashBtnStart.apply {
                setOnClickListener {
                    saveConfirmed = true
                    setFragmentResult(
                        requestKey = REQ_KEY_START_DASH_DIALOG,
                        result = bundleOf(
                            RESULT_START_DASH_CONFIRM_START to true,
                            ARG_ENTRY_ID to (item?.entryId ?: AUTO_ID)
                        )
                    )
                    dismiss()
//                    when {
//                        hasPermissions(activity as Context, *REQUIRED_PERMISSIONS) -> loadNewTrip()
//                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
//                            showRationaleLocation { locationPermLauncher.launch(LOCATION_PERMISSIONS) }
//                        else -> locationPermLauncher.launch(LOCATION_PERMISSIONS)
//                    }
                }
            }
        }

    override fun updateUI() {
        val tempEntry = item
        if (tempEntry != null) {
            binding.fragStartDashDate.text = tempEntry.date.format(dtfDate)
            tempEntry.startTime?.let { st ->
                binding.fragStartDashStartTime.text = st.format(dtfTime)
                binding.fragStartDashStartTime.tag = st
            }
            tempEntry.startOdometer?.let { so -> binding.fragStartDashStartMileage.setText(so.toString()) }
        } else {
            clearFields()
        }
    }

    override fun saveValues() {
        val currDate = binding.fragStartDashDate.text.toDateOrNull()
        val e = DashEntry(
            entryId = item?.entryId ?: AUTO_ID,
            date = currDate ?: LocalDate.now(),
            startTime = binding.fragStartDashStartTime.tag as LocalTime?,
            startOdometer = binding.fragStartDashStartMileage.text.toFloatOrNull(),
        )

        viewModel.upsert(e)
    }

    override fun clearFields() {
        binding.apply {
            fragStartDashDate.text = LocalDate.now().format(dtfDate)
            fragStartDashStartTime.text = LocalDateTime.now().format(dtfTime)
            fragStartDashStartTime.tag = LocalDateTime.now()
            fragStartDashStartMileage.text.clear()
        }
    }

    override fun isEmpty(): Boolean {
        val isTodaysDate = binding.fragStartDashDate.text == LocalDate.now().format(dtfDate)
        return isTodaysDate &&
                !startTimeChanged &&
                binding.fragStartDashStartMileage.text.isBlank()
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
                    binding.fragStartDashDate.text =
                        LocalDate.of(year, month, dayOfMonth).format(dtfDate).toString()
                }
            }
        }

        childFragmentManager.setFragmentResultListener(
            TimePickerFragment.REQUEST_KEY_TIME,
            this
        ) { _, bundle ->
            val hour = bundle.getInt(TimePickerFragment.ARG_NEW_HOUR)
            val minute = bundle.getInt(TimePickerFragment.ARG_NEW_MINUTE)
            val dialogTime = LocalTime.of(hour, minute)

            when (bundle.getInt(TimePickerFragment.ARG_TIME_TEXTVIEW)) {
                R.id.frag_entry_start_time -> {
                    binding.fragStartDashStartTime.text =
                        dialogTime.format(dtfTime).toString()
                    binding.fragStartDashStartTime.tag =
                        dialogTime
                }
            }
        }
    }

    companion object {
        private const val LOC_SVC_CHANNEL_ID = "location_practice_0"
        private const val LOC_SVC_CHANNEL_NAME = "dt_mileage_tracker"
        private const val LOC_SVC_CHANNEL_DESC = "Dashtracker mileage tracker is active"

        internal const val REQ_KEY_START_DASH_DIALOG = "result: start dash dialog"
        internal const val RESULT_START_DASH_CONFIRM_START = "arg: start dash dialog result"
        internal const val ARG_ENTRY_ID = "arg: start dash entry id"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "${PACKAGE_NAME}.extra.EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICAITON"
        private const val EXTRA_NOTIFICATION_CHANNEL =
            "${BuildConfig.APPLICATION_ID}.NotificationChannel"

        fun newInstance(entryId: Long): StartDashDialog =
            StartDashDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, entryId)
                }
            }
    }
}