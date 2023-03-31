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

package com.wtb.dashTracker.ui.fragment_income.fragment_weeklies

import android.annotation.SuppressLint
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ListItemWeeklyBinding
import com.wtb.dashTracker.databinding.ListItemWeeklyDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.DeductionCallback
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyViewModel
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.getDateRange
import com.wtb.dashTracker.ui.fragment_income.IncomeListItemFragment
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class WeeklyListFragment : IncomeListItemFragment() {
    private val viewModel: WeeklyViewModel by viewModels()

    private val fullWeeklyAdapter = FullWeeklyAdapter().apply {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.itemListRecyclerView.scrollToPosition(positionStart)
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.itemListRecyclerView.adapter = fullWeeklyAdapter

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                callback?.deductionType?.collectLatest {
                    deductionType = it
                    fullWeeklyAdapter.notifyDataSetChanged()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWeekliesPaged.collectLatest {
                    fullWeeklyAdapter.submitData(it)
                }
            }
        }
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
    inner class FullWeeklyAdapter : BaseItemPagingDataAdapter<FullWeekly, FullWeeklyAdapter.WeeklyHolder>(DIFF_CALLBACK) {
        override fun getViewHolder(parent: ViewGroup, viewType: Int?): WeeklyHolder =
            WeeklyHolder(parent)

        @ExperimentalAnimationApi
        inner class WeeklyHolder(parent: ViewGroup) : BaseItemHolder<FullWeekly>(
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_weekly, parent, false)
        ) {
            private val binding: ListItemWeeklyBinding = ListItemWeeklyBinding.bind(itemView)

            private val detailsBinding: ListItemWeeklyDetailsTableBinding =
                ListItemWeeklyDetailsTableBinding.bind(itemView)

            override val collapseArea: Array<View>
                get() = arrayOf(binding.listItemDetails)
            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper
            override val bgCard: CardView
                get() = binding.root

            private val showBPAs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(requireContext().PREF_SHOW_BASE_PAY_ADJUSTS, true)

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        WeeklyDialog.newInstance(this@WeeklyHolder.item.weekly.date)
                            .show(childFragmentManager, "edit_weekly_details")
                    }
                }
            }

            override fun updateHeaderFields() {
                if (this.item.isEmpty) {
                    viewModel.delete(item.weekly)
                } else {
                    binding.listItemBtnEdit.isVisible = showBPAs

                    binding.listItemTitle.text =
                        getDateRange(
                            start = this.item.weekly.date.minusDays(6),
                            end = this.item.weekly.date
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
                }
            }

            override fun updateDetailsFields() {
                val basePayAdjust = this.item.weekly.basePayAdjustment
                detailsBinding.listItemWeeklyAdjust.text =
                    getCurrencyString(basePayAdjust)
                detailsBinding.listItemRegularPay.text = getCurrencyString(this.item.regularPay)
                detailsBinding.listItemWeeklyAdjust.text =
                    getCurrencyString(basePayAdjust)
                detailsBinding.listItemWeeklyAdjust.setTextColor(
                    if (this.item.weekly.basePayAdjustment == null) {
                        requireContext().getAttrColor(R.attr.colorAlert)
                    } else {
                        requireContext().getAttrColor(R.attr.colorTextPrimary)
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
            }

            override fun launchObservers() {
                fun updateExpenseFieldsVisibility(show: Boolean) {
                    binding.listItemSubtitle2Label.fade(show)
                    binding.listItemSubtitle2.fade(show)
                    detailsBinding.listItemWeeklyCpmHeader.showOrHide(show, isExpanded)
                    detailsBinding.listItemWeeklyCpmDeductionType.showOrHide(show, isExpanded)
                    detailsBinding.listItemWeeklyCpm.showOrHide(show, isExpanded)
                    detailsBinding.listItemWeeklyExpensesHeader.showOrHide(show, isExpanded)
                    detailsBinding.listItemWeeklyExpenses.showOrHide(show, isExpanded)
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
                                DeductionType.NONE -> updateExpenseFieldsVisibility(false)
                                else -> {
                                    updateExpenseFieldsVisibility(true)

                                    detailsBinding.listItemWeeklyCpmDeductionType.text =
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
