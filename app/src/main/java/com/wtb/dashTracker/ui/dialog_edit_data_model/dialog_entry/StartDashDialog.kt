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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
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
import com.wtb.dashTracker.extensions.toFloatOrNull
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalTextApi
@ExperimentalMaterial3Api
@OptIn(ExperimentalAnimationApi::class)
@ExperimentalCoroutinesApi
class StartDashDialog : EditDataModelDialog<DashEntry, DialogFragStartDashBinding>() {
    override var item: DashEntry? = null
    override val viewModel: EntryViewModel by viewModels()
    override lateinit var binding: DialogFragStartDashBinding
    override val itemType: String
        get() = "Entry"

    private var startTimeChanged = false

    override fun getViewBinding(inflater: LayoutInflater): DialogFragStartDashBinding =
        DialogFragStartDashBinding.inflate(layoutInflater).apply {

            fragStartDashDate.apply {
                setOnClickListener {
                    ConfirmationDialogDatePicker.newInstance(
                        R.id.frag_entry_date,
                        this.text.toString(),
                        getString(R.string.lbl_date)
                    ).show(parentFragmentManager, "entry_date_picker")
                }
            }

            fragStartDashStartTime.apply {
                setOnClickListener {
                    ConfirmationDialogTimePicker.newInstance(
                        textViewId = R.id.frag_entry_start_time,
                        currentText = this.text.toString(),
                        headerText = getString(R.string.lbl_start_time)
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
                }
            }
        }

    override fun updateUI(firstRun: Boolean) {
        val tempEntry = item
        if (tempEntry != null) {
            binding.fragStartDashDate.text = tempEntry.date.format(dtfDate)
            binding.fragStartDashDate.tag = tempEntry.date

            tempEntry.startTime?.let { st ->
                binding.fragStartDashStartTime.text = st.format(dtfTime)
                binding.fragStartDashStartTime.tag = st
            }

            tempEntry.startOdometer.let { so ->
                binding.fragStartDashStartMileage.setText(so?.toString() ?: "")
            }
        } else {
            clearFields()
        }
    }

    override fun saveValues(showToast: Boolean) {
        super.saveValues(showToast)

        val currDate = binding.fragStartDashDate.tag as LocalDate?
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

        setFragmentResultListener(REQUEST_KEY_DATE) { _, bundle ->
            val year = bundle.getInt(ARG_DATE_PICKER_NEW_YEAR)
            val month = bundle.getInt(ARG_DATE_PICKER_NEW_MONTH)
            val dayOfMonth = bundle.getInt(ARG_DATE_PICKER_NEW_DAY)
            when (bundle.getInt(ARG_DATE_TEXTVIEW)) {
                R.id.frag_entry_date -> {
                    binding.fragStartDashDate.text =
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
                    binding.fragStartDashStartTime.text =
                        dialogTime.format(dtfTime).toString()
                    binding.fragStartDashStartTime.tag =
                        dialogTime
                }
            }
        }
    }

    companion object {
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