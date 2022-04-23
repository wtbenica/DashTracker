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

package com.wtb.dashTracker.ui.fragment_weeklies

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.DeductionCallback
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ListItemWeeklyBinding
import com.wtb.dashTracker.databinding.ListItemWeeklyDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyViewModel
import com.wtb.dashTracker.ui.fragment_base_list.BaseItemAdapter
import com.wtb.dashTracker.ui.fragment_base_list.BaseItemHolder
import com.wtb.dashTracker.ui.fragment_dailies.EntryListFragment.Companion.toVisibleIfTrueElseGone
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

@ExperimentalCoroutinesApi
class WeeklyListFragment : Fragment() {

    private val viewModel: WeeklyViewModel by viewModels()
    private var callback: IncomeFragment.IncomeFragmentCallback? = null

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

        val entryAdapter = EntryAdapter().apply {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    recyclerView.scrollToPosition(positionStart)
                }
            })
        }
        recyclerView.adapter = entryAdapter

        callback?.deductionType?.asLiveData()?.observe(viewLifecycleOwner) {
            deductionType = it
            entryAdapter.notifyDataSetChanged()
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWeekliesPaged.collectLatest {
                    entryAdapter.submitData(it)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    interface WeeklyListFragmentCallback : DeductionCallback

    inner class EntryAdapter : BaseItemAdapter<FullWeekly>(DIFF_CALLBACK) {
        override fun getViewHolder(parent: ViewGroup, viewType: Int?): BaseItemHolder<FullWeekly> =
            WeeklyHolder(parent)

        inner class WeeklyHolder(parent: ViewGroup) : BaseItemHolder<FullWeekly>(
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_weekly, parent, false)
        ) {
            private val binding: ListItemWeeklyBinding = ListItemWeeklyBinding.bind(itemView)
            private val detailsBinding: ListItemWeeklyDetailsTableBinding =
                ListItemWeeklyDetailsTableBinding.bind(itemView)

            override val collapseArea: ConstraintLayout
                get() = binding.listItemDetails
            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper

            init {
                itemView.findViewById<ImageButton>(R.id.list_item_btn_edit).apply {
                    setOnClickListener {
                        WeeklyDialog.newInstance(this@WeeklyHolder.item.weekly.date)
                            .show(parentFragmentManager, "edit_details")
                    }
                }
            }

            override fun bind(item: FullWeekly, payloads: MutableList<Any>?) {
                fun showExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = VISIBLE
                    binding.listItemSubtitle2.visibility = VISIBLE
                    detailsBinding.listItemWeeklyCpmRow.visibility = VISIBLE
                    detailsBinding.listItemWeeklyNetRow.visibility = VISIBLE
                }

                fun hideExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = GONE
                    binding.listItemSubtitle2.visibility = GONE
                    detailsBinding.listItemWeeklyCpmRow.visibility = GONE
                    detailsBinding.listItemWeeklyNetRow.visibility = GONE
                }

                this.item = item

                if (this.item.isEmpty) {
                    viewModel.delete(item.weekly)
                }

                CoroutineScope(Dispatchers.Default).launch {
                    withContext(Dispatchers.Default) {
                        viewModel.getExpensesAndCostPerMile(
                            this@WeeklyHolder.item,
                            deductionType
                        )
                    }.let { (expenses, cpm) ->
                        (context as MainActivity).runOnUiThread {
                            when (deductionType) {
                                DeductionType.NONE -> hideExpenseFields()
                                else -> {
                                    showExpenseFields()

                                    binding.listItemSubtitle2Label.text = deductionType.fullDesc

                                    binding.listItemSubtitle2.text = getCurrencyString(expenses)

                                    detailsBinding.listItemWeeklyCpm.text = getCpmString(cpm)

                                    detailsBinding.listItemWeeklyNet.text =
                                        getCurrencyString(this@WeeklyHolder.item.getNet(cpm))

                                    detailsBinding.listItemWeeklyHourly.text = getCurrencyString(
                                        this@WeeklyHolder.item.getHourly(cpm)
                                    )

                                    detailsBinding.listItemWeeklyAvgDel.text = getCurrencyString(
                                        this@WeeklyHolder.item.getAvgDelivery(cpm)
                                    )
                                }
                            }
                        }
                    }

                }

                binding.listItemTitle.text =
                    getStringOrElse(
                        R.string.time_range,
                        "",
                        this.item.weekly.date.minusDays(6).shortFormat.uppercase(),
                        this.item.weekly.date.shortFormat.uppercase()
                    )
                binding.listItemAlert.visibility =
                    toVisibleIfTrueElseGone(this.item.weekly.isIncomplete)
                binding.listItemTitle2.text =
                    getCurrencyString(
                        if (this.item.totalPay != 0f) {
                            this.item.totalPay
                        } else {
                            null
                        }
                    )
                binding.listItemSubtitle.text =
                    getString(R.string.week_number, this.item.weekly.date.weekOfYear)

                val basePayAdjust = this.item.weekly.basePayAdjustment
                detailsBinding.listItemWeeklyAdjust.text =
                    getCurrencyString(basePayAdjust)
                detailsBinding.listItemRegularPay.text = getCurrencyString(this.item.regularPay)
                detailsBinding.listItemWeeklyAdjust.text =
                    getCurrencyString(basePayAdjust)
                detailsBinding.listItemWeeklyAdjust.setTextColor(
                    if (this.item.weekly.basePayAdjustment == null) {
                        MainActivity.getAttrColor(requireContext(), R.attr.colorAlert)
                    } else {
                        MainActivity.getAttrColor(requireContext(), R.attr.colorTextPrimary)
                    }
                )


                detailsBinding.listItemCashTips.text = getCurrencyString(this.item.cashTips)
                detailsBinding.listItemOtherPay.text = getCurrencyString(this.item.otherPay)
                detailsBinding.listItemWeeklyHours.text = getFloatString(this.item.hours)
                detailsBinding.listItemWeeklyHourly.text = getCurrencyString(this.item.hourly)
                detailsBinding.listItemWeeklyAvgDel.text = getCurrencyString(this.item.avgDelivery)
                detailsBinding.listItemWeeklyHourlyDels.text = getFloatString(this.item.delPerHour)
                detailsBinding.listItemWeeklyMiles.text = getFloatString(this.item.miles)

                setPayloadVisibility(payloads)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullWeekly>() {
            override fun areItemsTheSame(
                oldItem: FullWeekly,
                newItem: FullWeekly
            ): Boolean =
                oldItem.weekly.id == newItem.weekly.id


            override fun areContentsTheSame(
                oldItem: FullWeekly,
                newItem: FullWeekly
            ): Boolean =
                oldItem.weekly == newItem.weekly
        }
    }
}
