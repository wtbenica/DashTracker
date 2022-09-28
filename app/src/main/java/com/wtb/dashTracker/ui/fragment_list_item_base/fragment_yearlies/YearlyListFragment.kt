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
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.wtb.dashTracker.databinding.ListItemYearlyDetailsTableBinding
import com.wtb.dashTracker.extensions.getCpmIrsStdString
import com.wtb.dashTracker.extensions.getCpmString
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getStringOrElse
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
            private val detailsBinding: ListItemYearlyDetailsTableBinding =
                ListItemYearlyDetailsTableBinding.bind(itemView)

            override val collapseArea: ConstraintLayout
                get() = binding.listItemDetails
            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper

            override fun bind(item: Yearly, payloads: List<Any>?) {
                fun showExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = VISIBLE
                    binding.listItemSubtitle2.visibility = VISIBLE
                    detailsBinding.listItemYearlyCpmRow.visibility = VISIBLE
                    detailsBinding.listItemYearlyExpensesRow.visibility = VISIBLE
                }

                fun hideExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = GONE
                    binding.listItemSubtitle2.visibility = GONE
                    detailsBinding.listItemYearlyCpmRow.visibility = GONE
                    detailsBinding.listItemYearlyExpensesRow.visibility = GONE
                }

                this.item = item

                CoroutineScope(Dispatchers.Default).launch {
                    withContext(Dispatchers.Default) {
                        viewModel.getAnnualCostPerMile(
                            this@YearlyHolder.item.year,
                            deductionType
                        )
                    }.let { cpm: Map<Int, Float>? ->
                        (context as MainActivity).runOnUiThread {
                            when (deductionType) {
                                DeductionType.NONE -> hideExpenseFields()
                                DeductionType.IRS_STD -> {
                                    showExpenseFields()

                                    detailsBinding.listItemDeductionType.text =
                                        deductionType.fullDesc

                                    detailsBinding.listItemYearlyExpenses.text =
                                        getCurrencyString(
                                            this@YearlyHolder.getExpenses(
                                                deductionType,
                                                0f
                                            )
                                        )

                                    detailsBinding.listItemYearlyCpm.text =
                                        getCpmIrsStdString(cpm)

                                    binding.listItemSubtitle2.text =
                                        getCurrencyString(
                                            this@YearlyHolder.getNet(0f, deductionType)
                                        )

                                    detailsBinding.listItemYearlyHourly.text =
                                        getCurrencyString(
                                            this@YearlyHolder.getHourly(0f)
                                        )
                                }
                                else -> {
                                    showExpenseFields()

                                    val costPerMile: Float = cpm?.get(12) ?: 0f

                                    detailsBinding.listItemDeductionType.text =
                                        deductionType.fullDesc

                                    detailsBinding.listItemYearlyExpenses.text =
                                        getCurrencyString(
                                            this@YearlyHolder.getExpenses(
                                                deductionType,
                                                costPerMile
                                            )
                                        )

                                    detailsBinding.listItemYearlyCpm.text =
                                        getCpmString(costPerMile)

                                    binding.listItemSubtitle2.text =
                                        getCurrencyString(
                                            this@YearlyHolder.getNet(
                                                costPerMile,
                                                deductionType
                                            )
                                        )

                                    detailsBinding.listItemYearlyHourly.text =
                                        getCurrencyString(
                                            this@YearlyHolder.getHourly(costPerMile)
                                        )
                                }
                            }
                        }
                    }
                }

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

                setPayloadVisibility(payloads)
            }

            private fun getExpenses(deductionType: DeductionType, costPerMile: Float = 0f) =
                when (deductionType) {
                    DeductionType.IRS_STD -> getStandardDeductionExpense()
                    else -> getCalculatedExpenses(costPerMile)
                }

            private fun getCalculatedExpenses(costPerMile: Float): Float = item.mileage * costPerMile

            private fun getStandardDeductionExpense(): Float {
                val table = viewModel.standardMileageDeductionTable()
                var res = 0f
                item.monthlies.keys.forEach { mon ->
                    val deduction: Float = table[LocalDate.of(item.year, mon, 1)]
                    res += deduction * (item.monthlies[mon]?.mileage ?: 0f)
                }
                return res
            }

            private fun getNet(cpm: Float, deductionType: DeductionType): Float =
                item.totalPay - getExpenses(deductionType, cpm)

            private fun getHourly(cpm: Float): Float = getNet(cpm, deductionType) / item.hours
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
