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
import android.util.Log
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
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.databinding.ListItemYearlyBinding
import com.wtb.dashTracker.databinding.ListItemYearlyDetailsTableBinding
import com.wtb.dashTracker.extensions.getCpmString
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getStringOrElse
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.DeductionCallback
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.BaseItemHolder
import com.wtb.dashTracker.ui.fragment_list_item_base.BaseItemListAdapter
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.Month

@ExperimentalCoroutinesApi
class YearlyListFragment : ListItemFragment() {

    protected val viewModel: YearlyListViewModel by viewModels()
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
                viewModel.yearlyBasePayAdjustments.collectLatest { bpas: MutableMap<Int, Float> ->
                    bpaList = bpas
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allEntries.collectLatest { entries: List<DashEntry> ->
                    // TODO: Something tells me this could be done better with sql
                    yearlies.clear()
                    var numChecked = 0
                    // TODO: Originally, I added 1 to the default year, and I'm not sure why. I took
                    //  it out, and I added a 'isNotEmpty' check for thisYears. If there are issues,
                    //  this is where to look
                    var year =
                        entries.map { it.date.year }.maxOrNull() ?: LocalDate.now().year
                    Log.d(TAG, "New Entries coming in to yearly list fragment")
                    while (numChecked < entries.size) {
                        val thisYears = entries.mapNotNull { entry: DashEntry ->
                            if (entry.date.year == year) entry else null
                        }
                        numChecked += thisYears.size
                        val res = Yearly(year).apply {
                            basePayAdjustment = bpaList[year] ?: 0f
                        }
                        if (thisYears.isNotEmpty()) {
                            thisYears.forEach { entry: DashEntry ->
                                res.addEntry(entry)
                            }
                            yearlies.add(res)
                        }
                        year -= 1
                    }
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
                    }.let { cpm: Float? ->
                        val costPerMile: Float = cpm ?: 0f
                        (context as MainActivity).runOnUiThread {
                            when (deductionType) {
                                DeductionType.NONE -> hideExpenseFields()
                                else -> {
                                    showExpenseFields()

                                    detailsBinding.listItemDeductionType.text =
                                        deductionType.fullDesc

                                    detailsBinding.listItemYearlyExpenses.text =
                                        getCurrencyString(
                                            this@YearlyHolder.item.getExpenses(
                                                deductionType,
                                                costPerMile
                                            )
                                        )

                                    detailsBinding.listItemYearlyCpm.text =
                                        getCpmString(costPerMile)

                                    binding.listItemSubtitle2.text =
                                        getCurrencyString(
                                            this@YearlyHolder.item.getNet(
                                                costPerMile,
                                                deductionType
                                            )
                                        )

                                    detailsBinding.listItemYearlyHourly.text =
                                        getCurrencyString(
                                            this@YearlyHolder.item.getHourly(costPerMile)
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
        }
    }

    data class Monthly(
        var mileage: Float = 0f,
        var pay: Float = 0f,
        var otherPay: Float = 0f,
        var cashTips: Float = 0f,
        var hours: Float = 0f
    ) : ListItemType {
        val reportedPay: Float
            get() = pay + otherPay

        internal val totalPay: Float
            get() = reportedPay + cashTips

        val hourly: Float
            get() = totalPay / hours

        fun getExpenses(costPerMile: Float): Float = mileage * costPerMile

        fun getNet(cpm: Float): Float = totalPay - getExpenses(cpm)

        fun getHourly(cpm: Float): Float = getNet(cpm) / hours

        fun addEntry(entry: DashEntry) {
            mileage += entry.totalMileage ?: 0f
            pay += entry.pay ?: 0f
            otherPay += entry.otherPay ?: 0f
            cashTips += entry.cashTips ?: 0f
            hours += entry.totalHours ?: 0f
        }
    }

    inner class Yearly(val year: Int) : ListItemType {
        val monthlies = mutableMapOf<Month, Monthly>().apply {
            Month.values().forEach { this[it] = Monthly() }
        }

        operator fun get(month: Month): Monthly? = monthlies[month]

        var basePayAdjustment: Float = 0f

        // TODO: Need to still add in bpa
        val reportedPay: Float
            get() = monthlies.values.fold(0f) { acc, monthly -> acc + monthly.reportedPay }

        val cashTips: Float
            get() = monthlies.values.fold(0f) { acc, monthly -> acc + monthly.cashTips }

        val hourly: Float
            get() = totalPay / hours

        internal val totalPay: Float
            get() = monthlies.values.fold(0f) { acc, monthly -> acc + monthly.totalPay }

        val hours: Float
            get() = monthlies.values.fold(0f) { acc, monthly -> acc + monthly.hours }

        val mileage: Float
            get() = monthlies.values.fold(0f) { acc, monthly -> acc + monthly.mileage }

        fun getExpenses(deductionType: DeductionType, costPerMile: Float = 0f) =
            when (deductionType) {
                DeductionType.IRS_STD -> getStandardDeductionExpense()
                else -> getCalculatedExpenses(costPerMile)
            }

        fun getCalculatedExpenses(costPerMile: Float): Float = mileage * costPerMile

        fun getStandardDeductionExpense(): Float {
            val table = viewModel.standardMileageDeductionTable()
            var res = 0f
            monthlies.keys.forEach { mon ->
                val deduction: Float = table[LocalDate.of(year, mon, 1)]
                res += deduction * (monthlies[mon]?.mileage ?: 0f)
            }
            return res
        }

        fun getNet(cpm: Float, deductionType: DeductionType): Float =
            totalPay - getExpenses(deductionType, cpm)

        fun getHourly(cpm: Float): Float = getNet(cpm, deductionType) / hours

        fun addEntry(entry: DashEntry) {
            monthlies[entry.date.month]?.addEntry(entry)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Yearly

            if (year != other.year) return false

            return true
        }

        override fun hashCode(): Int {
            return year
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
