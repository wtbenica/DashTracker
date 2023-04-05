/*
 * Copyright 2023 Wesley T. Benica
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

package com.wtb.dashTracker.ui.fragment_expenses.fragment_dailies

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
import com.wtb.dashTracker.ui.fragment_expenses.ExpenseListItemFragment
import com.wtb.dashTracker.ui.fragment_expenses.ExpenseListViewModel
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class ExpenseListFragment : ExpenseListItemFragment() {

    private val viewModel: ExpenseListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setDialogListeners()
    }

    // TODO: This can be moved to superclass to get rid of redundant showToolbarsAndFab
    private fun setDialogListeners() {
        childFragmentManager.setFragmentResultListener(
            ConfirmDialog.DELETE.key,
            this
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_IS_CONFIRMED)
            val id = bundle.getLong(ARG_EXTRA_ITEM_ID)
            if (result) {
                viewModel.deleteExpenseById(id)
                (requireContext() as ListItemFragmentCallback).showToolbarsAndFab()
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

    @ExperimentalAnimationApi
    inner class ExpenseAdapter :
        BaseItemPagingDataAdapter<FullExpense, ExpenseAdapter.ExpenseHolder>(
            DIFF_CALLBACK
        ) {
        override fun getViewHolder(parent: ViewGroup, viewType: Int?): ExpenseHolder =
            when (viewType) {
                0 -> GasExpenseHolder(parent)
                else -> OtherExpenseHolder(parent)
            }

        override fun getItemViewType(position: Int): Int =
            when (getItem(position)?.purpose?.purposeId) {
                GAS.id -> 0
                else -> 1
            }

        abstract inner class ExpenseHolder(itemView: View) : BaseItemHolder<FullExpense>(itemView) {
            override val parentFrag: ListItemFragment
                get() = this@ExpenseListFragment
        }

        @ExperimentalAnimationApi
        inner class GasExpenseHolder(parent: ViewGroup) : ExpenseHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_expense, parent, false)
        ), View.OnClickListener {
            private val binding: ListItemExpenseBinding = ListItemExpenseBinding.bind(itemView)

            override val collapseArea: Map<View, Int?>
                get() {
                    val minTouchTarget = resources.getDimension(R.dimen.min_touch_target)
                        .toInt()
                    return mapOf(
                        binding.listItemDetailsCard to null,
                        binding.listItemBtnDelete to minTouchTarget,
                        binding.listItemBtnEdit to minTouchTarget
                    )
                }
            override val backgroundArea: ViewGroup
                get() = binding.listItemWrapper
            override val bgCard: CardView
                get() = binding.root

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        ExpenseDialog.newInstance(mItem.id).show(
                            childFragmentManager,
                            "edit_details"
                        )
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(confirmId = this@GasExpenseHolder.mItem.id)
                            .show(childFragmentManager, null)
                    }
                }
            }

            override fun updateHeaderFields() {
                if (this.mItem.isEmpty) {
                    viewModel.delete(mItem.expense)
                }

                binding.listItemTitle.text = this.mItem.expense.date.formatted
                binding.listItemTitle2.text =
                    getStringOrElse(R.string.currency_fmt, "-", this.mItem.expense.amount)
                binding.listItemSubtitle.text = this.mItem.purpose.name
                binding.listItemPrice.text =
                    getStringOrElse(
                        R.string.gas_price_display,
                        "-",
                        this.mItem.expense.pricePerGal
                    )
                binding.listItemGallons.text =
                    getStringOrElse(R.string.float_fmt, "-", this.mItem.expense.gallons)
            }

            override fun updateDetailsFields() {}
        }

        @ExperimentalAnimationApi
        inner class OtherExpenseHolder(parent: ViewGroup) : ExpenseHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_expense_non_gas, parent, false)
        ), View.OnClickListener {
            private val binding: ListItemExpenseNonGasBinding =
                ListItemExpenseNonGasBinding.bind(itemView)

            override val collapseArea: Map<View, Int?>
                get() = mapOf(binding.buttonBox to null)
            override val backgroundArea: ViewGroup
                get() = binding.listItemWrapper
            override val bgCard: CardView
                get() = binding.root

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        ExpenseDialog.newInstance(mItem.id).show(
                            childFragmentManager,
                            "edit_details"
                        )
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(confirmId = this@OtherExpenseHolder.mItem.id)
                            .show(childFragmentManager, null)
                    }
                }
            }

            override fun updateHeaderFields() {
                binding.listItemTitle.text = this.mItem.expense.date.formatted
                binding.listItemTitle2.text =
                    getStringOrElse(R.string.currency_fmt, "-", this.mItem.expense.amount)
                binding.listItemSubtitle.text = this.mItem.purpose.name
            }

            override fun updateDetailsFields() {}
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