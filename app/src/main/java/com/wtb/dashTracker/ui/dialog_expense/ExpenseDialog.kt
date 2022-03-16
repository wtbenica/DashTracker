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

package com.wtb.dashTracker.ui.dialog_expense

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.GasExpense
import com.wtb.dashTracker.database.models.MaintenanceExpense
import com.wtb.dashTracker.databinding.DialogFragExpenseBinding
import com.wtb.dashTracker.databinding.GridGasExpenseBinding
import com.wtb.dashTracker.databinding.GridOtherExpenseBinding
import com.wtb.dashTracker.extensions.dtfDate
import com.wtb.dashTracker.extensions.toDateOrNull
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
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

@ExperimentalCoroutinesApi
class ExpenseDialog(
    private var gasExpense: GasExpense? = null,
    private var maintenanceExpense: MaintenanceExpense? = null
) : FullWidthDialogFragment() {

    private val viewModel: ExpenseViewModel by viewModels()

    private var saveOnExit = true
    private var saveConfirmed = false

    private lateinit var binding: DialogFragExpenseBinding
    private lateinit var gasGridBinding: GridGasExpenseBinding
    private lateinit var otherExpenseBinding: GridOtherExpenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val expenseId = gasExpense?.gasId
        viewModel.loadDataModel(expenseId)
        val maintId = maintenanceExpense?.maintenanceId
        viewModel.loadMaintExpense(maintId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setDialogListeners()

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragExpenseBinding.inflate(layoutInflater)
        gasGridBinding =
            GridGasExpenseBinding.bind(binding.expenseViewFlipper.children.elementAt(0))
        otherExpenseBinding =
            GridOtherExpenseBinding.bind(binding.expenseViewFlipper.children.elementAt(1))

        binding.expenseTypeTabs.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.expenseViewFlipper.showNext()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Do nothing
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Do nothing
            }
        })

        /**
         * Gas Expense Grid
         */
        gasGridBinding.fragExpenseGasDate.apply {
            setOnClickListener {
                DatePickerFragment(this).show(childFragmentManager, "date_picker")
            }
        }

        gasGridBinding.fragExpenseGasAmount.doOnTextChanged { text, start, before, count ->
            updateSaveButtonIsEnabled(text, gasGridBinding.fragExpenseGasPrice.text)
        }

        gasGridBinding.fragExpenseGasPrice.doOnTextChanged { text, start, before, count ->
            updateSaveButtonIsEnabled(gasGridBinding.fragExpenseGasAmount.text, text)
        }

        binding.fragExpenseBtnSave.setOnClickListener {
            saveConfirmed = true
            dismiss()
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
                gasExpense?.let { e -> viewModel.delete(e) }
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
        if (binding.expenseViewFlipper.displayedChild == 0) {
            val currDate = gasGridBinding.fragExpenseGasDate.text.toDateOrNull()

            val g = GasExpense(
                gasId = gasExpense?.id ?: AUTO_ID,
                date = currDate ?: LocalDate.now(),
                amount = gasGridBinding.fragExpenseGasAmount.text.toString().toFloat(),
                pricePerGal = gasGridBinding.fragExpenseGasPrice.text.toString().toFloat(),
            )

            viewModel.upsert(g)
        } else if (binding.expenseViewFlipper.displayedChild == 1) {
            val otherExpenseBinding =
                GridOtherExpenseBinding.bind(binding.expenseViewFlipper.currentView)

            val currDate = otherExpenseBinding.fragExpenseOtherDate.text.toDateOrNull()

            val m = MaintenanceExpense(
                maintenanceId = gasExpense?.id ?: AUTO_ID,
                date = currDate ?: LocalDate.now(),
                amount = otherExpenseBinding.fragExpenseOtherAmount.text.toString().toFloat(),
                purpose = otherExpenseBinding.fragExpenseOtherPurpose.text.toString()
            )

            viewModel.upsert(m)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.item.collectLatest {
                gasExpense = it
                updateUI()
            }

            viewModel.maintItem.collectLatest {
                maintenanceExpense = it
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
            
//            val tempEntry = expense
//            if (tempEntry != null) {
//                dateTextView.text = tempEntry.date.format(dtfDate)
//                tempEntry.startTime?.let { st ->
//                    startTimeTextView.text = st.format(dtfTime)
//                }
//                tempEntry.endTime?.let { et -> endTimeTextView.text = et.format(dtfTime) }
//                endsNextDayCheckBox.isChecked =
//                    tempEntry.endDate.minusDays(1L).equals(tempEntry.date)
//                tempEntry.startOdometer?.let { so -> startMileageEditText.setText(so.toString()) }
//                tempEntry.endOdometer?.let { eo -> endMileageEditText.setText(eo.toString()) }
//                tempEntry.mileage?.let { m -> totalMileageEditText.setText(m.toString()) }
//                tempEntry.pay?.let { p -> payEditText.setText(p.toString()) }
//                tempEntry.otherPay?.let { op -> otherPayEditText.setText(op.toString()) }
//                tempEntry.cashTips?.let { ct -> cashTipsEditText.setText(ct.toString()) }
//                tempEntry.numDeliveries?.let { nd -> numDeliveriesEditText.setText(nd.toString()) }
//            } else {
                clearFields()
//            }
        }
    }

    private fun clearFields() {
        gasGridBinding.fragExpenseGasDate.text = LocalDate.now().format(dtfDate)
        otherExpenseBinding.fragExpenseOtherDate.text = LocalDate.now().format(dtfDate)

    //        startTimeTextView.text = LocalDateTime.now().format(dtfTime)
//        endTimeTextView.text = ""
//        startMileageEditText.text.clear()
//        endMileageEditText.text.clear()
//        totalMileageEditText.text.clear()
//        payEditText.text.clear()
//        otherPayEditText.text.clear()
//        cashTipsEditText.text.clear()
//        numDeliveriesEditText.text.clear()
    }

    private fun isEmpty() = true
//        dateTextView.text == LocalDate.now().format(dtfDate) &&
//                !startTimeChanged &&
//                endTimeTextView.text.isBlank() &&
//                startMileageEditText.text.isBlank() &&
//                endMileageEditText.text.isBlank() &&
//                payEditText.text.isBlank() &&
//                otherPayEditText.text.isBlank() &&
//                cashTipsEditText.text.isBlank() &&
//                numDeliveriesEditText.text.isBlank()

    private fun updateSaveButtonIsEnabled(amountText: CharSequence?, priceText: CharSequence?) {
        if (amountText == null || amountText.isEmpty() || priceText == null || priceText.isEmpty()) {
            binding.fragExpenseBtnSave.alpha = 0.7f
            binding.fragExpenseBtnSave.isClickable = false
        } else {
            binding.fragExpenseBtnSave.alpha = 1.0f
            binding.fragExpenseBtnSave.isClickable = true
        }
    }

    companion object {
        private const val TAG = APP + "EntryDialog"
    }
}