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

package com.wtb.dashTracker.ui.fragment_expenses

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.ui.activity_main.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullExpense
import com.wtb.dashTracker.database.models.Purpose.GAS
import com.wtb.dashTracker.databinding.ListItemExpenseBinding
import com.wtb.dashTracker.databinding.ListItemExpenseNonGasBinding
import com.wtb.dashTracker.extensions.formatted
import com.wtb.dashTracker.extensions.getStringOrElse
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_EXTRA
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_expense.ExpenseDialog
import com.wtb.dashTracker.ui.fragment_base_list.BaseItemAdapter
import com.wtb.dashTracker.ui.fragment_base_list.BaseItemHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ExpenseListFragment : Fragment() {

    private val viewModel: ExpenseListViewModel by viewModels()
    private var callback: ExpenseListFragmentCallback? = null

    private lateinit var recyclerView: RecyclerView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ExpenseListFragmentCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.frag_item_list, container, false)

        recyclerView = view.findViewById(R.id.item_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        setDialogListeners()

        return view
    }

    private fun setDialogListeners() {
        setFragmentResultListener(
            ConfirmType.DELETE.key
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            val id = bundle.getInt(ARG_EXTRA)
            if (result) {
                viewModel.deleteExpenseById(id)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val expenseAdapter = ExpenseAdapter().apply {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    recyclerView.scrollToPosition(positionStart)
                }
            })
        }
        recyclerView.adapter = expenseAdapter

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.expenseList.collectLatest {
                    expenseAdapter.submitData(it)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    interface ExpenseListFragmentCallback

    inner class ExpenseAdapter : BaseItemAdapter<FullExpense>(
        DIFF_CALLBACK
    ) {
        override fun getViewHolder(parent: ViewGroup, viewType: Int?): BaseItemHolder<FullExpense> =
            when (viewType) {
                0 -> GasExpenseHolder(parent)
                else -> OtherExpenseHolder(parent)
            }

        override fun getItemViewType(position: Int): Int =
            when (getItem(position)?.purpose?.purposeId) {
                GAS.id -> 0
                else -> 1
            }

        abstract inner class ExpenseHolder(itemView: View) : BaseItemHolder<FullExpense>(itemView)

        inner class GasExpenseHolder(parent: ViewGroup) : ExpenseHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_expense, parent, false)
        ), View.OnClickListener {
            private val binding: ListItemExpenseBinding = ListItemExpenseBinding.bind(itemView)

            override val collapseArea: ViewGroup
                get() = binding.listItemDetails
            override val backgroundArea: ViewGroup
                get() = binding.listItemWrapper

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        ExpenseDialog.newInstance(item.id).show(
                            parentFragmentManager,
                            "edit_details"
                        )
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(confirmId = this@GasExpenseHolder.item.id)
                            .show(parentFragmentManager, null)
                    }
                }
            }

            override fun bind(item: FullExpense, payloads: MutableList<Any>?) {
                this.item = item

                binding.listItemTitle.text = this.item.expense.date.formatted.uppercase()
                binding.listItemTitle2.text =
                    getStringOrElse(R.string.currency_unit, "-", this.item.expense.amount)
                binding.listItemSubtitle.text = this.item.purpose.name
                binding.listItemPrice.text =
                    getStringOrElse(
                        R.string.currency_unit,
                        "-",
                        this.item.expense.pricePerGal
                    )
                binding.listItemGallons.text =
                    getStringOrElse(R.string.float_fmt, "-", this.item.expense.gallons)

                setPayloadVisibility(payloads)
            }
        }

        inner class OtherExpenseHolder(parent: ViewGroup) : ExpenseAdapter.ExpenseHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_expense_non_gas, parent, false)
        ), View.OnClickListener {
            private val binding: ListItemExpenseNonGasBinding =
                ListItemExpenseNonGasBinding.bind(itemView)

            override val collapseArea: ViewGroup
                get() = binding.buttonBox
            override val backgroundArea: ViewGroup
                get() = binding.listItemWrapper

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        ExpenseDialog.newInstance(item.id).show(
                            parentFragmentManager,
                            "edit_details"
                        )
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(confirmId = this@OtherExpenseHolder.item.id)
                            .show(parentFragmentManager, null)
                    }
                }
            }

            override fun bind(item: FullExpense, payloads: MutableList<Any>?) {
                this.item = item

                binding.listItemTitle.text = this.item.expense.date.formatted.uppercase()
                binding.listItemTitle2.text =
                    getStringOrElse(R.string.currency_unit, "-", this.item.expense.amount)
                binding.listItemSubtitle.text = this.item.purpose.name

                setPayloadVisibility(payloads)
            }
        }
    }

    companion object {
        private const val TAG = APP + "ExpenseListFragment"

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullExpense>() {
            override fun areItemsTheSame(oldItem: FullExpense, newItem: FullExpense): Boolean =
                oldItem.expense.expenseId == newItem.expense.expenseId


            override fun areContentsTheSame(
                oldItem: FullExpense,
                newItem: FullExpense
            ): Boolean =
                oldItem == newItem
        }
    }
}