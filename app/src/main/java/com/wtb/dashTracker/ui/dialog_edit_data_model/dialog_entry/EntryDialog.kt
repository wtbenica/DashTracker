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
import android.widget.ImageButton
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.os.bundleOf
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.dtfFullDate
import com.wtb.dashTracker.extensions.dtfTime
import com.wtb.dashTracker.extensions.getStringOrElse
import com.wtb.dashTracker.extensions.toCurrencyString
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.fragment_list_item_base.fragment_dailies.EntryListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class EntryDialog : BaseEntryDialog() {

    override val titleText: String
        get() = getString(R.string.dialog_title_dash_entry)

    override fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            item?.let { entry ->
                binding.apply {
                    fragEntryDate.text = entry.date.format(dtfFullDate)
                    fragEntryDate.tag = entry.date

                    entry.startTime.let { st ->
                        fragEntryStartTime.text = st?.format(dtfTime) ?: ""
                        fragEntryStartTime.tag = st
                    }
                    entry.endTime.let { et ->
                        fragEntryEndTime.text = et?.format(dtfTime) ?: ""
                        fragEntryEndTime.tag = et
                    }

                    fragEntryCheckEndsNextDay.isChecked =
                        entry.endDate.minusDays(1L).equals(entry.date)

                    updateTotalHours()

                    entry.startOdometer.let { so ->
                        fragEntryStartMileage.setText(
                            getStringOrElse(R.string.odometer_fmt, "", so)
                        )
                    }

                    entry.endOdometer.let { eo ->
                        fragEntryEndMileage.setText(
                            getStringOrElse(R.string.odometer_fmt, "", eo)
                        )
                    }

                    entry.mileage.let { m ->
                        fragEntryTotalMileage.text =
                            getStringOrElse(R.string.odometer_fmt, "", m)
                    }

                    setUpdateMileageButtonVisibility()

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

                    updateTotalPay()
                }
            } ?: {
                clearFields()
            }
        }
    }

    override fun clearFields() {
        binding.apply {
            fragEntryDate.text = LocalDate.now().format(dtfFullDate)
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
        val isTodaysDate = binding.fragEntryDate.tag == LocalDate.now()
        val startUnchanged: Boolean = with (binding.fragEntryStartTime.tag) {
            this != null && ((this as LocalTime) == item?.startTime)
        }
        return isTodaysDate &&
                startUnchanged &&
                binding.fragEntryEndTime.tag == null &&
                binding.fragEntryStartMileage.text.isNullOrBlank() &&
                binding.fragEntryEndMileage.text.isNullOrBlank() &&
                binding.fragEntryPay.text.isNullOrBlank() &&
                binding.fragEntryPayOther.text.isNullOrBlank() &&
                binding.fragEntryCashTips.text.isNullOrBlank() &&
                binding.fragEntryNumDeliveries.text.isNullOrBlank()
    }

    /**
     * Delete item - defers deleting item to parent [MainActivity] or [EntryListFragment] in case
     * it is the active entry, in which case tracking can be stopped before entry is deleted
     *
     */
    override fun deleteItem() {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_DATA_MODEL_DIALOG, bundleOf(
                ARG_MODIFICATION_STATE to ModificationState.DELETED.name,
                ARG_MODIFIED_ID to item?.id
            )
        )
    }

    /**
     * Set on delete pressed - always confirms before deleting, whether dialog is 'empty' or not
     */
    override fun ImageButton.setOnDeletePressed() {
        setOnClickListener {
            ConfirmDeleteDialog.newInstance(fullEntry?.entry?.entryId)
                .show(childFragmentManager, "delete_entry")
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