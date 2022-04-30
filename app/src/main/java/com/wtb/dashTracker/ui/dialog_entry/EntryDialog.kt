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

package com.wtb.dashTracker.ui.dialog_entry

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.databinding.DialogFragEntryBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment.Companion.REQUEST_KEY_DATE
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmResetDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmSaveDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class EntryDialog : FullWidthDialogFragment() {

    private var entry: DashEntry? = null
    private val viewModel: EntryViewModel by viewModels()

    private var startTimeChanged = false

    // if save button is pressed or is confirmed by save dialog, gets assigned true
    private var saveConfirmed = false

    // if delete button is pressed, gets assigned false
    private var saveOnExit = true

    private lateinit var binding: DialogFragEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadDataModel(arguments?.getInt(ARG_ENTRY_ID))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setDialogListeners()

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragEntryBinding.inflate(layoutInflater)

        binding.apply {
            fragEntryDate.apply {
                setOnClickListener {
                    DatePickerFragment.newInstance(
                        R.id.frag_entry_date,
                        this.text.toString(),
                        REQUEST_KEY_DATE
                    ).show(childFragmentManager, "date_picker")
                }
            }

            fragEntryStartTime.apply {
                setOnClickListener {
                    TimePickerFragment(this).show(childFragmentManager, "time_picker_start")
                    startTimeChanged = true
                }
            }

            fragEntryEndTime.apply {
                setOnClickListener {
                    TimePickerFragment(this).show(parentFragmentManager, "time_picker_end")
                }
            }

            fragEntryBtnDelete.apply {
                setOnClickListener {
                    ConfirmDeleteDialog.newInstance(null).show(parentFragmentManager, null)
                }
            }

            fragEntryBtnCancel.apply {
                setOnClickListener {
                    ConfirmResetDialog.newInstance().show(parentFragmentManager, null)
                }
            }

            fragEntryBtnSave.setOnClickListener {
                saveConfirmed = true
                dismiss()
            }
        }

        updateUI()

        return binding.root
    }

    private fun setDialogListeners() {
        setFragmentResultListener(
            ConfirmType.DELETE.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            if (result) {
                saveOnExit = false
                dismiss()
                entry?.let { e -> viewModel.delete(e) }
            }
        }

        setFragmentResultListener(
            ConfirmType.RESET.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            if (result) {
                updateUI()
            }
        }

        setFragmentResultListener(
            ConfirmType.SAVE.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            if (result) {
                saveConfirmed = true
            }
            dismiss()
        }

        childFragmentManager.setFragmentResultListener(
            REQUEST_KEY_DATE,
            this
        ) { _, bundle ->
            val year = bundle.getInt(DatePickerFragment.ARG_NEW_YEAR)
            val month = bundle.getInt(DatePickerFragment.ARG_NEW_MONTH)
            val dayOfMonth = bundle.getInt(DatePickerFragment.ARG_NEW_DAY)
            val int = bundle.getInt(DatePickerFragment.ARG_DATE_TEXTVIEW)
            when (int) {
                R.id.frag_entry_date -> {
                    binding.fragEntryDate.text =
                        LocalDate.of(year, month, dayOfMonth).format(dtfDate).toString()
                }
            }
        }
    }

    private fun saveValues() {

        val currDate = binding.fragEntryDate.text.toDateOrNull()
        val totalMileage =
            if (binding.fragEntryStartMileage.text.isEmpty() && binding.fragEntryEndMileage.text.isEmpty()) {
                binding.fragEntryTotalMileage.text.toFloatOrNull()
            } else {
                null
            }
        val e = DashEntry(
            entryId = entry?.entryId ?: AUTO_ID,
            date = currDate ?: LocalDate.now(),
            endDate = (if (binding.fragEntryCheckEndsNextDay.isChecked) currDate?.plusDays(1) else currDate)
                ?: LocalDate.now(),
            startTime = binding.fragEntryStartTime.text.toTimeOrNull(),
            endTime = binding.fragEntryEndTime.text.toTimeOrNull(),
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.item.collectLatest {
                entry = it
                updateUI()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                if (isEmpty() && !saveConfirmed) {
                    ConfirmSaveDialog.newInstance(
                        text = R.string.confirm_save_entry_incomplete
                    ).show(parentFragmentManager, null)
                } else {
                    super.onBackPressed()
                }
            }
        }

    override fun onDestroy() {
        if (saveOnExit && (!isEmpty() || saveConfirmed)) {
            saveValues()
        }

        super.onDestroy()
    }

    private fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            val tempEntry = entry
            if (tempEntry != null) {
                binding.fragEntryDate.text = tempEntry.date.format(dtfDate)
                tempEntry.startTime?.let { st ->
                    binding.fragEntryStartTime.text = st.format(dtfTime)
                }
                tempEntry.endTime?.let { et -> binding.fragEntryEndTime.text = et.format(dtfTime) }
                binding.fragEntryCheckEndsNextDay.isChecked =
                    tempEntry.endDate.minusDays(1L).equals(tempEntry.date)
                tempEntry.startOdometer?.let { so -> binding.fragEntryStartMileage.setText(so.toString()) }
                tempEntry.endOdometer?.let { eo -> binding.fragEntryEndMileage.setText(eo.toString()) }
                tempEntry.mileage?.let { m -> binding.fragEntryTotalMileage.setText(m.toString()) }
                tempEntry.pay?.let { p -> binding.fragEntryPay.setText(p.toString()) }
                tempEntry.otherPay?.let { op -> binding.fragEntryPayOther.setText(op.toString()) }
                tempEntry.cashTips?.let { ct -> binding.fragEntryCashTips.setText(ct.toString()) }
                tempEntry.numDeliveries?.let { nd -> binding.fragEntryNumDeliveries.setText(nd.toString()) }
            } else {
                clearFields()
            }
        }
    }

    private fun clearFields() {
        binding.apply {
            fragEntryDate.text = LocalDate.now().format(dtfDate)
            fragEntryStartTime.text = LocalDateTime.now().format(dtfTime)
            fragEntryEndTime.text = ""
            fragEntryStartMileage.text.clear()
            fragEntryEndMileage.text.clear()
            fragEntryTotalMileage.text.clear()
            fragEntryPay.text.clear()
            fragEntryPayOther.text.clear()
            fragEntryCashTips.text.clear()
            fragEntryNumDeliveries.text.clear()
        }
    }

    private fun isEmpty(): Boolean {
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
        private const val TAG = APP + "EntryDialog"
        private const val ARG_ENTRY_ID = "entry_id"

        fun newInstance(entryId: Int) =
            EntryDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ENTRY_ID, entryId)
                }
            }
    }
}