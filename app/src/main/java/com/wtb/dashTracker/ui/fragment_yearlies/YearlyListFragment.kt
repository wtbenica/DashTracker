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

package com.wtb.dashTracker.ui.fragment_yearlies

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.DeductionCallback
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.databinding.ListItemYearlyBinding
import com.wtb.dashTracker.databinding.ListItemYearlyDetailsTableBinding
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getMileageString
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@ExperimentalCoroutinesApi
class YearlyListFragment : Fragment() {

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
    ) {
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
                viewModel.allWeeklies.collectLatest { cwList: List<CompleteWeekly> ->
                    // TODO: Something tells me this could be done better with sql
                    yearlies.clear()
                    var numChecked = 0
                    var year =
                        cwList.map { it.weekly.date.year }.maxOrNull() ?: LocalDate.now().year + 1
                    while (numChecked < cwList.size) {
                        val thisYears = cwList.mapNotNull { cw: CompleteWeekly ->
                            if (cw.weekly.date.year == year) cw else null
                        }
                        numChecked += thisYears.size
                        val res = Yearly(year)
                        thisYears.forEach { cw: CompleteWeekly ->
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
                        year -= 1
                    }
                    entryAdapter.submitList(yearlies)
                }
            }
        }
    }

    interface YearlyListFragmentCallback : DeductionCallback

    inner class YearlyAdapter : ListAdapter<Yearly, YearlyHolder>(DIFF_CALLBACK) {
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

    inner class YearlyHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_yearly, parent, false)
    ),
        View.OnClickListener {
        private val binding: ListItemYearlyBinding = ListItemYearlyBinding.bind(itemView)
        private val detailsBinding: ListItemYearlyDetailsTableBinding =
            ListItemYearlyDetailsTableBinding.bind(itemView)
        private lateinit var yearly: Yearly

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val currentVisibility = binding.listItemDetails.visibility
            binding.listItemDetails.visibility = if (currentVisibility == VISIBLE) GONE else VISIBLE
            binding.listItemWrapper.setBackgroundResource(
                if (currentVisibility == VISIBLE) R.drawable.bg_list_item else R.drawable.bg_list_item_expanded
            )
            bindingAdapter?.notifyItemChanged(
                bindingAdapterPosition,
                binding.listItemDetails.visibility
            )
        }

        fun bind(item: Yearly, payloads: MutableList<Any>? = null) {
            fun showExpenseFields() {
                binding.listItemSubtitle2Label.visibility = VISIBLE
                binding.listItemSubtitle2.visibility = VISIBLE
                detailsBinding.listItemYearlyCpmRow.visibility = VISIBLE
                detailsBinding.listItemYearlyNetRow.visibility = VISIBLE
            }

            fun hideExpenseFields() {
                binding.listItemSubtitle2Label.visibility = GONE
                binding.listItemSubtitle2.visibility = GONE
                detailsBinding.listItemYearlyCpmRow.visibility = GONE
                detailsBinding.listItemYearlyNetRow.visibility = GONE
            }

            this.yearly = item

            CoroutineScope(Dispatchers.Default).launch {
                withContext(Dispatchers.Default) {
                    viewModel.getAnnualCostPerMile(yearly.year, deductionType)
                }.let { cpm: Float? ->
                    val costPerMile: Float = cpm ?: 0f
                    (context as MainActivity).runOnUiThread {
                        when (deductionType) {
                            DeductionType.NONE -> hideExpenseFields()
                            else -> {
                                showExpenseFields()

                                binding.listItemSubtitle2Label.text = deductionType.fullDesc

                                binding.listItemSubtitle2.text =
                                    getCurrencyString(yearly.getExpenses(costPerMile))

                                detailsBinding.listItemYearlyCpm.text = getCurrencyString(costPerMile)

                                detailsBinding.listItemYearlyNet.text =
                                    getCurrencyString(this@YearlyHolder.yearly.getNet(costPerMile))

                                detailsBinding.listItemYearlyHourly.text = getCurrencyString(
                                    this@YearlyHolder.yearly.getHourly(costPerMile)
                                )
                            }
                        }
                    }
                }
            }

            val listItemDetailsVisibility = (payloads?.let {
                if (it.size == 1 && it[0] in listOf(
                        VISIBLE,
                        GONE
                    )
                ) it[0] else null
            } ?: GONE) as Int

            binding.listItemWrapper.setBackgroundResource(
                if (listItemDetailsVisibility == VISIBLE)
                    R.drawable.bg_list_item_expanded
                else
                    R.drawable.bg_list_item
            )

            binding.listItemTitle.text = this.yearly.year.toString()
            binding.listItemTitle2.text = getCurrencyString(this.yearly.totalPay)
            detailsBinding.listItemReportedIncome.text = getCurrencyString(yearly.reportedPay)
            detailsBinding.listItemCashTips.text = getCurrencyString(yearly.cashTips)
            detailsBinding.listItemYearlyMileage.text = getMileageString(yearly.mileage)
            detailsBinding.listItemYearlyHours.text = getString(R.string.format_hours, yearly.hours)
            detailsBinding.listItemYearlyHourly.text = getCurrencyString(yearly.hourly)
            binding.listItemDetails.visibility = listItemDetailsVisibility
        }
    }

    companion object {
        private const val TAG = APP + "MainFragment"

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
