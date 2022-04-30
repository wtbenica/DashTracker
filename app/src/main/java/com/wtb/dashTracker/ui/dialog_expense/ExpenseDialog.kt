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

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.asLiveData
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.database.models.Purpose.GAS
import com.wtb.dashTracker.databinding.DialogFragExpenseBinding
import com.wtb.dashTracker.databinding.DialogFragExpensePurposeDropdownFooterBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment.Companion.REQUEST_KEY_DATE
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmResetDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogEditPurposes
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose.Companion.ARG_PURPOSE_ID
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose.Companion.ARG_PURPOSE_NAME
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose.Companion.RK_ADD_PURPOSE
import com.wtb.dashTracker.ui.dialog_list_item.DataModelListItemDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalCoroutinesApi
class ExpenseDialog : DataModelListItemDialog<Expense, DialogFragExpenseBinding>() {

    override var item: Expense? = null
    override val viewModel: ExpenseViewModel by viewModels()
    override lateinit var binding: DialogFragExpenseBinding


    override fun getViewBinding(inflater: LayoutInflater): DialogFragExpenseBinding =
        DialogFragExpenseBinding.inflate(inflater).apply {
            fragExpenseDate.apply {
                setOnClickListener {
                    DatePickerFragment.newInstance(
                        R.id.frag_expense_date,
                        this.text.toString(),
                        REQUEST_KEY_DATE
                    ).show(parentFragmentManager, null)
                }
                doOnTextChanged { text, _, _, _ ->
                    updateExpense(date = text?.toDateOrNull() ?: LocalDate.now())
                }
            }

            fragExpenseAmount.apply {
                doOnTextChanged { _, _, _, _ ->
                    updateSaveButtonIsEnabled()
                }
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        updateExpense(amount = this.text.toFloatOrNull())
                    }
                }
            }

            fragExpensePrice.apply {
                doOnTextChanged { _, _, _, _ ->
                    updateSaveButtonIsEnabled()
                }
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        updateExpense(pricePerGal = this.text.toFloatOrNull())
                    }
                }
            }

            fragExpensePurpose.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val purpose = parent?.getItemAtPosition(position) as ExpensePurpose?
                        purpose?.purposeId?.let { updateExpense(purpose = it) }
                        if (purpose?.purposeId == GAS.id) {
                            fragExpensePrice.visibility = VISIBLE
                            fragExpensePriceLbl.visibility = VISIBLE
                        } else {
                            fragExpensePrice.text.clear()
                            fragExpensePrice.visibility = GONE
                            fragExpensePriceLbl.visibility = GONE
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Do nothing
                    }

                }

            fragExpenseBtnSave.setOnClickListener {
                saveConfirmed = true
                dismiss()
            }

            fragExpenseBtnDelete.setOnClickListener {
                ConfirmDeleteDialog.newInstance(null).show(parentFragmentManager, null)
            }

            fragExpenseBtnReset.setOnClickListener {
                ConfirmResetDialog.newInstance().show(parentFragmentManager, null)
            }
        }

    override fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            val tempExpense = item
            if (tempExpense != null) {
                binding.fragExpenseDate.text = tempExpense.date.format(dtfDate)
                binding.fragExpenseAmount.setText(
                    getStringOrElse(R.string.float_fmt, "", tempExpense.amount)
                )
                binding.fragExpensePrice.setText(
                    getStringOrElse(R.string.float_fmt, "", tempExpense.pricePerGal)
                )
                Log.d(TAG, "updateUI: Purpose: ${tempExpense.purpose}")
                binding.fragExpensePurpose.apply {
                    (adapter as PurposeAdapter?)?.getPositionById(tempExpense.purpose)?.let { pos ->
                        Log.d(
                            TAG,
                            "updateUI: Setting spinner: purpose: ${tempExpense.purpose} $pos"
                        )
                        if (pos != -1) {
                            setSelection(pos)
                        }
                    }
                }
            } else {
                clearFields()
            }

            updateSaveButtonIsEnabled()
        }
    }

    override fun saveValues() {
        updateExpense(
            date = binding.fragExpenseDate.text.toDateOrNull() ?: LocalDate.now(),
            amount = binding.fragExpenseAmount.text.toFloatOrNull(),
            purpose = (binding.fragExpensePurpose.selectedItem as ExpensePurpose).purposeId,
            pricePerGal = if (item?.purpose == GAS.id)
                binding.fragExpensePrice.text.toString().toFloatOrNull()
            else null
        )
    }

    override fun isEmpty(): Boolean {
        val isTodaysDate = binding.fragExpenseDate.text == LocalDate.now().format(dtfDate)
        val amountIsBlank = binding.fragExpenseAmount.text.isNullOrBlank()
        val isGasExpense =
            binding.fragExpensePurpose.let { it.adapter.getItem(it.selectedItemPosition) } == GAS.id
        val priceIsBlank = binding.fragExpensePrice.text.isNullOrBlank()
        return isTodaysDate && amountIsBlank && isGasExpense && priceIsBlank
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
    }

    override fun setDialogListeners() {
        super.setDialogListeners()

        setFragmentResultListener(
            RK_ADD_PURPOSE
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            bundle.getInt(ARG_PURPOSE_ID).let { id ->
                if (result) {
                    bundle.getString(ARG_PURPOSE_NAME)?.let { purposeName ->
                        viewModel.upsert(ExpensePurpose(purposeId = id, name = purposeName), false)
                    }
                } else {
                    updateExpense(purpose = id)
                }
                binding.fragExpensePurpose.hideDropdown()
            }
        }

        childFragmentManager.setFragmentResultListener(
            REQUEST_KEY_DATE,
            this,
        ) { _, bundle ->
            val year = bundle.getInt(DatePickerFragment.ARG_NEW_YEAR)
            val month = bundle.getInt(DatePickerFragment.ARG_NEW_MONTH)
            val dayOfMonth = bundle.getInt(DatePickerFragment.ARG_NEW_DAY)
            val int = bundle.getInt(DatePickerFragment.ARG_DATE_TEXTVIEW)
            when (int) {
                R.id.frag_expense_date -> {
                    binding.fragExpenseDate.text =
                        LocalDate.of(year, month, dayOfMonth).format(dtfDate).toString()
                }
            }
        }
    }

    override fun clearFields() {
        Log.d(TAG, "clearFields")
        binding.fragExpenseDate.text = LocalDate.now().format(dtfDate)
        binding.fragExpensePurpose.apply {
            (adapter as PurposeAdapter?)?.getPositionById(GAS.id)
                ?.let { if (it != -1) setSelection(it) }
        }
        binding.fragExpenseAmount.text.clear()
        binding.fragExpensePrice.text.clear()
    }

    private fun saveExpense() = item?.let { viewModel.upsert(it) }

    private fun updateExpense(
        date: LocalDate? = null,
        amount: Float? = null,
        purpose: Int? = null,
        pricePerGal: Float? = null
    ) {
        item?.let { e ->
            date?.let { e.date = it }
            amount?.let { e.amount = it }
            purpose?.let { e.purpose = it }
            pricePerGal?.let { e.pricePerGal = it }
            saveExpense()
        }
    }

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

    companion object {
        private const val TAG = APP + "ExpenseDialog"

        fun newInstance(expenseId: Int): ExpenseDialog =
            ExpenseDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ITEM_ID, expenseId)
                }
            }
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

            getItem(position)?.name?.let { viewHolder?.line?.text = it }

            return tempConvertView
        }

        override fun getDropDownView(
            position: Int, convertView: View?, parent: ViewGroup
        ): View {
            var tempConvertView: View?

            if (position == count - 1) {
                val binding = DialogFragExpensePurposeDropdownFooterBinding.inflate(layoutInflater)
                tempConvertView = binding.root
                binding.addPurposeBtn.setOnClickListener {
                    CoroutineScope(Dispatchers.Default).launch {
                        val purposeId = viewModel.upsertAsync(ExpensePurpose())
                        val prevPurpose = item?.purpose
                        updateExpense(purpose = purposeId.toInt())
                        ConfirmationDialogAddOrModifyPurpose.newInstance(
                            purposeId = purposeId.toInt(),
                            prevPurpose = prevPurpose,
                        ).show(parentFragmentManager, null)
                    }

                }
                binding.editPurposeBtn.setOnClickListener {
                    ConfirmationDialogEditPurposes().show(parentFragmentManager, null)
                }
            } else {
                tempConvertView = convertView
                if (tempConvertView == null || tempConvertView.tag == null) {
                    val t: View =
                        layoutInflater.inflate(
                            R.layout.dialog_frag_expense_purpose_dropdown_item,
                            parent,
                            false
                        )
                    tempConvertView = t
                    viewHolder = PurposeSpinnerViewHolder(t.findViewById(R.id.text1))
                    tempConvertView.tag = viewHolder
                } else {
                    viewHolder = tempConvertView.tag as PurposeSpinnerViewHolder?
                }

                viewHolder?.line?.text = getItem(position)?.name ?: ""
            }

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

        override fun getCount(): Int = super.getCount() + 1

        override fun getItem(position: Int): ExpensePurpose? =
            if (position == count - 1) {
                null
            } else {
                super.getItem(position)
            }
    }

    data class PurposeSpinnerViewHolder(
        val line: TextView,
    )
}