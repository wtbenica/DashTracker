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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
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
import com.wtb.dashTracker.ui.date_time_pickers.TimePickerFragment
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmResetDialog
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmSaveDialog
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class EntryDialog(
    private var entry: DashEntry? = null
) : FullWidthDialogFragment() {

    private val viewModel: EntryViewModel by viewModels()
    private var saveOnExit = true
    private var startTimeChanged = false
    private var saveConfirmed = false

    private lateinit var dateTextView: TextView
    private lateinit var startTimeTextView: TextView
    private lateinit var endTimeTextView: TextView
    private lateinit var endsNextDayCheckBox: CheckBox

    private lateinit var startMileageEditText: EditText
    private lateinit var endMileageEditText: EditText
    private lateinit var totalMileageEditText: EditText
    private lateinit var payEditText: EditText
    private lateinit var otherPayEditText: EditText
    private lateinit var cashTipsEditText: EditText
    private lateinit var numDeliveriesEditText: EditText
    private lateinit var deleteButton: ImageButton
    private lateinit var cancelButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entryId = entry?.entryId

        viewModel.loadDataModel(entryId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setDialogListeners()

        val view = inflater.inflate(R.layout.dialog_frag_entry, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dateTextView = view.findViewById<TextView>(R.id.frag_entry_date).apply {
            setOnClickListener {
                DatePickerFragment(this).show(childFragmentManager, "date_picker")
            }
        }

        startTimeTextView = view.findViewById<TextView>(R.id.frag_entry_start_time).apply {
            setOnClickListener {
                TimePickerFragment(this).show(childFragmentManager, "time_picker_start")
                startTimeChanged = true
            }
        }

        endTimeTextView = view.findViewById<TextView>(R.id.frag_entry_end_time).apply {
            setOnClickListener {
                TimePickerFragment(this).show(parentFragmentManager, "time_picker_end")
            }
        }

        endsNextDayCheckBox = view.findViewById(R.id.frag_entry_check_ends_next_day)

        startMileageEditText = view.findViewById(R.id.frag_entry_start_mileage)
        endMileageEditText = view.findViewById(R.id.frag_entry_end_mileage)
        totalMileageEditText = view.findViewById(R.id.frag_entry_total_mileage)
        payEditText = view.findViewById(R.id.frag_entry_pay)
        otherPayEditText = view.findViewById(R.id.frag_entry_pay_other)
        cashTipsEditText = view.findViewById(R.id.frag_entry_cash_tips)
        numDeliveriesEditText = view.findViewById(R.id.frag_entry_num_deliveries)

        deleteButton = view.findViewById<ImageButton>(R.id.frag_entry_btn_delete).apply {
            setOnClickListener {
                ConfirmDeleteDialog(null).show(parentFragmentManager, null)
            }
        }

        cancelButton = view.findViewById<ImageButton>(R.id.frag_entry_btn_cancel).apply {
            setOnClickListener {
                ConfirmResetDialog().show(parentFragmentManager, null)
            }
        }

        DialogFragEntryBinding.bind(view).fragEntryBtnSave.setOnClickListener {
            saveConfirmed = true
            dismiss()
        }

        updateUI()

        return view
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
    }

    private fun saveValues() {

        val currDate = dateTextView.text.toDateOrNull()
        val totalMileage = if (startMileageEditText.text.isEmpty() && endMileageEditText.text.isEmpty()) {
            totalMileageEditText.text.toFloatOrNull()
        } else {
            null
        }
        val e = DashEntry(
            entryId = entry?.entryId ?: AUTO_ID,
            date = currDate ?: LocalDate.now(),
            endDate = (if (endsNextDayCheckBox.isChecked) currDate?.plusDays(1) else currDate)
                ?: LocalDate.now(),
            startTime = startTimeTextView.text.toTimeOrNull(),
            endTime = endTimeTextView.text.toTimeOrNull(),
            startOdometer = startMileageEditText.text.toFloatOrNull(),
            endOdometer = endMileageEditText.text.toFloatOrNull(),
            totalMileage = totalMileage,
            pay = payEditText.text.toFloatOrNull(),
            otherPay = otherPayEditText.text.toFloatOrNull(),
            cashTips = cashTipsEditText.text.toFloatOrNull(),
            numDeliveries = numDeliveriesEditText.text.toIntOrNull(),
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
                    ConfirmSaveDialog(
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
                dateTextView.text = tempEntry.date.format(dtfDate)
                tempEntry.startTime?.let { st ->
                    startTimeTextView.text = st.format(dtfTime)
                }
                tempEntry.endTime?.let { et -> endTimeTextView.text = et.format(dtfTime) }
                endsNextDayCheckBox.isChecked =
                    tempEntry.endDate.minusDays(1L).equals(tempEntry.date)
                tempEntry.startOdometer?.let { so -> startMileageEditText.setText(so.toString()) }
                tempEntry.endOdometer?.let { eo -> endMileageEditText.setText(eo.toString()) }
                tempEntry.mileage?.let { m -> totalMileageEditText.setText(m.toString()) }
                tempEntry.pay?.let { p -> payEditText.setText(p.toString()) }
                tempEntry.otherPay?.let { op -> otherPayEditText.setText(op.toString()) }
                tempEntry.cashTips?.let { ct -> cashTipsEditText.setText(ct.toString()) }
                tempEntry.numDeliveries?.let { nd -> numDeliveriesEditText.setText(nd.toString()) }
            } else {
                clearFields()
            }
        }
    }

    private fun clearFields() {
        dateTextView.text = LocalDate.now().format(dtfDate)
        startTimeTextView.text = LocalDateTime.now().format(dtfTime)
        endTimeTextView.text = ""
        startMileageEditText.text.clear()
        endMileageEditText.text.clear()
        totalMileageEditText.text.clear()
        payEditText.text.clear()
        otherPayEditText.text.clear()
        cashTipsEditText.text.clear()
        numDeliveriesEditText.text.clear()
    }

    private fun isEmpty() =
        dateTextView.text == LocalDate.now().format(dtfDate) &&
                !startTimeChanged &&
                endTimeTextView.text.isBlank() &&
                startMileageEditText.text.isBlank() &&
                endMileageEditText.text.isBlank() &&
                payEditText.text.isBlank() &&
                otherPayEditText.text.isBlank() &&
                cashTipsEditText.text.isBlank() &&
                numDeliveriesEditText.text.isBlank()

    companion object {

        private const val TAG = APP + "EntryDialog"
    }
}