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

package com.wtb.dashTracker.ui.frag_list_expense

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.MainActivity.Companion.APP
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
import com.wtb.dashTracker.ui.dialog_expense.ExpenseDialog
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

    inner class ExpenseAdapter :
        PagingDataAdapter<FullExpense, ExpenseAdapter.ExpenseHolder>(DIFF_CALLBACK) {

        override fun onBindViewHolder(
            holder: ExpenseHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            super.onBindViewHolder(holder, position, payloads)
            getItem(position)?.let { holder.bind(it, payloads) }
        }

        override fun onBindViewHolder(holder: ExpenseHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseHolder =
            when (viewType) {
                0 -> GasExpenseHolder(parent)
                else -> OtherExpenseHolder(parent)
            }

        override fun getItemViewType(position: Int): Int =
            when (getItem(position)?.purpose?.purposeId) {
                GAS.id -> 0
                else -> 1
            }

        abstract inner class ExpenseHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            abstract fun bind(item: FullExpense, payloads: MutableList<Any>? = null)
        }

        inner class GasExpenseHolder(parent: ViewGroup) : ExpenseHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_expense, parent, false)
        ), View.OnClickListener {
            private lateinit var expense: FullExpense

            private val binding: ListItemExpenseBinding = ListItemExpenseBinding.bind(itemView)
            private val detailsTable: ConstraintLayout = binding.listItemDetails

            init {
                itemView.setOnClickListener(this)

                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        ExpenseDialog.newInstance(expense.id).show(
                            parentFragmentManager,
                            "edit_details"
                        )
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(confirmId = this@GasExpenseHolder.expense.id)
                            .show(parentFragmentManager, null)
                    }
                }
            }

            override fun onClick(v: View?) {
                val currentVisibility = detailsTable.visibility
                detailsTable.visibility = if (currentVisibility == VISIBLE) GONE else VISIBLE
                binding.listItemWrapper.setBackgroundResource(if (currentVisibility == VISIBLE) R.drawable.bg_list_item else R.drawable.bg_list_item_expanded)
                bindingAdapter?.notifyItemChanged(bindingAdapterPosition, detailsTable.visibility)
            }

            override fun bind(item: FullExpense, payloads: MutableList<Any>?) {
                this.expense = item

                val detailsTableIsVisibile = (payloads?.let {
                    if (it.size == 1 && it[0] in listOf(VISIBLE, GONE)) it[0] else null
                } ?: GONE) == VISIBLE

                binding.listItemWrapper.setBackgroundResource(if (detailsTableIsVisibile) R.drawable.bg_list_item_expanded else R.drawable.bg_list_item)

                binding.listItemTitle.text = this.expense.expense.date.formatted.uppercase()
                binding.listItemTitle2.text =
                    getStringOrElse(R.string.currency_unit, "-", this.expense.expense.amount)
                binding.listItemSubtitle.text = this.expense.purpose.name
                binding.listItemDetailsCard.visibility =
                    if (expense.purpose.purposeId == GAS.id) VISIBLE else GONE
                if (expense.purpose.purposeId == GAS.id) {
                    binding.listItemPrice.text =
                        getStringOrElse(
                            R.string.currency_unit,
                            "-",
                            this.expense.expense.pricePerGal
                        )
                    binding.listItemGallons.text =
                        getStringOrElse(R.string.float_fmt, "-", this.expense.expense.gallons)
                }
            }
        }

        inner class OtherExpenseHolder(parent: ViewGroup) : ExpenseHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_expense_non_gas, parent, false)
        ), View.OnClickListener {
            private lateinit var expense: FullExpense

            private val binding: ListItemExpenseNonGasBinding =
                ListItemExpenseNonGasBinding.bind(itemView)
            private val buttonBox: LinearLayout = binding.buttonBox

            init {
                itemView.setOnClickListener(this)

                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        ExpenseDialog.newInstance(expense.id).show(
                            parentFragmentManager,
                            "edit_details"
                        )
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(confirmId = this@OtherExpenseHolder.expense.id)
                            .show(parentFragmentManager, null)
                    }
                }
            }

            override fun onClick(v: View?) {
                val currentVisibility = buttonBox.visibility
                buttonBox.visibility = if (currentVisibility == VISIBLE) GONE else VISIBLE
                binding.listItemWrapper.setBackgroundResource(if (currentVisibility == VISIBLE) R.drawable.bg_list_item else R.drawable.bg_list_item_expanded)
                bindingAdapter?.notifyItemChanged(bindingAdapterPosition, buttonBox.visibility)
            }

            override fun bind(item: FullExpense, payloads: MutableList<Any>?) {
                this.expense = item

                val detailsTableIsVisibile = (payloads?.let {
                    if (it.size == 1 && it[0] in listOf(
                            VISIBLE,
                            GONE
                        )
                    ) it[0] else null
                } ?: GONE) == VISIBLE

                binding.listItemWrapper.setBackgroundResource(if (detailsTableIsVisibile) R.drawable.bg_list_item_expanded else R.drawable.bg_list_item)

                binding.listItemTitle.text = this.expense.expense.date.formatted.uppercase()
                binding.listItemTitle2.text =
                    getStringOrElse(R.string.currency_unit, "-", this.expense.expense.amount)
                binding.listItemSubtitle.text = this.expense.purpose.name
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