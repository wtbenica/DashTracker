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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.database.models.Purpose.GAS
import com.wtb.dashTracker.databinding.DialogFragExpenseBinding
import com.wtb.dashTracker.databinding.DialogFragExpensePurposeDropdownHeaderBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.date_time_pickers.DatePickerFragment
import com.wtb.dashTracker.ui.dialog_confirm.*
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogAddPurpose.Companion.ARG_PREV_PURPOSE
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogAddPurpose.Companion.ARG_PURPOSE_ID
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogAddPurpose.Companion.ARG_PURPOSE_NAME
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogAddPurpose.Companion.RK_ADD_PURPOSE
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@ExperimentalCoroutinesApi
class ExpenseDialog(private val expenseId: Int) : FullWidthDialogFragment() {

    private var expense: Expense? = null
    private val viewModel: ExpenseViewModel by viewModels()

    private val explicitDismiss
        get() = deleteBtnPressed || saveBtnPressed
    private var deleteBtnPressed = false
    private var saveBtnPressed = false

    private lateinit var binding: DialogFragExpenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            doOnTextChanged { text, _, _, _ ->
                updateExpense(date = text?.toDateOrNull() ?: LocalDate.now())
            }
        }

        binding.fragExpenseAmount.apply {
            doOnTextChanged { _, _, _, _ ->
                updateSaveButtonIsEnabled()
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateExpense(amount = this.text.toFloatOrNull())
                }
            }
        }
        binding.fragExpensePrice.apply {
            doOnTextChanged { _, _, _, _ ->
                updateSaveButtonIsEnabled()
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateExpense(pricePerGal = this.text.toFloatOrNull())
                }
            }
        }

        binding.fragExpensePurpose.onItemSelectedListener =
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
            ConfirmResetDialog().show(parentFragmentManager, null)
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
        if (!explicitDismiss || !deleteBtnPressed && (isNotEmpty() || saveBtnPressed)) {
            saveValues()
        }

        super.onDestroy()
    }

    private fun updateUI() {
        (context as MainActivity?)?.runOnUiThread {
            Log.d(TAG, "updateUI: $expense ${expense?.expenseId}")
            val tempExpense = expense
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

    private fun saveValues() {
        updateExpense(date = binding.fragExpenseDate.text.toDateOrNull() ?: LocalDate.now())
        updateExpense(amount = binding.fragExpenseAmount.text.toFloatOrNull())
        updateExpense(purpose = (binding.fragExpensePurpose.selectedItem as ExpensePurpose).purposeId)
        updateExpense(
            pricePerGal = if (expense?.purpose == GAS.id)
                binding.fragExpensePrice.text.toString().toFloatOrNull()
            else null
        )
    }

    private fun saveExpense() = expense?.let { viewModel.upsert(it) }

    private fun updateExpense(
        date: LocalDate? = null,
        amount: Float? = null,
        purpose: Int? = null,
        pricePerGal: Float? = null
    ) {
        expense?.let { e ->
            date?.let { e.date = it }
            amount?.let { e.amount = it }
            purpose?.let { e.purpose = it }
            pricePerGal?.let { e.pricePerGal = it }
            saveExpense()
        }
    }

    private fun clearFields() {
        Log.d(TAG, "clearFields")
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

        setFragmentResultListener(RK_ADD_PURPOSE) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            bundle.getInt(ARG_PURPOSE_ID).let { id ->
                if (result) {
                    bundle.getString(ARG_PURPOSE_NAME)?.let { purposeName ->
                        viewModel.upsert(ExpensePurpose(purposeId = id, name = purposeName))
                    }
                } else {
                    updateExpense(purpose = bundle.getInt(ARG_PREV_PURPOSE))
                    viewModel.delete(ExpensePurpose(purposeId = id))
                }
                binding.fragExpensePurpose.hideDropdown()
            }
        }
    }

    private fun isEmpty(): Boolean {
        val isTodaysDate = binding.fragExpenseDate.text == LocalDate.now().format(dtfDate)
        val amountIsBlank = binding.fragExpenseAmount.text.isNullOrBlank()
        val isGasExpense =
            binding.fragExpensePurpose.let { it.adapter.getItem(it.selectedItemPosition) } == GAS.id
        val priceIsBlank = binding.fragExpensePrice.text.isNullOrBlank()
        return isTodaysDate && amountIsBlank && !isGasExpense && priceIsBlank
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

            getItem(position)?.name?.let { viewHolder?.line?.text = it }

            return tempConvertView
        }

        override fun getDropDownView(
            position: Int, convertView: View?, parent: ViewGroup
        ): View {
            var tempConvertView: View?

            if (position == 0) {
                val binding = DialogFragExpensePurposeDropdownHeaderBinding.inflate(layoutInflater)
                tempConvertView = binding.root
                binding.addPurposeBtn.setOnClickListener {
                    CoroutineScope(Dispatchers.Default).launch {
                        withContext(Dispatchers.Default) {
                            viewModel.upsertAsync(ExpensePurpose())
                        }.let {
                            val prevPurpose = expense?.purpose
                            updateExpense(purpose = it.toInt())
                            ConfirmationDialogAddPurpose(
                                purposeId = it.toInt(),
                                prevPurpose = prevPurpose
                            ).show(
                                parentFragmentManager,
                                null
                            )
                        }
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
            var pos = -2
            itemList.forEachIndexed { index, purpose ->
                if (id == purpose.purposeId) {
                    pos = index
                }
            }
            return pos + 1
        }

        override fun getCount(): Int = super.getCount() + 1

        override fun getItem(position: Int): ExpensePurpose? =
            if (position == 0) {
                null
            } else {
                super.getItem(position - 1)
            }
    }

    data class PurposeSpinnerViewHolder(
        val line: TextView,
    )
}