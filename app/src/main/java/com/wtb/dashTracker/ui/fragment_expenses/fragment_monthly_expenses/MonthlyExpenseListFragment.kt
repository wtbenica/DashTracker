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

package com.wtb.dashTracker.ui.fragment_expenses.fragment_monthly_expenses

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.databinding.ListItemDetailsTableBinding
import com.wtb.dashTracker.databinding.ListItemHolderBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.dialog_confirm.composables.HeaderText
import com.wtb.dashTracker.ui.dialog_confirm.composables.ValueText
import com.wtb.dashTracker.ui.fragment_expenses.ExpenseListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.lang.Float.max
import java.lang.Float.min
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class MonthlyExpenseListFragment : ExpenseListItemFragment() {

    private val viewModel: MonthlyExpenseListViewModel by viewModels()
    private var purposes: List<ExpensePurpose>? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val entryAdapter = MonthlyExpenseAdapter()
        recyclerView.adapter = entryAdapter

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.monthlies.collectLatest { monthlies ->
                    entryAdapter.submitList(monthlies)
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.expensePurposes.collectLatest { eps ->
                    purposes = eps
                }
            }
        }
    }

    inner class MonthlyExpenseAdapter :
        ListItemFragment.BaseItemListAdapter<MonthlyExpenses>(DIFF_CALLBACK) {

        override fun onBindViewHolder(
            holder: BaseItemHolder<MonthlyExpenses>,
            position: Int,
            payloads: List<Any>
        ) {
            if (getItem(position)?.showInList == true) {
                super.onBindViewHolder(holder, position, payloads)
            }
        }

        override fun onBindViewHolder(holder: BaseItemHolder<MonthlyExpenses>, position: Int) {
            if (getItem(position)?.showInList == true) {
                super.onBindViewHolder(holder, position)
            }
        }

        override fun getViewHolder(
            parent: ViewGroup,
            viewType: Int?
        ): BaseItemHolder<MonthlyExpenses> = MonthlyExpenseHolder(parent)

        inner class MonthlyExpenseHolder(parent: ViewGroup) : BaseItemHolder<MonthlyExpenses>(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_holder, parent, false)
        ) {
            private val binding: ListItemHolderBinding = ListItemHolderBinding.bind(itemView)

            private val detailsBinding: ListItemDetailsTableBinding =
                ListItemDetailsTableBinding.bind(itemView)

            override val collapseArea: Array<View>
                get() = arrayOf(binding.listItemDetails)

            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper

            override val bgCard: CardView
                get() = binding.root

            override fun bind(item: MonthlyExpenses, payloads: List<Any>?) {
                this.item = item

                binding.apply {
                    listItemTitle.text =
                        this@MonthlyExpenseHolder.item.date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

                    listItemTitle2.text = getCurrencyString(item.total)

                    listItemSubtitle.revealIfTrue(false)
                    listItemSubtitle2.revealIfTrue(false)
                    listItemSubtitle2Label.revealIfTrue(false)

                    listItemBtnEdit.visibility = GONE
                    listItemBtnDelete.visibility = GONE
                }

                detailsBinding.apply {
                    listItemWorkMiles.text =
                        getString(R.string.odometer_fmt, item.workMiles)

                    listItemTotalMiles.text = item.totalMiles.toString()

                    listItemPctWorkMiles.text =
                        getString(R.string.percent_int, item.workMilePercentDisplay)

                    val showWorkCosts = item.workCost > 0f
                    listItemWorkCosts.apply {
                        setVisibleIfTrue(showWorkCosts)
                        text = getString(R.string.currency_fmt, item.workCost)
                    }

                    listItemWorkCostsLabel.setVisibleIfTrue(showWorkCosts)

                    val showTopDetailsTable = item.workMiles > 0f
                    val showBottomDetailsTable = purposes?.isNotEmpty() == true

                    detailsTableSplit.setVisibleIfTrue(showTopDetailsTable && showBottomDetailsTable)

                    topDetailsTable.setVisibleIfTrue(showTopDetailsTable)

                    bottomDetailsTable.apply {
                        setVisibleIfTrue(showBottomDetailsTable)
                        if (showBottomDetailsTable) {
                            setContent {
                                DashTrackerTheme {
                                    MonthlyExpensesDetails(item, purposes)
                                }
                            }
                        }
                    }
                }

                setVisibilityFromPayloads(payloads)
            }
        }
    }

    @Composable
    private fun MonthlyExpensesDetails(item: MonthlyExpenses, purposes: List<ExpensePurpose>?) {
        val mPurposes: MutableList<ExpensePurpose>? = purposes?.toMutableList()
        Column {
            item.expenses.toList().sortedByDescending { it.second }.forEach {
                mPurposes?.remove(it.first)
                Row(
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.margin_default))
                ) {
                    HeaderText(text = it.first.name.toString(), padding = 0.dp)
                    ValueText(
                        text = stringResource(
                            id = R.string.currency_fmt,
                            it.second
                        ),
                        padding = 0.dp,
                        textSize = 16.sp
                    )
                }
            }
            mPurposes?.sortedBy { it.name }?.forEach {
                Row(
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.margin_default))
                ) {
                    HeaderText(text = it.name.toString(), padding = 0.dp)
                    ValueText(
                        text = stringResource(
                            id = R.string.currency_fmt,
                            0f
                        ),
                        padding = 0.dp,
                        textSize = 16.sp
                    )
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MonthlyExpenses>() {
            override fun areItemsTheSame(
                oldItem: MonthlyExpenses,
                newItem: MonthlyExpenses
            ): Boolean =
                oldItem.date == newItem.date &&
                        oldItem.expenses == newItem.expenses


            override fun areContentsTheSame(
                oldItem: MonthlyExpenses,
                newItem: MonthlyExpenses
            ): Boolean =
                oldItem.date == newItem.date &&
                        oldItem.expenses == newItem.expenses
        }
    }
}

