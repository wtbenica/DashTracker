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
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.database.models.Purpose.GAS
import com.wtb.dashTracker.databinding.DialogFragExpenseBinding
import com.wtb.dashTracker.extensions.dtfDate
import com.wtb.dashTracker.extensions.toDateOrNull
import com.wtb.dashTracker.extensions.toFloatOrNull
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
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

@ExperimentalCoroutinesApi
class ExpenseDialog(
    private var expense: Expense? = null,
) : FullWidthDialogFragment() {

    private val viewModel: ExpenseViewModel by viewModels()

    private var deleteBtnPressed = false
    private var saveBtnPressed = false

    private lateinit var binding: DialogFragExpenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val expenseId = expense?.expenseId
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

        /**
         * Gas Expense Grid
         */
        binding.fragExpenseDate.apply {
            setOnClickListener {
                DatePickerFragment(this).show(childFragmentManager, "date_picker")
            }
        }

        binding.fragExpenseAmount.doOnTextChanged { text, start, before, count ->
            updateSaveButtonIsEnabled()
        }

        binding.fragExpensePrice.doOnTextChanged { text, start, before, count ->
            updateSaveButtonIsEnabled()
        }

        binding.fragExpensePurpose.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if ((binding.fragExpensePurpose.adapter.getItem(position) as ExpensePurpose).purposeId == GAS.id) {
                        binding.fragExpensePrice.visibility = VISIBLE
                        binding.fragExpensePriceLbl.visibility = VISIBLE
                    } else {
                        binding.fragExpensePrice.text.clear()
                        binding.fragExpensePrice.visibility = GONE
                        binding.fragExpensePriceLbl.visibility = GONE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }

            }

        binding.fragExpenseBtnSave.setOnClickListener {
            saveBtnPressed = true
            dismiss()
        }

        binding.fragExpenseBtnDelete.setOnClickListener {
            ConfirmDeleteDialog(null).show(parentFragmentManager, null)
        }

        binding.fragExpenseBtnReset.setOnClickListener {
            ConfirmResetDialog()
        }

        updateUI()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.expensePurposes.asLiveData().observe(viewLifecycleOwner) {
            binding.fragExpensePurpose.adapter = PurposeAdapter(
                requireContext(),
                it?.toTypedArray() ?: arrayOf()
            ).apply {
                setDropDownViewResource(R.layout.dialog_frag_expense_purpose_spinner_item)
            }
            updateUI()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.item.collectLatest {
                    expense = it
                    updateUI()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                if (isNotEmpty() && !saveBtnPressed) {
                    ConfirmSaveDialog(
                        text = R.string.confirm_save_entry_incomplete
                    ).show(parentFragmentManager, null)
                } else {
                    super.onBackPressed()
                }
            }
        }

    override fun onDestroy() {
        if (!deleteBtnPressed && (isNotEmpty() || saveBtnPressed)) {
            saveValues()
        }

        super.onDestroy()
    }

    private fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            val tempExpense = expense
            if (tempExpense != null) {
                binding.fragExpenseDate.text = tempExpense.date.format(dtfDate)
                binding.fragExpenseAmount.setText(
                    getString(R.string.float_fmt, tempExpense.amount)
                )
                binding.fragExpensePrice.setText(
                    getString(R.string.float_fmt, tempExpense.pricePerGal)
                )
                binding.fragExpensePurpose.apply {
                    (adapter as PurposeAdapter?)?.getPositionById(tempExpense.purpose)
                        ?.let {
                            if (it != -1)
                                setSelection(it)
                        }
                }
            } else {
                clearFields()
            }

            updateSaveButtonIsEnabled()
        }
    }

    private fun saveValues() {
        val date: LocalDate? = binding.fragExpenseDate.text.toDateOrNull()
        val amount: Float? = binding.fragExpenseAmount.text.toFloatOrNull()
        val purposeId = (binding.fragExpensePurpose.selectedItem as ExpensePurpose).purposeId
        val pricePerGal = if (purposeId == GAS.id)
            binding.fragExpensePrice.text.toString().toFloat()
        else null

        CoroutineScope(Dispatchers.Main).launch {
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

    private fun clearFields() {
        binding.fragExpenseDate.text = LocalDate.now().format(dtfDate)
        binding.fragExpensePurpose.apply {
            (adapter as PurposeAdapter?)?.getPositionById(GAS.id)
                ?.let { if (it != -1) setSelection(it) }
        }
        binding.fragExpenseAmount.text.clear()
        binding.fragExpensePrice.text.clear()
    }


    private fun setDialogListeners() {
        setFragmentResultListener(
            ConfirmType.DELETE.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            if (result) {
                deleteBtnPressed = true
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
                saveBtnPressed = true
            }
            dismiss()
        }
    }

    private fun isEmpty() =
        binding.fragExpenseDate.text == LocalDate.now().format(dtfDate) &&
                binding.fragExpenseAmount.text.isBlank() &&
                binding.fragExpensePurpose.let {
                    it.adapter.getItem(it.selectedItemPosition)
                } == GAS.id && binding.fragExpensePrice.text.isBlank()

    private fun updateSaveButtonIsEnabled() {
        if (binding.fragExpenseAmount.text == null || binding.fragExpenseAmount.text.isEmpty() ||
            ((binding.fragExpensePurpose.selectedItem as ExpensePurpose?)?.purposeId == GAS.id && (binding.fragExpensePrice.text == null || binding.fragExpensePrice.text.isEmpty()))
        ) {
            binding.fragExpenseBtnSave.alpha = 0.7f
            binding.fragExpenseBtnSave.isClickable = false
        } else {
            binding.fragExpenseBtnSave.alpha = 1.0f
            binding.fragExpenseBtnSave.isClickable = true
        }
    }

    private fun isNotEmpty() = !isEmpty()

    companion object {
        private const val TAG = APP + "ExpenseDialog"
    }

    inner class PurposeAdapter(
        context: Context,
        private val itemList: Array<ExpensePurpose>
    ) : ArrayAdapter<ExpensePurpose>(
        context,
        R.layout.dialog_frag_expense_purpose_spinner_item,
        itemList
    ) {
        private var viewHolder: PurposeSpinnerViewHolder? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var tempConvertView = convertView

            if (tempConvertView == null) {
                val t: View =
                    layoutInflater.inflate(
                        R.layout.dialog_frag_expense_purpose_spinner_item,
                        parent,
                        false
                    )
                tempConvertView = t
                viewHolder = PurposeSpinnerViewHolder(
                    t.findViewById(R.id.text1)
                )
                tempConvertView.tag = viewHolder
            } else {
                viewHolder = tempConvertView.tag as PurposeSpinnerViewHolder
            }

            viewHolder?.line?.text = itemList[position].name

            return tempConvertView
        }

        override fun getDropDownView(
            position: Int, convertView: View?, parent: ViewGroup
        ): View {
            var tempConvertView = convertView

            if (tempConvertView == null) {
                val t: View =
                    layoutInflater.inflate(
                        R.layout.dialog_frag_expense_purpose_dropdown_item,
                        parent,
                        false
                    )
                tempConvertView = t
                viewHolder = PurposeSpinnerViewHolder(
                    t.findViewById(R.id.text1)
                )
                tempConvertView.tag = viewHolder
            } else {
                viewHolder = tempConvertView.tag as PurposeSpinnerViewHolder
            }

            viewHolder?.line?.text = itemList[position].name

            return tempConvertView
        }

        fun getPositionById(id: Int): Int {
            var pos = -1
            itemList.forEachIndexed { index, purpose ->
                if (id == purpose.purposeId) {
                    pos = index
                }
            }
            return pos
        }
    }

    data class PurposeSpinnerViewHolder(
        val line: TextView,
    )
}