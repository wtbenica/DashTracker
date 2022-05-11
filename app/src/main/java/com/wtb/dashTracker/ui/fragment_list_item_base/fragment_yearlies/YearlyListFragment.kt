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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ListItemYearlyBinding
import com.wtb.dashTracker.databinding.ListItemYearlyDetailsTableBinding
import com.wtb.dashTracker.extensions.getCpmString
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getMileageString
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.DeductionCallback
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.BaseItemHolder
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
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

    data class Yearly(
        val year: Int,
        var mileage: Float = 0f,
        var pay: Float = 0f,
        var otherPay: Float = 0f,
        var cashTips: Float = 0f,
        var adjust: Float = 0f,
        var hours: Float = 0f
    ) : ListItemType {
        val reportedPay: Float
            get() = pay + otherPay + adjust

        val hourly: Float
            get() = (reportedPay + cashTips) / hours

        internal val totalPay: Float
            get() = pay + otherPay + adjust + cashTips

        fun getExpenses(costPerMile: Float): Float = mileage * costPerMile

        fun getNet(cpm: Float): Float = totalPay - getExpenses(cpm)

        fun getHourly(cpm: Float): Float = getNet(cpm) / hours
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
                viewModel.allWeeklies.collectLatest { cwList: List<FullWeekly> ->
                    // TODO: Something tells me this could be done better with sql
                    yearlies.clear()
                    var numChecked = 0
                    // TODO: Originally, I added 1 to the default year, and I'm not sure why. I took
                    //  it out, and I added a 'isNotEmpty' check for thisYears. If there are issues,
                    //  this is where to look
                    var year =
                        cwList.map { it.weekly.date.year }.maxOrNull() ?: LocalDate.now().year
                    while (numChecked < cwList.size) {
                        val thisYears = cwList.mapNotNull { cw: FullWeekly ->
                            if (cw.weekly.date.year == year) cw else null
                        }
                        numChecked += thisYears.size
                        val res = Yearly(year)
                        if (thisYears.isNotEmpty()) {
                            thisYears.forEach { cw: FullWeekly ->
                                res.adjust += cw.weekly.basePayAdjustment ?: 0f
                                res.pay += cw.entries.mapNotNull(DashEntry::pay)
                                    .reduceOrNull { acc, fl -> acc + fl } ?: 0f
                                res.otherPay += cw.entries.mapNotNull(DashEntry::otherPay)
                                    .reduceOrNull { acc, fl -> acc + fl } ?: 0f
                                res.cashTips += cw.entries.mapNotNull(DashEntry::cashTips)
                                    .reduceOrNull { acc, fl -> acc + fl } ?: 0f
                                res.mileage += cw.entries.mapNotNull(DashEntry::mileage)
                                    .reduceOrNull { acc, fl -> acc + fl } ?: 0f
                                res.hours += cw.entries.mapNotNull(DashEntry::totalHours)
                                    .reduceOrNull { acc, fl -> acc + fl } ?: 0f
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

    interface YearlyListFragmentCallback : DeductionCallback

    inner class YearlyAdapter :
        ListAdapter<YearlyListFragment.Yearly, YearlyHolder>(DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearlyHolder =
            YearlyHolder(parent)

        override fun onBindViewHolder(holder: YearlyHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onBindViewHolder(
            holder: YearlyHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            super.onBindViewHolder(holder, position, payloads)
            holder.bind(getItem(position), payloads)
        }
    }

    inner class YearlyHolder(parent: ViewGroup) : BaseItemHolder<Yearly>(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_yearly, parent, false)
    ) {
        private val binding: ListItemYearlyBinding = ListItemYearlyBinding.bind(itemView)
        private val detailsBinding: ListItemYearlyDetailsTableBinding =
            ListItemYearlyDetailsTableBinding.bind(itemView)

        override val collapseArea: ViewGroup
            get() = binding.listItemDetails
        override val backgroundArea: ViewGroup
            get() = binding.listItemWrapper

        override fun bind(item: Yearly, payloads: MutableList<Any>?) {
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
                    viewModel.getAnnualCostPerMile(this@YearlyHolder.item.year, deductionType)
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
                                            costPerMile
                                        )
                                    )

                                detailsBinding.listItemYearlyCpm.text =
                                    getCpmString(costPerMile)

                                binding.listItemSubtitle2.text =
                                    getCurrencyString(this@YearlyHolder.item.getNet(costPerMile))

                                detailsBinding.listItemYearlyHourly.text = getCurrencyString(
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
            detailsBinding.listItemYearlyMileage.text = getMileageString(this.item.mileage)
            detailsBinding.listItemYearlyHours.text =
                getString(R.string.format_hours, item.hours)
            detailsBinding.listItemYearlyHourly.text = getCurrencyString(this.item.hourly)

            setPayloadVisibility(payloads)
        }
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Yearly>() {
            override fun areItemsTheSame(oldItem: Yearly, newItem: Yearly): Boolean =
                oldItem.year == newItem.year


            override fun areContentsTheSame(
                oldItem: Yearly,
                newItem: Yearly
            ): Boolean =
                oldItem == newItem
        }
    }
}
