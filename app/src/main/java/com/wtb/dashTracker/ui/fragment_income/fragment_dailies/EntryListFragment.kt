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

package com.wtb.dashTracker.ui.fragment_income.fragment_dailies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.ListItemEntryBinding
import com.wtb.dashTracker.databinding.ListItemEntryDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType.NONE
import com.wtb.dashTracker.ui.activity_main.DeductionCallback
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDialog
import com.wtb.dashTracker.ui.dialog_confirm.SimpleConfirmationDialog.Companion.ARG_EXTRA_ITEM_ID
import com.wtb.dashTracker.ui.dialog_confirm.SimpleConfirmationDialog.Companion.ARG_IS_CONFIRMED
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog.Companion.ARG_MODIFICATION_STATE
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog.Companion.ARG_MODIFIED_ID
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog.Companion.REQUEST_KEY_DATA_MODEL_DIALOG
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog.ModificationState
import com.wtb.dashTracker.ui.dialog_edit_data_model.EditDataModelDialog.ModificationState.DELETED
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.EntryDialog
import com.wtb.dashTracker.ui.fragment_income.IncomeListItemFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class EntryListFragment :
    IncomeListItemFragment<FullEntry, Float, EntryListFragment.EntryAdapter.EntryHolder>() {

    override val entryAdapter: EntryAdapter = EntryAdapter().apply {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.itemListRecyclerView.scrollToPosition(positionStart)
            }
        })
    }

    private val viewModel: EntryListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        fun setDialogListeners() {
            childFragmentManager.setFragmentResultListener(
                ConfirmDialog.DELETE.key,
                this
            ) { _, bundle ->
                val result = bundle.getBoolean(ARG_IS_CONFIRMED)
                val id = bundle.getLong(ARG_EXTRA_ITEM_ID)

                if (result) {
                    (activity as MainActivity).clearActiveEntry(id)
                    viewModel.deleteEntryById(id)
                    (requireContext() as ListItemFragmentCallback).showToolbarsAndFab()
                }
            }

            // Entry Dialog Result
            childFragmentManager.setFragmentResultListener(
                REQUEST_KEY_DATA_MODEL_DIALOG,
                this
            ) { _, bundle ->
                val modifyState = bundle.getString(ARG_MODIFICATION_STATE)
                    ?.let { ModificationState.valueOf(it) }

                val id = bundle.getLong(ARG_MODIFIED_ID, -1)

                if (modifyState == DELETED && id != -1L) {
                    (activity as MainActivity).clearActiveEntry(id)
                    viewModel.deleteEntryById(id)
                }
            }
        }

        super.onCreate(savedInstanceState)

        setDialogListeners()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullEntryList.collectLatest {
                    entryAdapter.submitData(it)
                }
            }
        }
    }

    interface EntryListFragmentCallback : DeductionCallback

    inner class EntryAdapter :
        IncomeItemPagingDataAdapter<FullEntry, Float, EntryAdapter.EntryHolder>(DIFF_CALLBACK) {

        override fun getViewHolder(parent: ViewGroup, viewType: Int?): EntryHolder =
            EntryHolder(parent)

        inner class EntryHolder(parent: ViewGroup) : IncomeItemHolder<FullEntry, Float>(
            LayoutInflater.from(context).inflate(R.layout.list_item_entry, parent, false)
        ) {
            // BaseItemHolder Overrides
            override val collapseArea: Array<View>
                get() = arrayOf(binding.listItemDetails)

            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper

            override val bgCard: CardView
                get() = binding.root

            override val parentFrag: EntryListFragment
                get() = this@EntryListFragment

            // IncomeItemHolder Overrides
            override var expenseValues: Float = 0f

            private val binding = ListItemEntryBinding.bind(itemView)

            private val detailsBinding = ListItemEntryDetailsTableBinding.bind(itemView)

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        EntryDialog.newInstance(this@EntryHolder.item.entry.entryId)
                            .show(childFragmentManager, "edit_entry_details")
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(this@EntryHolder.item.entry.entryId)
                            .show(childFragmentManager, "delete_entry")
                    }
                }
            }

            // BaseItemHolder Overrides
            override fun updateHeaderFields() {
                binding.listItemEntryDayOfWeek.text =
                    getString(
                        R.string.add_comma,
                        this.item.entry.date.dayOfWeek.name.capitalize()
                    )

                binding.listItemEntryDayOfWeek.setTextColor(
                    if (this.item.entry.date.endOfWeek == LocalDate.now().endOfWeek) {
                        requireContext().getAttrColor(R.attr.textColorListItemEntryDayThisWeek)
                    } else {
                        requireContext().getAttrColor(R.attr.textColorListItemEntryDay)
                    }
                )

                binding.listItemTitle.text = this.item.entry.date.format(dtfDate)

                binding.listItemTitle2.text = getCurrencyString(this.item.entry.totalEarned)

                binding.listItemSubtitle.apply {
                    text = getHoursRangeString(
                        start = this@EntryHolder.item.entry.startDateTime,
                        end = this@EntryHolder.item.entry.endDateTime
                    )
                    setTextColor(
                        context.getAttrColor(
                            if (this@EntryHolder.item.entry.mismatchedTimes) {
                                R.attr.colorAlert
                            } else {
                                R.attr.colorListItemSubtitle
                            }
                        )
                    )
                }
            }

            override fun updateDetailsFields() {
                fun updateMissingAlerts() {
                    binding.listItemAlert.setVisibleIfTrue(this.item.entry.isIncomplete)
                    detailsBinding.listItemAlertHours.setVisibleIfTrue(
                        this.item.entry.startTime == null ||
                                this.item.entry.endTime == null ||
                                this.item.entry.mismatchedTimes
                    )
                    detailsBinding.listItemAlertMiles.setVisibleIfTrue(
                        this.item.entry.mileage == null || this.item.entry.mismatchedOdometers
                    )
                    detailsBinding.listItemAlertDeliveries.setVisibleIfTrue(this.item.entry.numDeliveries == null)
                }

                detailsBinding.listItemRegularPay.text = getCurrencyString(this.item.entry.pay)

                detailsBinding.listItemCashTips.text =
                    getCurrencyString(this.item.entry.cashTips)

                detailsBinding.listItemOtherPay.text =
                    getCurrencyString(this.item.entry.otherPay)

                detailsBinding.listItemEntryHours.apply {
                    text =
                        getStringOrElse(
                            R.string.float_fmt,
                            "-",
                            this@EntryHolder.item.entry.totalHours
                        )
                }

                detailsBinding.listItemEntryMileageRange.apply {
                    text =
                        getOdometerRangeString(
                            this@EntryHolder.item.entry.startOdometer,
                            this@EntryHolder.item.entry.endOdometer
                        )
                    setTextColor(
                        context.getAttrColor(
                            if (this@EntryHolder.item.entry.mismatchedOdometers) {
                                R.attr.colorAlert
                            } else {
                                R.attr.colorRowValueSecondaryText
                            }
                        )
                    )
                }
                detailsBinding.listItemEntryMileage.text =
                    getStringOrElse(R.string.odometer_fmt, "-", this.item.entry.mileage)

                detailsBinding.listItemEntryNumDeliveries.text =
                    "${this.item.entry.numDeliveries ?: "-"}"
                detailsBinding.listItemEntryHourly.text =
                    getCurrencyString(this.item.entry.hourly)
                detailsBinding.listItemEntryAvgDel.text =
                    getCurrencyString(this.item.entry.avgDelivery)
                detailsBinding.listItemEntryHourlyDels.text =
                    getFloatString(this.item.entry.hourlyDeliveries)

                updateMissingAlerts()
            }

            // IncomeItemHolder Overrides
            override suspend fun getExpenseValues(): Float =
                viewModel.getCostPerMile(item.entry.date, deductionType)

            override fun onNewExpenseValues() {
                val shouldShow = deductionType != NONE
                binding.listItemSubtitle2Label.fade(shouldShow)
                binding.listItemSubtitle2.fade(shouldShow)
                detailsBinding.listItemEntryCpmHeader.revealIfTrue(shouldShow)
                detailsBinding.listItemEntryCpmDeductionType.revealIfTrue(shouldShow)
                detailsBinding.listItemEntryCpm.revealIfTrue(shouldShow)
                detailsBinding.listItemEntryExpensesHeader.revealIfTrue(shouldShow)
                detailsBinding.listItemEntryExpenses.revealIfTrue(shouldShow)

                detailsBinding.listItemEntryCpmDeductionType.apply {
                    text = deductionType.fullDesc
                }

                val cpm: Float = expenseValues

                detailsBinding.listItemEntryExpenses.apply {
                    text = getCurrencyString(this@EntryHolder.item.entry.getExpenses(cpm))
                }

                detailsBinding.listItemEntryCpm.apply {
                    text = getCpmString(cpm)
                }

                binding.listItemSubtitle2Label.apply {
                    setText(R.string.list_item_label_net)
                }

                binding.listItemSubtitle2.apply {
                    text = getCurrencyString(this@EntryHolder.item.entry.getNet(cpm))
                }

                detailsBinding.listItemEntryHourly.apply {
                    text = getCurrencyString(this@EntryHolder.item.entry.getHourly(cpm))
                }

                detailsBinding.listItemEntryAvgDel.apply {
                    text = getCurrencyString(this@EntryHolder.item.entry.getAvgDelivery(cpm))
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FullEntry>() {
            override fun areItemsTheSame(oldItem: FullEntry, newItem: FullEntry): Boolean =
                oldItem.entry.entryId == newItem.entry.entryId


            override fun areContentsTheSame(
                oldItem: FullEntry,
                newItem: FullEntry
            ): Boolean =
                oldItem.entry.equals(newItem.entry) &&
                        oldItem.trackedDistance == newItem.trackedDistance
        }
    }
}