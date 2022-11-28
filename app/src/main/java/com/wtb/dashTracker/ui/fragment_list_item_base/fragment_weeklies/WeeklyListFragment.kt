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

package com.wtb.dashTracker.ui.fragment_list_item_base.fragment_weeklies

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.FragItemListBinding
import com.wtb.dashTracker.databinding.ListItemWeeklyBinding
import com.wtb.dashTracker.databinding.ListItemWeeklyDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.DeductionCallback
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyViewModel
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.BaseItemHolder
import com.wtb.dashTracker.ui.fragment_list_item_base.BaseItemPagingDataAdapter
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class WeeklyListFragment : ListItemFragment() {
    private val viewModel: WeeklyViewModel by viewModels()
    private var callback: IncomeFragment.IncomeFragmentCallback? = null

    private lateinit var binding: FragItemListBinding
    private var deductionType: DeductionType = DeductionType.NONE
    private val fullWeeklyAdapter = FullWeeklyAdapter().apply {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.itemListRecyclerView.scrollToPosition(positionStart)
            }
        })
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as IncomeFragment.IncomeFragmentCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragItemListBinding.inflate(inflater)
        binding.itemListRecyclerView.layoutManager = LinearLayoutManager(context)

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.itemListRecyclerView.adapter = fullWeeklyAdapter

        callback?.deductionType?.asLiveData()?.observe(viewLifecycleOwner) {
            deductionType = it
            fullWeeklyAdapter.notifyDataSetChanged()
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWeekliesPaged.collectLatest {
                    fullWeeklyAdapter.submitData(it)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onResume() {
        super.onResume()

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(sharedPrefsListener)
    }

    override fun onPause() {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)

        super.onPause()
    }

    @SuppressLint("NotifyDataSetChanged")
    private val sharedPrefsListener = OnSharedPreferenceChangeListener { sharedPrefs, key ->
        if (key == requireContext().PREF_SHOW_BASE_PAY_ADJUSTS) {
            val recyclerView = binding.itemListRecyclerView
            val myLayoutManager = recyclerView.layoutManager
            recyclerView.adapter = null
            recyclerView.layoutManager = null
            recyclerView.adapter = fullWeeklyAdapter
            recyclerView.layoutManager = myLayoutManager
            fullWeeklyAdapter.notifyDataSetChanged()
        }
    }

    interface WeeklyListFragmentCallback : DeductionCallback

    @ExperimentalAnimationApi
    inner class FullWeeklyAdapter : BaseItemPagingDataAdapter<FullWeekly>(DIFF_CALLBACK) {
        override fun getViewHolder(parent: ViewGroup, viewType: Int?): BaseItemHolder<FullWeekly> =
            WeeklyHolder(parent)

        @ExperimentalAnimationApi
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

            private val showBPAs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(requireContext().PREF_SHOW_BASE_PAY_ADJUSTS, true)

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        WeeklyDialog.newInstance(this@WeeklyHolder.item.weekly.date)
                            .show(parentFragmentManager, "edit_weekly_details")
                    }
                }
            }

            override fun bind(item: FullWeekly, payloads: List<Any>?) {
                fun showExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = VISIBLE
                    binding.listItemSubtitle2.visibility = VISIBLE
                    detailsBinding.listItemWeeklyCpmRow.visibility = VISIBLE
                    detailsBinding.listItemWeeklyExpensesRow.visibility = VISIBLE
                }

                fun hideExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = GONE
                    binding.listItemSubtitle2.visibility = GONE
                    detailsBinding.listItemWeeklyCpmRow.visibility = GONE
                    detailsBinding.listItemWeeklyExpensesRow.visibility = GONE
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

                                    detailsBinding.listItemDeductionType.text =
                                        deductionType.fullDesc

                                    detailsBinding.listItemWeeklyExpenses.text =
                                        getCurrencyString(expenses)

                                    detailsBinding.listItemWeeklyCpm.text = getCpmString(cpm)

                                    binding.listItemSubtitle2.text =
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

                binding.listItemBtnEdit.isVisible = showBPAs

                binding.listItemTitle.text =
                    getStringOrElse(
                        R.string.time_range,
                        "",
                        this.item.weekly.date.minusDays(6).shortFormat.uppercase(),
                        this.item.weekly.date.shortFormat.uppercase()
                    )

                binding.listItemAlert.setVisibleIfTrue(showBPAs && this.item.weekly.isIncomplete)

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



                detailsBinding.listItemWeeklyAdjust.setVisibleIfTrue(showBPAs)
                detailsBinding.labelBasePayAdjust.setVisibleIfTrue(showBPAs)
                detailsBinding.listItemCashTips.text = getCurrencyString(this.item.cashTips)
                detailsBinding.listItemOtherPay.text = getCurrencyString(this.item.otherPay)
                detailsBinding.listItemWeeklyHours.text = getFloatString(this.item.hours)
                detailsBinding.listItemWeeklyHourly.text = getCurrencyString(this.item.hourly)
                detailsBinding.listItemWeeklyAvgDel.text = getCurrencyString(this.item.avgDelivery)
                detailsBinding.listItemWeeklyHourlyDels.text = getFloatString(this.item.delPerHour)
                detailsBinding.listItemWeeklyMiles.text =
                    getStringOrElse(R.string.odometer_fmt, "-", this.item.miles)

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
