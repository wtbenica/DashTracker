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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.dtfFullDate
import com.wtb.dashTracker.extensions.dtfTime
import com.wtb.dashTracker.extensions.getStringOrElse
import com.wtb.dashTracker.extensions.toCurrencyString
import com.wtb.dashTracker.ui.activity_main.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class EndDashDialog : BaseEntryDialog() {
    override val titleText: String
        get() = getString(R.string.dialog_title_end_dash)

    override fun onFirstRun() {
        super.onFirstRun()

        item?.let { entry ->
            binding.fragEntryCheckEndsNextDay.isChecked =
                LocalDate.now().minusDays(1L).equals(entry.date)

            entry.endTime.let { et: LocalTime? ->
                val time = et ?: LocalTime.now()
                binding.fragEntryEndTime.text = time.format(dtfTime)
                binding.fragEntryEndTime.tag = time
            }

            updateTotalMileageFields()

            save(showToast = false)
        }
    }

    override fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            item?.let { entry ->
                binding.apply {
                    fragEntryDate.text = entry.date.format(dtfFullDate)
                    fragEntryDate.tag = entry.date

                    fragEntryCheckEndsNextDay.isChecked =
                        entry.endDate.minusDays(1L).equals(entry.date)

                    entry.startTime.let { st: LocalTime? ->
                        fragEntryStartTime.text = st?.format(dtfTime) ?: ""
                        fragEntryStartTime.tag = st
                    }

                    entry.endTime?.let { et: LocalTime ->
                        fragEntryEndTime.text = et.format(dtfTime)
                        fragEntryEndTime.tag = et
                    }

                    entry.startOdometer.let { so: Float? ->
                        fragEntryStartMileage.setText(
                            getStringOrElse(R.string.odometer_fmt, "", so)
                        )
                    }

                    updateTotalMileageFields()

                    entry.pay.let { p ->
                        fragEntryPay.setText(p?.toCurrencyString() ?: "")
                    }

                    entry.otherPay.let { op ->
                        fragEntryPayOther.setText(op?.toCurrencyString() ?: "")
                    }

                    entry.cashTips.let { ct ->
                        fragEntryCashTips.setText(ct?.toCurrencyString() ?: "")
                    }

                    entry.numDeliveries.let { nd ->
                        fragEntryNumDeliveries.setText(nd?.toString() ?: "")
                    }
                }
            } ?: {
                clearFields()
            }
        }
    }

    override fun clearFields() {
        binding.apply {
            fragEntryDate.text = item?.date?.format(dtfFullDate) ?: LocalDate.now().format(dtfFullDate)
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

    companion object {
        fun newInstance(entryId: Long): EndDashDialog =
            EndDashDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, entryId)
                }
            }
    }
}