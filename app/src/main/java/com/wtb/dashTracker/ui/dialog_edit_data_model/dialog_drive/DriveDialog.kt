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

package com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_drive

import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.Drive
import com.wtb.dashTracker.databinding.DialogFragDriveBinding
import com.wtb.dashTracker.extensions.dtfTime
import com.wtb.dashTracker.extensions.toIntOrNull
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalCoroutinesApi
class DriveDialog : EditDataModelDialog<Drive, DialogFragDriveBinding>() {

    override var item: Drive? = null
    override val viewModel: DriveViewModel by viewModels()
    override lateinit var binding: DialogFragDriveBinding

    override fun getViewBinding(inflater: LayoutInflater): DialogFragDriveBinding =
        DialogFragDriveBinding.inflate(inflater).apply {
            tabber.apply {
                val trackedMileageTab = newTab()
                    .setText(R.string.lbl_tracked_mileage)
                addTab(trackedMileageTab)
                val endOdometerTab = newTab()
                    .setText(R.string.lbl_end_mileage)
                addTab(endOdometerTab)
                addOnTabSelectedListener(
                    object : OnTabSelectedListener {
                        override fun onTabSelected(tab: TabLayout.Tab?) {
                            when (tab) {
                                endOdometerTab -> {
                                    topLabel.setText(R.string.lbl_end_mileage)
                                    btmLabel.setText(R.string.lbl_total_mileage)
                                    fragDriveTopValue.setBackgroundResource(R.drawable.bg_edit_text)
                                    fragDriveBtmValue.setBackgroundResource(R.drawable.bg_edit_text_disabled)
                                }
                                trackedMileageTab -> {
                                    topLabel.setText(R.string.lbl_tracked_mileage)
                                    btmLabel.setText(R.string.lbl_end_mileage)
                                    fragDriveTopValue.setBackgroundResource(R.drawable.bg_edit_text_disabled)
                                    fragDriveBtmValue.setBackgroundResource(R.drawable.bg_edit_text)
                                }
                            }
                        }

                        override fun onTabUnselected(tab: TabLayout.Tab?) {}

                        override fun onTabReselected(tab: TabLayout.Tab?) {}
                    }
                )
            }

            fragDriveStartTime.apply {
                setOnClickListener {
                    TimePickerFragment.newInstance(
                        R.id.frag_drive_start_time,
                        this.text.toString(),
                        TimePickerFragment.REQUEST_KEY_TIME
                    ).show(childFragmentManager, "time_picker_start")
//                    startTimeChanged = true
                }
            }

            fragDriveEndTime.apply {
                setOnClickListener {
                    TimePickerFragment.newInstance(
                        R.id.frag_drive_end_time,
                        this.text.toString(),
                        TimePickerFragment.REQUEST_KEY_TIME
                    ).show(childFragmentManager, "time_picker_start")
                }
            }

            fragDriveBtnSave.apply {
                setOnSavePressed()
            }

            fragDriveBtnDelete.apply {
                setOnDeletePressed()
            }

            fragDriveBtnReset.apply {
                setOnResetPressed()
            }
        }

    override fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            val tempDrive = item
            if (tempDrive != null) {
                binding.fragDriveStartTime.apply {
                    text = tempDrive.start?.format(dtfTime)
                    tag = tempDrive.start
                }

                binding.fragDriveEndTime.apply {
                    text = tempDrive.end?.format(dtfTime)
                    tag = tempDrive.end
                }

                binding.fragDriveStartMileage.setText((tempDrive.startOdometer ?: 0).toString())


            } else {
                clearFields()
            }

            updateSaveButtonIsEnabled()
        }
    }

    override fun saveValues() {
        item?.apply {
            this.start = binding.fragDriveStartTime.tag as LocalDateTime? ?: start
            this.end = binding.fragDriveEndTime.tag as LocalDateTime? ?: end
            startOdometer = binding.fragDriveStartMileage.text.toIntOrNull() ?: startOdometer
            viewModel.upsert(this)
        }
    }

    override fun isEmpty(): Boolean {
        // TODO: this
        return false
    }

    override fun setDialogListeners() {
        super.setDialogListeners()

        childFragmentManager.setFragmentResultListener(
            TimePickerFragment.REQUEST_KEY_TIME,
            this
        ) { _, bundle ->
            val hour = bundle.getInt(TimePickerFragment.ARG_NEW_HOUR)
            val minute = bundle.getInt(TimePickerFragment.ARG_NEW_MINUTE)
            when (bundle.getInt(TimePickerFragment.ARG_TIME_TEXTVIEW)) {
                R.id.frag_drive_start_time -> {
                    val startTimeTag = binding.fragDriveStartTime.tag as LocalDateTime?
                    if (startTimeTag == null || startTimeTag.hour != hour
                        || startTimeTag.minute != minute
                    ) {
                        val newTime = LocalTime.of(hour, minute)
                        val startDate = item?.start?.toLocalDate() ?: LocalDate.now()
                        val newDateTime = LocalDateTime.of(startDate, newTime)
                        binding.fragDriveStartTime.text = newTime.format(dtfTime).toString()
                        binding.fragDriveStartTime.tag = newDateTime
                    }
                }
                R.id.frag_drive_end_time -> {
                    val endTimeTag = binding.fragDriveEndTime.tag as LocalDateTime?
                    if (endTimeTag == null || endTimeTag.hour != hour
                        || endTimeTag.minute != minute
                    ) {
                        val newTime = LocalTime.of(hour, minute)
                        val endDate = item?.end?.toLocalDate() ?: LocalDate.now()
                        val newDateTime = LocalDateTime.of(endDate, newTime)
                        binding.fragDriveEndTime.text = newTime.format(dtfTime).toString()
                        binding.fragDriveEndTime.tag = newDateTime
                    }
                }
            }
        }
    }

    override fun clearFields() {
//        binding.fragDriveDate.text = LocalDate.now().format(dtfDate)
//        binding.fragDrivePurpose.apply {
//            (adapter as PurposeAdapter?)?.getPositionById(GAS.id)
//                ?.let { if (it != -1) setSelection(it) }
//        }
//        binding.fragDriveAmount.text.clear()
//        binding.fragDrivePrice.text.clear()
    }

    private fun updateSaveButtonIsEnabled() {
//        if (binding.fragDriveAmount.text == null || binding.fragDriveAmount.text.isEmpty() ||
//            ((binding.fragDrivePurpose.selectedItem as DrivePurpose?)?.purposeId == GAS.id && (binding.fragDrivePrice.text == null || binding.fragDrivePrice.text.isEmpty()))
//        ) {
//            binding.fragDriveBtnSave.alpha = 0.7f
//            binding.fragDriveBtnSave.isClickable = false
//        } else {
//            binding.fragDriveBtnSave.alpha = 1.0f
//            binding.fragDriveBtnSave.isClickable = true
//        }
    }

    companion object {
        fun newInstance(expenseId: Long): DriveDialog =
            DriveDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, expenseId)
                }
            }
    }
}