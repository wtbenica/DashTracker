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
import com.wtb.dashTracker.extensions.dtfDate
import com.wtb.dashTracker.extensions.dtfTime
import com.wtb.dashTracker.extensions.getStringOrElse
import com.wtb.dashTracker.extensions.toCurrencyString
import com.wtb.dashTracker.ui.activity_main.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class EntryDialog : BaseEntryDialog() {

    override val titleText: String
        get() = getString(R.string.dialog_title_dash_entry)

    override fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            val tempEntry = item

            if (tempEntry != null) {
                binding.apply {
                    fragEntryDate.text = tempEntry.date.format(dtfDate)
                    fragEntryDate.tag = tempEntry.date

                    tempEntry.startTime.let { st ->
                        fragEntryStartTime.text = st?.format(dtfTime) ?: ""
                        fragEntryStartTime.tag = st
                    }
                    tempEntry.endTime.let { et ->
                        fragEntryEndTime.text = et?.format(dtfTime) ?: ""
                        fragEntryEndTime.tag = et
                    }

                    fragEntryCheckEndsNextDay.isChecked =
                        tempEntry.endDate.minusDays(1L).equals(tempEntry.date)

                    tempEntry.startOdometer.let { so ->
                        fragEntryStartMileage.setText(
                            getStringOrElse(R.string.odometer_fmt, "", so)
                        )
                    }

                    tempEntry.endOdometer.let { eo ->
                        fragEntryEndMileage.setText(
                            getStringOrElse(R.string.odometer_fmt, "", eo)
                        )
                    }

                    tempEntry.mileage.let { m ->
                        fragEntryTotalMileage.text =
                            getStringOrElse(R.string.odometer_fmt, "", m)
                    }

                    setUpdateMileageButtonVisibility()

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
                }
            } else {
                clearFields()
            }
        }
    }

    // TODO: This erases everything, whereas other dialogs it resets. decide what it should do
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

    companion object {
        fun newInstance(entryId: Long): EntryDialog =
            EntryDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, entryId)
                }
            }
    }
}