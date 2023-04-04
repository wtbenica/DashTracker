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
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.WeeklyViewModel
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_weekly.getDateRange
import com.wtb.dashTracker.ui.fragment_income.IncomeListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class WeeklyListFragment :
    IncomeListItemFragment<FullWeekly, Pair<Float, Float>, WeeklyListFragment.FullWeeklyAdapter.WeeklyHolder>() {

    override val entryAdapter: FullWeeklyAdapter = FullWeeklyAdapter().apply {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                // TODO: Is this on purpose? is it that when loading it takes list to the end? check it out one day
                binding.itemListRecyclerView.scrollToPosition(positionStart)
            }
        })
    }

    private val viewModel: WeeklyViewModel by viewModels()

    // TODO: Switch to notify items changes and check payloads
    @SuppressLint("NotifyDataSetChanged")
    private val sharedPrefsListener = OnSharedPreferenceChangeListener { sharedPrefs, key ->
        if (key == requireContext().PREF_SHOW_BASE_PAY_ADJUSTS) {
            val recyclerView = binding.itemListRecyclerView
            val myLayoutManager = recyclerView.layoutManager
            recyclerView.adapter = null
            recyclerView.layoutManager = null
            recyclerView.adapter = entryAdapter
            recyclerView.layoutManager = myLayoutManager
            entryAdapter.notifyDataSetChanged()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWeekliesPaged.collectLatest {
                    entryAdapter.submitData(it)
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

    @ExperimentalAnimationApi
    inner class FullWeeklyAdapter :
        IncomeItemPagingDataAdapter<FullWeekly, Pair<Float, Float>, FullWeeklyAdapter.WeeklyHolder>(
            DIFF_CALLBACK
        ) {
        override fun getViewHolder(parent: ViewGroup, viewType: Int?): WeeklyHolder =
            WeeklyHolder(parent)

        inner class WeeklyHolder(parent: ViewGroup) :
            IncomeItemHolder<FullWeekly, Pair<Float, Float>>(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_weekly, parent, false)
            ) {

            // BaseItemHolder Overrides
            override val collapseArea: Array<View>
                get() = arrayOf(binding.listItemDetails)

            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper

            override val bgCard: CardView
                get() = binding.root

            override val parentFrag: ListItemFragment
                get() = this@WeeklyListFragment

            // IncomeItemHolder Overrides
            override var expenseValues: Pair<Float, Float> = Pair(0f, 0f)

            override val holderDeductionTypeFlow: StateFlow<DeductionType>
                get() = incomeDeductionTypeFlow

            private val binding: ListItemWeeklyBinding = ListItemWeeklyBinding.bind(itemView)

            private val detailsBinding: ListItemWeeklyDetailsTableBinding =
                ListItemWeeklyDetailsTableBinding.bind(itemView)

            private val showBPAs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(requireContext().PREF_SHOW_BASE_PAY_ADJUSTS, true)

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        WeeklyDialog.newInstance(this@WeeklyHolder.mItem.weekly.date)
                            .show(childFragmentManager, "edit_weekly_details")
                    }
                }
            }

            // BaseItemHolder Overrides
            override fun updateHeaderFields() {
                if (this.mItem.isEmpty) {
                    viewModel.delete(mItem.weekly)
                } else {
                    binding.listItemBtnEdit.isVisible = showBPAs

                    binding.listItemTitle.text =
                        getDateRange(
                            start = this.mItem.weekly.date.minusDays(6),
                            end = this.mItem.weekly.date
                        )

                    binding.listItemAlert.setVisibleIfTrue(showBPAs && this.mItem.weekly.isIncomplete)

                    binding.listItemTitle2.text =
                        getCurrencyString(
                            if (this.mItem.totalPay != 0f) {
                                this.mItem.totalPay
                            } else {
                                null
                            }
                        )
                    binding.listItemSubtitle.text =
                        getString(R.string.week_number, this.mItem.weekly.date.weekOfYear)
                }
            }

            override fun updateDetailsFields() {
                val basePayAdjust = this.mItem.weekly.basePayAdjustment

                detailsBinding.listItemWeeklyAdjust.text =
                    getCurrencyString(basePayAdjust)
                detailsBinding.listItemRegularPay.text = getCurrencyString(this.mItem.regularPay)
                detailsBinding.listItemWeeklyAdjust.text =
                    getCurrencyString(basePayAdjust)
                detailsBinding.listItemWeeklyAdjust.setTextColor(
                    if (this.mItem.weekly.basePayAdjustment == null) {
                        requireContext().getAttrColor(R.attr.colorAlert)
                    } else {
                        requireContext().getAttrColor(R.attr.colorTextPrimary)
                    }
                )

                detailsBinding.listItemWeeklyAdjust.setVisibleIfTrue(showBPAs)
                detailsBinding.labelBasePayAdjust.setVisibleIfTrue(showBPAs)
                detailsBinding.listItemCashTips.text = getCurrencyString(this.mItem.cashTips)
                detailsBinding.listItemOtherPay.text = getCurrencyString(this.mItem.otherPay)
                detailsBinding.listItemWeeklyHours.text = getFloatString(this.mItem.hours)
                detailsBinding.listItemWeeklyHourly.text = getCurrencyString(this.mItem.hourly)
                detailsBinding.listItemWeeklyAvgDel.text = getCurrencyString(this.mItem.avgDelivery)
                detailsBinding.listItemWeeklyHourlyDels.text = getFloatString(this.mItem.delPerHour)
                detailsBinding.listItemWeeklyMiles.text =
                    getStringOrElse(R.string.odometer_fmt, "-", this.mItem.miles)
            }

            // IncomeItemHolder Overrides
            override suspend fun getExpenseValues(deductionType: DeductionType): Pair<Float, Float> =
                viewModel.getExpensesAndCostPerMile(
                    this@WeeklyHolder.mItem,
                    deductionType
                )

            override fun updateExpenseFieldVisibilities() {
                val shouldShow = deductionType != DeductionType.NONE
                binding.listItemSubtitle2Label.fade(shouldShow)
                binding.listItemSubtitle2.fade(shouldShow)
                detailsBinding.listItemWeeklyCpmHeader.showOrHide(shouldShow)
                detailsBinding.listItemWeeklyCpmDeductionType.showOrHide(shouldShow)
                detailsBinding.listItemWeeklyCpm.showOrHide(shouldShow)
                detailsBinding.listItemWeeklyExpensesHeader.showOrHide(shouldShow)
                detailsBinding.listItemWeeklyExpenses.showOrHide(shouldShow)
            }

            override fun updateExpenseFieldValues() {
                val (expenses, cpm) = expenseValues

                binding.listItemSubtitle2.text =
                    getCurrencyString(this@WeeklyHolder.mItem.getNet(cpm))

                detailsBinding.listItemWeeklyCpmDeductionType.text =
                    deductionType.fullDesc

                detailsBinding.listItemWeeklyCpm.text = formatCpm(cpm)

                detailsBinding.listItemWeeklyExpenses.text =
                    getCurrencyString(expenses)

                detailsBinding.listItemWeeklyHourly.text = getCurrencyString(
                    this@WeeklyHolder.mItem.getHourly(cpm)
                )

                detailsBinding.listItemWeeklyAvgDel.text = getCurrencyString(
                    this@WeeklyHolder.mItem.getAvgDelivery(cpm)
                )
            }
        }
    }

    interface WeeklyListFragmentCallback : DeductionCallback

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
