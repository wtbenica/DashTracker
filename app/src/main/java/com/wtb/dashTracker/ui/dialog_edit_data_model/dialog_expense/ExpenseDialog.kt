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

package com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_expense

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.database.models.Purpose.GAS
import com.wtb.dashTracker.databinding.DialogFragExpenseBinding
import com.wtb.dashTracker.databinding.DialogFragExpensePurposeDropdownFooterBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.ARG_DATE_PICKER_NEW_DAY
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.ARG_DATE_PICKER_NEW_MONTH
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.ARG_DATE_PICKER_NEW_YEAR
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.ARG_DATE_TEXTVIEW
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogDatePicker.Companion.REQUEST_KEY_DATE
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogEditPurposes
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose.Companion.REQUEST_KEY_DIALOG_ADD_OR_MODIFY_PURPOSE
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose.Companion.RESULT_PURPOSE_ID
import com.wtb.dashTracker.ui.dialog_confirm.add_modify_purpose.ConfirmationDialogAddOrModifyPurpose.Companion.RESULT_UPDATE_PURPOSE
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class ExpenseDialog : EditDataModelDialog<Expense, DialogFragExpenseBinding>() {

    override var item: Expense? = null

    override val viewModel: ExpenseViewModel by viewModels()
    override lateinit var binding: DialogFragExpenseBinding
    override val itemType: String
        get() = "Expense"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.expensePurposes.collectLatest {
                (context as MainActivity?)?.runOnUiThread {
                    binding.fragExpensePurpose.adapter = PurposeAdapter(
                        requireContext(),
                        it.toTypedArray()
                    ).apply {
                        setDropDownViewResource(R.layout.dialog_frag_expense_purpose_spinner_item)
                    }
                    updateUI()
                }
            }
        }
    }

    override fun getViewBinding(inflater: LayoutInflater): DialogFragExpenseBinding =
        DialogFragExpenseBinding.inflate(inflater).apply {

            fragExpenseDate.apply {
                setOnClickListener {
                    ConfirmationDialogDatePicker.newInstance(
                        R.id.frag_expense_date,
                        this.tag as LocalDate? ?: LocalDate.now(),
                        getString(R.string.lbl_date)
                    ).show(childFragmentManager, "expense_date_picker")
                }
            }

            fragExpenseAmount.apply {
                doOnTextChanged { _, _, _, _ ->
                    updateSaveButtonIsEnabled()
                }
            }

            fragExpensePrice.apply {
                doOnTextChanged { _, _, _, _ ->
                    updateSaveButtonIsEnabled()
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

            fragExpenseBtnSave.apply {
                setOnSavePressed()
            }

            fragExpenseBtnDelete.apply {
                setOnDeletePressed()
            }

            fragExpenseBtnReset.apply {
                setOnResetPressed()
            }
        }

    override fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            val tempExpense = item
            if (tempExpense != null) {
                binding.fragExpenseDate.text = tempExpense.date.format(dtfDate)
                binding.fragExpenseDate.tag = tempExpense.date

                binding.fragExpenseAmount.setText(tempExpense.amount?.toCurrencyString() ?: "")
                binding.fragExpensePrice.setText(
                    getStringOrElse(R.string.gas_price_edit, "", tempExpense.pricePerGal)
                )

                binding.fragExpensePurpose.apply {
                    (adapter as PurposeAdapter?)?.getPositionById(tempExpense.purpose)?.let { pos ->
                        if (pos != -1) {
                            setSelection(pos)
                        } else {
                            val newPos = (adapter as PurposeAdapter?)?.getPositionById(GAS.id) ?: 0
                            setSelection(newPos)
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
        item?.apply {
            date = binding.fragExpenseDate.tag as LocalDate? ?: LocalDate.now()
            amount = binding.fragExpenseAmount.text.toFloatOrNull()
            purpose = (binding.fragExpensePurpose.selectedItem as ExpensePurpose).purposeId
            pricePerGal =
                if (item?.purpose == GAS.id) binding.fragExpensePrice.text.toFloatOrNull() else null
            isNew = false
            viewModel.upsert(this)
        }
    }

    override fun isEmpty(): Boolean {
        val isTodaysDate = binding.fragExpenseDate.text == LocalDate.now().format(dtfFullDate)
        val amountIsBlank = binding.fragExpenseAmount.text.isNullOrBlank()
        val isGasExpense =
            binding.fragExpensePurpose.let { it.adapter.getItem(it.selectedItemPosition) } == GAS.id
        val priceIsBlank = binding.fragExpensePrice.text.isNullOrBlank()
        return isTodaysDate && amountIsBlank && isGasExpense && priceIsBlank
    }

    override fun setDialogListeners() {
        super.setDialogListeners()

        childFragmentManager.setFragmentResultListener(
            REQUEST_KEY_DIALOG_ADD_OR_MODIFY_PURPOSE,
            this
        ) { _, bundle ->
            val updatePurpose = bundle.getBoolean(RESULT_UPDATE_PURPOSE)
            if (updatePurpose) {
                bundle.getLong(RESULT_PURPOSE_ID, -1L).let { id ->
                    if (id != -1L) {
                        item?.let {
                            it.purpose = id
                            viewModel.upsert(it)
                        }
                    }
                }
            }
            binding.fragExpensePurpose.hideDropdown()
        }

        childFragmentManager.setFragmentResultListener(
            REQUEST_KEY_DATE,
            this
        ) { _, bundle ->
            val year = bundle.getInt(ARG_DATE_PICKER_NEW_YEAR)
            val month = bundle.getInt(ARG_DATE_PICKER_NEW_MONTH)
            val dayOfMonth = bundle.getInt(ARG_DATE_PICKER_NEW_DAY)

            when (bundle.getInt(ARG_DATE_TEXTVIEW)) {
                R.id.frag_expense_date -> {
                    val selectedDate = LocalDate.of(year, month, dayOfMonth)
                    binding.fragExpenseDate.text = selectedDate.format(dtfDate)
                    binding.fragExpenseDate.tag = selectedDate
                }
            }
        }
    }

    override fun clearFields() {
        binding.fragExpenseDate.text = LocalDate.now().format(dtfFullDate)
        binding.fragExpensePurpose.apply {
            (adapter as PurposeAdapter?)?.getPositionById(GAS.id)
                ?.let { if (it != -1) setSelection(it) }
        }
        binding.fragExpenseAmount.text.clear()
        binding.fragExpensePrice.text.clear()
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
        fun newInstance(expenseId: Long): ExpenseDialog =
            ExpenseDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, expenseId)
                }
            }
    }

    @ExperimentalAnimationApi
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

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {

            var tempConvertView: View?

            if (position == count - 1) {
                val binding = DialogFragExpensePurposeDropdownFooterBinding.inflate(layoutInflater)
                tempConvertView = binding.root

                binding.addPurposeBtn.setOnClickListener {
                    CoroutineScope(Dispatchers.Default).launch {
                        val purposeId = viewModel.upsertAsync(ExpensePurpose())
                        val prevPurpose = item?.purpose

                        ConfirmationDialogAddOrModifyPurpose.newInstance(
                            purposeId = purposeId,
                            prevPurpose = prevPurpose,
                        ).show(childFragmentManager, null)
                    }
                }

                binding.editPurposeBtn.setOnClickListener {
                    ConfirmationDialogEditPurposes().show(childFragmentManager, null)
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

        /**
         * @return [indexOfFirst] [ExpensePurpose] in itemList with [ExpensePurpose.purposeId] [id]
         */
        fun getPositionById(id: Long): Int =
            itemList.indexOfFirst { purpose -> id == purpose.purposeId }

        /**
         * @return [indexOfFirst] [ExpensePurpose] in itemList with [ExpensePurpose.name]
         * [expenseName]
         */
        fun getPositionByName(expenseName: String): Int =
            itemList.indexOfFirst { purpose -> expenseName == purpose.name }

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