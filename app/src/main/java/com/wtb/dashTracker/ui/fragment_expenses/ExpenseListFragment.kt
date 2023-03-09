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
import androidx.cardview.widget.CardView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullExpense
import com.wtb.dashTracker.database.models.Purpose.GAS
import com.wtb.dashTracker.databinding.ListItemExpenseBinding
import com.wtb.dashTracker.databinding.ListItemExpenseNonGasBinding
import com.wtb.dashTracker.extensions.formatted
import com.wtb.dashTracker.extensions.getStringOrElse
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDialog
import com.wtb.dashTracker.ui.dialog_confirm.SimpleConfirmationDialog.Companion.ARG_EXTRA_ITEM_ID
import com.wtb.dashTracker.ui.dialog_confirm.SimpleConfirmationDialog.Companion.ARG_IS_CONFIRMED
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_expense.ExpenseDialog
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class ExpenseListFragment : ListItemFragment() {

    private val viewModel: ExpenseListViewModel by viewModels()
    private var callback: ExpenseListFragmentCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as ExpenseListFragmentCallback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setDialogListeners()
    }

    private fun setDialogListeners() {
        childFragmentManager.setFragmentResultListener(
            ConfirmDialog.DELETE.key,
            this
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_IS_CONFIRMED)
            val id = bundle.getLong(ARG_EXTRA_ITEM_ID)
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

    @ExperimentalAnimationApi
    inner class ExpenseAdapter : BaseItemPagingDataAdapter<FullExpense>(
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

        @ExperimentalAnimationApi
        inner class GasExpenseHolder(parent: ViewGroup) : ExpenseHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_expense, parent, false)
        ), View.OnClickListener {
            private val binding: ListItemExpenseBinding = ListItemExpenseBinding.bind(itemView)

            override val collapseArea: Array<View>
                get() = arrayOf(
                    binding.listItemDetailsCard,
                    binding.listItemBtnDelete,
                    binding.listItemBtnEdit
                )
            override val backgroundArea: ViewGroup
                get() = binding.listItemWrapper
            override val bgCard: CardView
                get() = binding.root

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        ExpenseDialog.newInstance(item.id).show(
                            childFragmentManager,
                            "edit_details"
                        )
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(confirmId = this@GasExpenseHolder.item.id)
                            .show(childFragmentManager, null)
                    }
                }
            }

            override fun bind(item: FullExpense, payloads: List<Any>?) {
                this.item = item

                if (this.item.isEmpty) {
                    viewModel.delete(item.expense)
                }

                binding.listItemTitle.text = this.item.expense.date.formatted
                binding.listItemTitle2.text =
                    getStringOrElse(R.string.currency_fmt, "-", this.item.expense.amount)
                binding.listItemSubtitle.text = this.item.purpose.name
                binding.listItemPrice.text =
                    getStringOrElse(
                        R.string.gas_price_display,
                        "-",
                        this.item.expense.pricePerGal
                    )
                binding.listItemGallons.text =
                    getStringOrElse(R.string.float_fmt, "-", this.item.expense.gallons)

                setVisibilityFromPayloads(payloads)
            }
        }

        @ExperimentalAnimationApi
        inner class OtherExpenseHolder(parent: ViewGroup) : ExpenseAdapter.ExpenseHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_expense_non_gas, parent, false)
        ), View.OnClickListener {
            private val binding: ListItemExpenseNonGasBinding =
                ListItemExpenseNonGasBinding.bind(itemView)

            override val collapseArea: Array<View>
                get() = arrayOf(binding.buttonBox)
            override val backgroundArea: ViewGroup
                get() = binding.listItemWrapper
            override val bgCard: CardView
                get() = binding.root

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        ExpenseDialog.newInstance(item.id).show(
                            childFragmentManager,
                            "edit_details"
                        )
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(confirmId = this@OtherExpenseHolder.item.id)
                            .show(childFragmentManager, null)
                    }
                }
            }

            override fun bind(item: FullExpense, payloads: List<Any>?) {
                this.item = item

                binding.listItemTitle.text = this.item.expense.date.formatted
                binding.listItemTitle2.text =
                    getStringOrElse(R.string.currency_fmt, "-", this.item.expense.amount)
                binding.listItemSubtitle.text = this.item.purpose.name

                setVisibilityFromPayloads(payloads)
            }
        }
    }

    companion object {
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