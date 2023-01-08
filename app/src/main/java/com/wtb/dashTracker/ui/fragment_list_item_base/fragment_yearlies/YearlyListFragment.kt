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

package com.wtb.dashTracker.ui.fragment_list_item_base.fragment_yearlies

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.ListItemYearlyBinding
import com.wtb.dashTracker.databinding.ListItemYearlyDetailsTable2Binding
import com.wtb.dashTracker.databinding.YearlyMileageGridBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.DeductionCallback
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.BaseItemHolder
import com.wtb.dashTracker.ui.fragment_list_item_base.BaseItemListAdapter
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.Month

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class YearlyListFragment : ListItemFragment() {

    private val viewModel: YearlyListViewModel by viewModels()
    private var callback: IncomeFragment.IncomeFragmentCallback? = null

    private val yearlies = mutableListOf<Yearly>()

    private lateinit var recyclerView: RecyclerView
    private var deductionType: DeductionType = DeductionType.NONE

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as IncomeFragment.IncomeFragmentCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.frag_item_list, container, false)

        recyclerView = view.findViewById(R.id.item_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val entryAdapter = YearlyAdapter()
        recyclerView.adapter = entryAdapter

        callback?.deductionType?.asLiveData()?.observe(viewLifecycleOwner) {
            deductionType = it
            entryAdapter.notifyDataSetChanged()
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.yearlies.collectLatest { yearlies ->
                    entryAdapter.submitList(yearlies)
                }
            }
        }
    }

    var bpaList: MutableMap<Int, Float> = mutableMapOf()

    interface YearlyListFragmentCallback : DeductionCallback

    inner class YearlyAdapter : BaseItemListAdapter<Yearly>(DIFF_CALLBACK) {
        override fun getViewHolder(
            parent: ViewGroup,
            viewType: Int?
        ): BaseItemHolder<Yearly> =
            YearlyHolder(parent)

        inner class YearlyHolder(parent: ViewGroup) : BaseItemHolder<Yearly>(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_yearly, parent, false)
        ) {
            private val binding: ListItemYearlyBinding = ListItemYearlyBinding.bind(itemView)
            private val detailsBinding: ListItemYearlyDetailsTable2Binding =
                ListItemYearlyDetailsTable2Binding.bind(itemView)
            private val mileageGridBinding: YearlyMileageGridBinding =
                YearlyMileageGridBinding.bind(itemView)

            override val collapseArea: ConstraintLayout
                get() = binding.listItemDetails
            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper

            override fun bind(item: Yearly, payloads: List<Any>?) {
                fun updateExpenseFields(item: Yearly) {
                    fun showExpenseFields(hasMultipleStdDeductions: Boolean = false) {
                        binding.listItemSubtitle2Label.visibility = VISIBLE
                        binding.listItemSubtitle2.visibility = VISIBLE
                        detailsBinding.listItemYearlyExpensesRow.visibility = VISIBLE
                        if (hasMultipleStdDeductions) {
                            detailsBinding.mileageBreakdownCard.visibility = VISIBLE
                            detailsBinding.listItemYearlyCpmRow.visibility = GONE
                        } else {
                            detailsBinding.mileageBreakdownCard.visibility = GONE
                            detailsBinding.listItemYearlyCpmRow.visibility = VISIBLE
                        }
                    }

                    fun hideExpenseFields() {
                        binding.listItemSubtitle2Label.visibility = GONE
                        binding.listItemSubtitle2.visibility = GONE
                        detailsBinding.mileageBreakdownCard.visibility = GONE
                        detailsBinding.listItemYearlyCpmRow.visibility = GONE
                        detailsBinding.listItemYearlyExpensesRow.visibility = GONE
                    }

                    fun getCalculatedExpenses(costPerMile: Float): Float =
                        item.mileage * costPerMile

                    // TODO: Refactor.
                    fun getStandardDeductionExpense(): Float {
                        val table = viewModel.standardMileageDeductionTable()
                        var res = 0f
                        item.monthlies.keys.forEach { mon ->
                            val deduction: Float = table[LocalDate.of(item.year, mon, 1)]
                            res += deduction * (item.monthlies[mon]?.mileage ?: 0f)
                        }
                        return res
                    }

                    fun getExpenses(deductionType: DeductionType, costPerMile: Float = 0f) =
                        when (deductionType) {
                            DeductionType.IRS_STD -> getStandardDeductionExpense()
                            else -> getCalculatedExpenses(costPerMile)
                        }

                    fun getNet(cpm: Float, deductionType: DeductionType): Float =
                        item.totalPay - getExpenses(deductionType, cpm)

                    fun getHourly(cpm: Float): Float = getNet(cpm, deductionType) / item.hours

                    fun getMileageBreakdownData(cpm: Map<Int, Float>?): MutableMap<Int, Map<String, Float?>> {
                        val res = mutableMapOf<Int, Map<String, Float?>>()
                        var startMonth = 0
                        cpm?.keys?.forEach { endMonth ->
                            val miles: Float =
                                item.monthlies.filter { m -> m.key.value in startMonth..endMonth }
                                    .toList()
                                    .fold(0f) { acc, value ->
                                        acc + value.second.mileage
                                    }
                            res[endMonth] = mapOf("cpm" to cpm[endMonth], "miles" to miles)
                            startMonth = endMonth + 1
                        }

                        return res
                    }

                    CoroutineScope(Dispatchers.Default).launch {
                        withContext(Dispatchers.Default) {
                            viewModel.getAnnualCostPerMile(
                                this@YearlyHolder.item.year,
                                deductionType
                            )
                        }.let { cpm: Map<Int, Float>? ->
                            (context as MainActivity).runOnUiThread {
                                detailsBinding.mileageBreakdownCard.visibility = GONE

                                when (deductionType) {
                                    DeductionType.NONE -> hideExpenseFields()
                                    DeductionType.IRS_STD -> {
                                        val hasMultipleStdDeductions = cpm != null && cpm.size > 1

                                        showExpenseFields(hasMultipleStdDeductions = hasMultipleStdDeductions)

                                        if (hasMultipleStdDeductions) {
                                            mileageGridBinding.mileageBreakdownGrid.apply {
                                                removeViews(5, childCount - 5)
                                            }

                                            var startMonth = 1
                                            val mbd = getMileageBreakdownData(cpm)
                                            mbd.forEach { entry ->
                                                mileageGridBinding.mileageBreakdownGrid.apply {
                                                    val row = (childCount + 1) / 3
                                                    val dates = "${
                                                        Month.of(startMonth).toString()
                                                            .substring(0..2)
                                                    }-${
                                                        Month.of(entry.key).toString()
                                                            .substring(0..2)
                                                    }"
                                                    startMonth = entry.key + 1
                                                    yearlyMileageRow(
                                                        context,
                                                        this,
                                                        row,
                                                        dates,
                                                        entry.value["cpm"] ?: 0f,
                                                        entry.value["miles"] ?: 0f
                                                    )
                                                }
                                            }
                                        } else {
                                            detailsBinding.listItemDeductionType.text =
                                                deductionType.fullDesc

                                            detailsBinding.listItemYearlyCpm.text =
                                                getCpmIrsStdString(cpm)

                                        }

                                        detailsBinding.listItemYearlyExpenses.text =
                                            getCurrencyString(getExpenses(deductionType))

                                        binding.listItemSubtitle2.text =
                                            getCurrencyString(getNet(0f, deductionType))

                                        detailsBinding.listItemYearlyHourly.text =
                                            getCurrencyString(getHourly(0f))
                                    }
                                    else -> {
                                        showExpenseFields()

                                        val costPerMile: Float = cpm?.get(12) ?: 0f

                                        detailsBinding.listItemDeductionType.text =
                                            deductionType.fullDesc

                                        detailsBinding.listItemYearlyCpm.text =
                                            getCpmString(costPerMile)

                                        detailsBinding.listItemYearlyExpenses.text =
                                            getCurrencyString(getExpenses(deductionType, costPerMile))

                                        binding.listItemSubtitle2.text =
                                            getCurrencyString(getNet(costPerMile, deductionType))

                                        detailsBinding.listItemYearlyHourly.text =
                                            getCurrencyString(getHourly(costPerMile))
                                    }
                                }
                            }
                        }
                    }
                }

                this.item = item

                binding.listItemTitle.text = this.item.year.toString()
                binding.listItemTitle2.text = getCurrencyString(this.item.totalPay)
                detailsBinding.listItemReportedIncome.text =
                    getCurrencyString(this.item.reportedPay)
                detailsBinding.listItemCashTips.text = getCurrencyString(this.item.cashTips)
                detailsBinding.listItemYearlyMileage.text =
                    getStringOrElse(R.string.odometer_fmt, "-", this.item.mileage)
                detailsBinding.listItemYearlyHours.text =
                    getString(R.string.format_hours, item.hours)
                detailsBinding.listItemYearlyHourly.text = getCurrencyString(this.item.hourly)

                updateExpenseFields(item)

                setVisibilityFromPayloads(payloads)
            }
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
        root.addView(ValueTextView(context, pos = 0).apply {
            layoutParams = lp
            text = it
            textAlignment = TEXT_ALIGNMENT_TEXT_START
            textSize = context.getDimen(R.dimen.text_size_med)
        })
    }

    mCpm.let {
        val lp = GridLayout.LayoutParams().apply {
            columnSpec = GridLayout.spec(1, 1, 1f)
            rowSpec = GridLayout.spec(row)
        }
        root.addView(HeaderTextView(context, pos = 1).apply {
            layoutParams = lp
            text = context.getString(R.string.cpm_unit, it)
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
            0 -> setPaddingRelative(resources.getDimension(R.dimen.margin_default).toInt(), 0, 0, 0)
            2 -> setPaddingRelative(0, 0, resources.getDimension(R.dimen.margin_default).toInt(), 0)
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
            0 -> setPaddingRelative(resources.getDimension(R.dimen.margin_default).toInt(), 0, 0, 0)
            2 -> setPaddingRelative(0, 0, resources.getDimension(R.dimen.margin_default).toInt(), 0)
        }
    }
}
