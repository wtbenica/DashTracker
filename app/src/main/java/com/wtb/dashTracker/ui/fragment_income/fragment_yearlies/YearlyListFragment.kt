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

package com.wtb.dashTracker.ui.fragment_income.fragment_yearlies

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.view.setPadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.ListItemYearlyBinding
import com.wtb.dashTracker.databinding.ListItemYearlyDetailsTableBinding
import com.wtb.dashTracker.databinding.YearlyMileageGridBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.DeductionCallback
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogExpenseBreakdown
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialogMileageBreakdown
import com.wtb.dashTracker.ui.fragment_income.IncomeListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.aggregate_list_items.Yearly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class YearlyListFragment :
    IncomeListItemFragment<Yearly, Map<Int, Float>, YearlyListFragment.YearlyAdapter.YearlyHolder>() {

    override val entryAdapter: YearlyAdapter = YearlyAdapter()

    private val viewModel: YearlyListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.yearlies.collectLatest { yearlies ->
                    entryAdapter.submitList(yearlies)
                }
            }
        }
    }

    interface YearlyListFragmentCallback : DeductionCallback

    enum class MileageBreakdownKey {
        CPM, MILES
    }

    inner class YearlyAdapter :
        IncomeItemListAdapter<Yearly, Map<Int, Float>, YearlyAdapter.YearlyHolder>(DIFF_CALLBACK) {
        override fun getViewHolder(parent: ViewGroup, viewType: Int?): YearlyHolder =
            YearlyHolder(parent)

        inner class YearlyHolder(parent: ViewGroup) : IncomeItemHolder<Yearly, Map<Int, Float>>(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_yearly, parent, false)
        ) {
            // BaseItemHolder Overrides
            override val collapseArea: Map<View, Int?>
                get() = mapOf(binding.listItemDetails to null)

            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper

            override val bgCard: CardView
                get() = binding.root

            override val parentFrag: ListItemFragment
                get() = this@YearlyListFragment

            // IncomeItemHolder Overrides
            override var expenseValues: Map<Int, Float> = emptyMap()

            override val holderDeductionTypeFlow: StateFlow<DeductionType>
                get() = incomeDeductionTypeFlow

            private val binding: ListItemYearlyBinding = ListItemYearlyBinding.bind(itemView)

            private val detailsBinding: ListItemYearlyDetailsTableBinding =
                binding.listItemDetailsTableCard.apply {
                    listItemYearlyExpenseDetailsButton.setOnClickListener { v ->
                        ConfirmationDialogExpenseBreakdown(mItem).show(childFragmentManager, null)
                    }
                    mileageDetailsButton.setOnClickListener { v ->
                        ConfirmationDialogMileageBreakdown(mItem).show(childFragmentManager, null)
                    }
                }

            private val mileageGridBinding: YearlyMileageGridBinding =
                YearlyMileageGridBinding.bind(itemView)

            private val hasMultipleStdDeductions: Boolean
                get() {
                    val irsStd = deductionType == DeductionType.IRS_STD
                    val multValues = expenseValues.size > 1
                    return irsStd && multValues
                }

            // BaseItemHolder Overrides
            override fun updateHeaderFields() {
                binding.listItemTitle.text = this.mItem.year.toString()
                binding.listItemTitle2.text = getCurrencyString(this.mItem.totalPay)
            }

            override fun updateDetailsFields() {
                detailsBinding.listItemReportedIncome.text =
                    getCurrencyString(this.mItem.reportedPay)
                detailsBinding.listItemCashTips.text = getCurrencyString(this.mItem.cashTips)
                detailsBinding.listItemYearlyMileage.text =
                    getStringOrElse(R.string.odometer_fmt, "-", this.mItem.mileage)
                detailsBinding.listItemYearlyHours.text =
                    getString(R.string.format_hours, mItem.hours)
                detailsBinding.listItemYearlyHourly.text = getCurrencyString(this.mItem.hourly)
            }

            // IncomeItemHolder Overrides
            override fun updateExpenseFieldVisibilities() {
                binding.listItemSubtitle2Label.fade(fadeIn = shouldShow)
                binding.listItemSubtitle2.fade(fadeIn = shouldShow)

                detailsBinding.apply {
                    listItemYearlyExpensesHeader.showOrHide(shouldShow = shouldShow)
                    listItemYearlyExpenseDetailsButtonFrame.apply {
                        val target = resources.getDimension(R.dimen.min_touch_target).toInt()
                        revealToHeightIfTrue(
                            shouldExpand = shouldShow,
                            toHeight = target,
                            toWidth = target
                        )
                    }

                    listItemYearlyExpenseDetailsButton.showOrHide(shouldShow = shouldShow)
                    listItemYearlyExpenses.showOrHide(shouldShow = shouldShow)

                    mileageBreakdownCard.showOrHide(
                        shouldShow = shouldShow && hasMultipleStdDeductions,
                        onHiddenVisibility = GONE,
                        animate = mIsExpanded
                    )

                    listItemYearlyCpmHeader.showOrHide(
                        shouldShow = shouldShow && !hasMultipleStdDeductions
                    )
                    listItemYearlyCpmDeductionType.showOrHide(
                        shouldShow = shouldShow && !hasMultipleStdDeductions
                    )
                    listItemYearlyCpm.showOrHide(
                        shouldShow = shouldShow && !hasMultipleStdDeductions
                    )
                }

            }

            override fun updateExpenseFieldValues() {
                /**
                 * Update mileage breakdown table
                 */
                fun updateMileageBreakdownTable(expenseValues: Map<Int, Float>?) {
                    fun getMileageBreakdownData(cpm: Map<Int, Float>?): MutableMap<Int, Map<MileageBreakdownKey, Float?>> {
                        val res = mutableMapOf<Int, Map<MileageBreakdownKey, Float?>>()
                        var startMonth = 0
                        cpm?.keys?.forEach { endMonth ->
                            val miles: Float =
                                mItem.monthlies.filter { m -> m.key.value in startMonth..endMonth }
                                    .toList()
                                    .fold(0f) { acc, value ->
                                        acc + value.second.mileage
                                    }
                            res[endMonth] = mapOf(
                                MileageBreakdownKey.CPM to cpm[endMonth],
                                MileageBreakdownKey.MILES to miles
                            )
                            startMonth = endMonth + 1
                        }

                        return res
                    }

                    val initChildren = 3
                    val spannedColumns = 0
                    mileageGridBinding.mileageBreakdownGrid.apply {
                        removeViews(initChildren, childCount - initChildren)
                    }

                    var startMonth = 1
                    val mbd = getMileageBreakdownData(expenseValues)
                    mbd.forEach { entry ->
                        mileageGridBinding.mileageBreakdownGrid.apply {
                            val row = (childCount + spannedColumns) / 3
                            val dates = "${
                                Month.of(startMonth).toString()
                                    .substring(0..2)
                                    .capitalize()
                            }-${
                                Month.of(entry.key).toString()
                                    .substring(0..2)
                                    .capitalize()
                            }"
                            startMonth = entry.key + 1
                            yearlyMileageRow(
                                context = context,
                                root = this,
                                row = row,
                                mDateRange = dates,
                                mCpm = entry.value[MileageBreakdownKey.CPM]
                                    ?: 0f,
                                mMiles = entry.value[MileageBreakdownKey.MILES]
                                    ?: 0f
                            )
                        }
                    }
                }

                /**
                 * Calculate yearly expenses using the appropriate method according to [deductionType]
                 */
                fun getExpenses(deductionType: DeductionType, costPerMile: Float = 0f): Float {
                    /**
                     * Use IRS Std Deduction to calculate mileage expenses for the year
                     */
                    fun getStandardDeductionExpense(): Float {
                        val table = viewModel.standardMileageDeductionTable()
                        var res = 0f
                        mItem.monthlies.keys.forEach { mon ->
                            val stdDedAmount: Float = table[LocalDate.of(mItem.year, mon, 1)]
                            res += stdDedAmount * (mItem.monthlies[mon]?.mileage ?: 0f)
                        }
                        return res
                    }

                    return when (deductionType) {
                        DeductionType.IRS_STD -> getStandardDeductionExpense()
                        else -> mItem.mileage * costPerMile
                    }
                }

                fun getNet(cpm: Float, deductionType: DeductionType): Float =
                    mItem.totalPay - getExpenses(deductionType, cpm)

                fun getHourly(cpm: Float): Float = getNet(cpm, deductionType) / mItem.hours

                if (hasMultipleStdDeductions) {
                    updateMileageBreakdownTable(expenseValues)
                }

                if (shouldShow) {
                    detailsBinding.listItemYearlyCpmDeductionType.text =
                        deductionType.fullDesc

                    val costPerMile: Float =
                        if (deductionType == DeductionType.IRS_STD) {
                            0f
                        } else {
                            expenseValues.get(12) ?: 0f
                        }

                    detailsBinding.listItemYearlyCpm.text =
                        if (deductionType == DeductionType.IRS_STD) {
                            getCpmIrsStdString(expenseValues)
                        } else {
                            formatCpm(costPerMile)
                        }

                    detailsBinding.listItemYearlyExpenses.text =
                        getCurrencyString(getExpenses(deductionType, costPerMile))

                    binding.listItemSubtitle2.text =
                        getCurrencyString(getNet(costPerMile, deductionType))

                    detailsBinding.listItemYearlyHourly.text =
                        getCurrencyString(getHourly(costPerMile))
                }
            }

            override suspend fun getExpenseValues(deductionType: DeductionType): Map<Int, Float> =
                viewModel.getAnnualCostPerMile(
                    this@YearlyHolder.mItem.year,
                    deductionType
                ) ?: emptyMap()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Yearly>() {
            override fun areItemsTheSame(
                oldItem: Yearly,
                newItem: Yearly
            ): Boolean =
                oldItem.year == newItem.year


            override fun areContentsTheSame(
                oldItem: Yearly,
                newItem: Yearly
            ): Boolean =
                oldItem == newItem
        }
    }
}

