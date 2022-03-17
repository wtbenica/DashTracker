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
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.databinding.DialogFragExpenseBinding
import com.wtb.dashTracker.databinding.GridGasExpenseBinding
import com.wtb.dashTracker.databinding.GridOtherExpenseBinding
import com.wtb.dashTracker.extensions.dtfDate
import com.wtb.dashTracker.extensions.toDateOrNull
import com.wtb.dashTracker.extensions.toFloatOrNull
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmSaveDialog
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@ExperimentalCoroutinesApi
class ExpenseDialog(
    private var expense: Expense? = null,
) : FullWidthDialogFragment() {

    private val viewModel: ExpenseViewModel by viewModels()

    private var saveOnExit = true
    private var saveConfirmed = false

    private lateinit var binding: DialogFragExpenseBinding
    private lateinit var gasGridBinding: GridGasExpenseBinding
    private lateinit var otherExpenseBinding: GridOtherExpenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val expenseId = expense?.expenseId ?: 1
        viewModel.loadDataModel(expenseId)
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
                expense?.let { e -> viewModel.delete(e) }
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
        val date: LocalDate?
        val name: String
        val amount: Float?
        val pricePerGal: Float?

        if (binding.expenseViewFlipper.displayedChild == 0) {
            date = gasGridBinding.fragExpenseGasDate.text.toDateOrNull()
            name = "Gas"
            amount = gasGridBinding.fragExpenseGasAmount.text.toFloatOrNull()
            pricePerGal = gasGridBinding.fragExpenseGasPrice.text.toString().toFloat()
        } else {
            date = otherExpenseBinding.fragExpenseOtherDate.text.toDateOrNull()
            name = otherExpenseBinding.fragExpenseOtherPurpose.text.toString()
            amount = otherExpenseBinding.fragExpenseOtherAmount.text.toFloatOrNull()
            pricePerGal = null
        }

        CoroutineScope(Dispatchers.Main).launch {
            var purposeId: Int? = viewModel.getPurposeIdByName(name)

            if (purposeId == null) {
                val purpose =
                    ExpensePurpose(purposeId = AUTO_ID, name = name)
                purposeId = withContext(Dispatchers.Main) {
                    viewModel.upsertAsync(purpose).toInt()
                }
            }

            val newExpense = Expense(
                expenseId = expense?.expenseId ?: AUTO_ID,
                date = date ?: LocalDate.now(),
                amount = amount ?: 0F,
                purpose = purposeId,
                pricePerGal = pricePerGal
            )

            viewModel.upsert(newExpense)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.item.collectLatest {
                expense = it
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
            val tempExpense = expense
            if (tempExpense != null) {
                if (tempExpense.purpose == 1) {
                    binding.expenseViewFlipper.displayedChild = 0
                    gasGridBinding.fragExpenseGasDate.text = tempExpense.date.format(dtfDate)
                    gasGridBinding.fragExpenseGasAmount.setText(
                        getString(R.string.float_fmt, tempExpense.amount)
                    )
                    gasGridBinding.fragExpenseGasPrice.setText(
                        getString(R.string.float_fmt, tempExpense.pricePerGal)
                    )
                } else {
                    binding.expenseViewFlipper.displayedChild = 1
                    otherExpenseBinding.fragExpenseOtherDate.text = tempExpense.date.format(dtfDate)
                    otherExpenseBinding.fragExpenseOtherAmount.setText(
                        getString(R.string.float_fmt, tempExpense.amount)
                    )
                    otherExpenseBinding.fragExpenseOtherPurpose.setText(tempExpense.purpose)
                }
            } else {
                clearFields()
            }
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
        private const val TAG = APP + "ExpenseDialog"
    }
}