@ExperimentalCoroutinesApi
fun MutableCollection<MonthlyExpenses>.fix(): List<MonthlyExpenses> {
    val res = this.sortedByDescending { it.date }

    res.forEachIndexed { index, monthlyExpenses ->
        val prevMonthlyExpense = res.getOrNull(index + 1)
        monthlyExpenses.adjustEndStartOdometers(prevMonthlyExpense)
    }

    return res
}

@ExperimentalCoroutinesApi
class MonthlyExpenses(val date: LocalDate) : ListItemType {
    private val _expenses: MutableMap<ExpensePurpose, Float> = mutableMapOf()
    val expenses: Map<ExpensePurpose, Float>
        get() = _expenses

    internal var startOdometer: Float? = null
    internal var endOdometer: Float? = null
    internal var workMiles: Float = 0f

    internal val totalMiles: Int
        get() = endOdometer?.let { eo -> startOdometer?.let { so -> eo - so } }?.toInt() ?: 0

    private val workMilePercent: Float
        get() = if (totalMiles != 0) {
            workMiles / totalMiles
        } else {
            0f
        }

    internal val workMilePercentDisplay: Int
        get() = (workMilePercent * 100).toInt()

    internal val workCost: Float
        get() = workMilePercent * total

    val total: Float
        get() = expenses.values.reduceOrNull { acc, v ->
            acc + v
        } ?: 0f

    val showInList: Boolean
        get() = total > 0f || workMiles > 0f

    fun addExpense(purpose: ExpensePurpose, amount: Float) {
        val current = _expenses.getOrDefault(purpose, 0f)
        _expenses[purpose] = current + amount
    }

    fun addEntry(entry: DashEntry) {
        val newStart: Float? = entry.startOdometer
        val oldStart: Float? = startOdometer

        startOdometer =
            if (newStart != null && oldStart != null) {
                min(newStart, oldStart)
            } else newStart ?: oldStart

        val newEnd: Float? = entry.endOdometer
        val oldEnd: Float? = endOdometer

        endOdometer =
            if (newEnd != null && oldEnd != null) {
                max(newEnd, oldEnd)
            } else newEnd ?: oldEnd

        val addedMiles = entry.mileage ?: 0f
        workMiles += addedMiles
    }

    fun getAmount(purpose: ExpensePurpose): Float = expenses.getOrDefault(purpose, 0f)

    fun adjustEndStartOdometers(other: MonthlyExpenses?) {
        if (other != null && this.date.isPreviousMonth(other.date)) {
            val currStart = this.startOdometer
            val prevEnd = other.endOdometer
            when {
                currStart == null && prevEnd != null -> {
                    this.startOdometer = prevEnd
                }
                currStart != null && prevEnd == null -> {
                    other.endOdometer = currStart
                }
                currStart != null &&
                        prevEnd != null &&
                        currStart > prevEnd -> {
                    val mid: Float = (currStart + prevEnd) / 2
                    this.startOdometer = mid
                    other.endOdometer = mid
                }
            }
        }
    }
}

fun LocalDate.isPreviousMonth(other: LocalDate): Boolean {
    return this.minusMonths(1) == other
}