fun yearlyMileageRow(
    context: Context,
    root: ViewGroup,
    row: Int,
    mDateRange: String,
    mCpm: Float,
    mMiles: Float
) {
    mDateRange.let {
        val lp = GridLayout.LayoutParams().apply {
            columnSpec = GridLayout.spec(0, 1, 1f)
            rowSpec = GridLayout.spec(row)
        }

        root.addView(ValueTextView(context).apply {
            layoutParams = lp
            text = it
            textAlignment = TEXT_ALIGNMENT_TEXT_START
            textSize = context.getDimen(R.dimen.text_size_med)
            setPadding(0)
        })
    }

    mCpm.let {
        val lp = GridLayout.LayoutParams().apply {
            columnSpec = GridLayout.spec(1, 1, 1f)
            rowSpec = GridLayout.spec(row)
        }
        root.addView(HeaderTextView(context).apply {
            layoutParams = lp
            text = context.getString(R.string.irs_cpm_unit, it)
            textAlignment = TEXT_ALIGNMENT_TEXT_END
            textSize = context.getDimen(R.dimen.text_size_med)
            setPadding(0)
        })
    }

    mMiles.let {
        val lp = GridLayout.LayoutParams().apply {
            columnSpec = GridLayout.spec(2, 1, 1f)
            rowSpec = GridLayout.spec(row)
        }
        root.addView(ValueTextView(context).apply {
            layoutParams = lp
            text = it.toInt().toString()
            textAlignment = TEXT_ALIGNMENT_TEXT_END
            textSize = context.getDimen(R.dimen.text_size_med)
            setPadding(0)
        })
    }
}

class ValueTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    pos: Int = 2
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.textview_value_style, null)
        when (pos) {
            0 -> setPaddingRelative(resources.getDimension(R.dimen.margin_wide).toInt(), 0, 0, 0)
            2 -> setPaddingRelative(0, 0, resources.getDimension(R.dimen.margin_wide).toInt(), 0)
        }
    }
}

class HeaderTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    pos: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.textview_header_style, null)
        when (pos) {
            0 -> setPaddingRelative(resources.getDimension(R.dimen.margin_wide).toInt(), 0, 0, 0)
            2 -> setPaddingRelative(0, 0, resources.getDimension(R.dimen.margin_wide).toInt(), 0)
        }
    }
}
