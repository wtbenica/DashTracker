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

package com.wtb.dashTracker.ui.fragment_list_item_base.fragment_dailies

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.ListItemEntryBinding
import com.wtb.dashTracker.databinding.ListItemEntryDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
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
import com.wtb.dashTracker.ui.fragment_list_item_base.IncomeListItemFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class EntryListFragment : IncomeListItemFragment() {

    private val viewModel: EntryListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setDialogListeners()
    }

    private fun setDialogListeners() {
        childFragmentManager.setFragmentResultListener(
            ConfirmDialog.DELETE.key,
            this
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_IS_CONFIRMED)
            val id = bundle.getLong(ARG_EXTRA_ITEM_ID)

            if (result) {
                (activity as MainActivity).clearActiveEntry(id)
                viewModel.deleteEntryById(id)
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val entryAdapter = EntryAdapter().apply {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    binding.itemListRecyclerView.scrollToPosition(positionStart)
                }
            })
        }
        binding.itemListRecyclerView.adapter = entryAdapter

        callback?.deductionType?.asLiveData()?.observe(viewLifecycleOwner) {
            deductionType = it
            entryAdapter.notifyItemRangeChanged(0, entryAdapter.itemCount)
        }

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
        ListItemFragment.BaseItemPagingDataAdapter<FullEntry>(DIFF_CALLBACK) {
        override fun getViewHolder(parent: ViewGroup, viewType: Int?): BaseItemHolder<FullEntry> =
            EntryHolder(parent)

        inner class EntryHolder(parent: ViewGroup) : BaseItemHolder<FullEntry>(
            LayoutInflater.from(context).inflate(R.layout.list_item_entry, parent, false)
        ) {
            private val binding = ListItemEntryBinding.bind(itemView)
            private val detailsBinding = ListItemEntryDetailsTableBinding.bind(itemView)

            override val collapseArea: Array<View>
                get() = arrayOf(binding.listItemDetails)
            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper
            override val bgCard: CardView
                get() = binding.root

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

            override fun bind(item: FullEntry, payloads: List<Any>?) {
                fun showExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = VISIBLE
                    binding.listItemSubtitle2.visibility = VISIBLE
                    detailsBinding.listItemEntryCpmHeader.visibility = VISIBLE
                    detailsBinding.listItemEntryCpmDeductionType.visibility = VISIBLE
                    detailsBinding.listItemEntryCpm.visibility = VISIBLE
                    detailsBinding.listItemEntryExpensesHeader.visibility = VISIBLE
                    detailsBinding.listItemEntryExpenses.visibility = VISIBLE
                }

                fun hideExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = GONE
                    binding.listItemSubtitle2.visibility = GONE
                    detailsBinding.listItemEntryCpmHeader.visibility = GONE
                    detailsBinding.listItemEntryCpmDeductionType.visibility = GONE
                    detailsBinding.listItemEntryCpm.visibility = GONE
                    detailsBinding.listItemEntryExpensesHeader.visibility = GONE
                    detailsBinding.listItemEntryExpenses.visibility = GONE
                }

                fun updateMissingAlerts() {
                    binding.listItemAlert.setVisibleIfTrue(this.item.entry.isIncomplete)
                    detailsBinding.listItemAlertHours.setVisibleIfTrue(
                        this.item.entry.startTime == null || this.item.entry.endTime == null
                    )
                    detailsBinding.listItemAlertMiles.setVisibleIfTrue(this.item.entry.mileage == null)
                    detailsBinding.listItemAlertDeliveries.setVisibleIfTrue(this.item.entry.numDeliveries == null)
                }

                this.item = item

                CoroutineScope(Dispatchers.Default).launch {
                    withContext(Dispatchers.Default) {
                        viewModel.getCostPerMile(item.entry.date, deductionType)
                    }.let { cpm: Float? ->
                        val costPerMile = cpm ?: 0f
                        (context as MainActivity).runOnUiThread {
                            when (deductionType) {
                                DeductionType.NONE -> hideExpenseFields()
                                else -> {
                                    showExpenseFields()

                                    detailsBinding.listItemEntryCpmDeductionType.text =
                                        deductionType.fullDesc

                                    detailsBinding.listItemEntryExpenses.text = getCurrencyString(
                                        this@EntryHolder.item.entry.getExpenses(costPerMile)
                                    )

                                    detailsBinding.listItemEntryCpm.text =
                                        getCpmString(costPerMile)

                                    binding.listItemSubtitle2Label.setText(R.string.list_item_label_net)

                                    binding.listItemSubtitle2.text =
                                        getCurrencyString(
                                            this@EntryHolder.item.entry.getNet(costPerMile)
                                        )

                                    detailsBinding.listItemEntryHourly.text = getCurrencyString(
                                        this@EntryHolder.item.entry.getHourly(costPerMile)
                                    )

                                    detailsBinding.listItemEntryAvgDel.text = getCurrencyString(
                                        this@EntryHolder.item.entry.getAvgDelivery(costPerMile)
                                    )
                                }
                            }
                        }
                    }
                }

                binding.listItemEntryDayOfWeek.text =
                    getString(R.string.add_comma, this.item.entry.date.dayOfWeek.name.capitalize())
                binding.listItemEntryDayOfWeek.setTextColor(
                    if (this.item.entry.date.endOfWeek == LocalDate.now().endOfWeek) {
                        requireContext().getAttributeColor(R.attr.textColorListItemEntryDayThisWeek)
                    } else {
                        requireContext().getAttributeColor(R.attr.textColorListItemEntryDay)
                    }
                )

                binding.listItemTitle.text = this.item.entry.date.format(dtfDate)
                binding.listItemTitle2.text = getCurrencyString(this.item.entry.totalEarned)
                binding.listItemSubtitle.text =
                    getHoursRangeString(this.item.entry.startTime, this.item.entry.endTime)

                detailsBinding.listItemRegularPay.text = getCurrencyString(this.item.entry.pay)
                detailsBinding.listItemCashTips.text = getCurrencyString(this.item.entry.cashTips)
                detailsBinding.listItemOtherPay.text = getCurrencyString(this.item.entry.otherPay)

                detailsBinding.listItemEntryHours.apply {
                    setTextColor(
                        context.getAttributeColor(
                            if (this@EntryHolder.item.entry.startTime == null ||
                                this@EntryHolder.item.entry.endTime == null
                            ) {
                                R.attr.colorAlert
                            } else {
                                R.attr.colorTextPrimary
                            }
                        )
                    )
                }

                detailsBinding.listItemEntryHours.apply {
                    text =
                        getStringOrElse(
                            R.string.float_fmt,
                            "-",
                            this@EntryHolder.item.entry.totalHours
                        )
                    setTextColor(
                        context.getAttributeColor(
                            if (this@EntryHolder.item.entry.startTime == null ||
                                this@EntryHolder.item.entry.endTime == null
                            ) {
                                R.attr.colorAlert
                            } else {
                                R.attr.colorTextPrimary
                            }
                        )
                    )
                }

                detailsBinding.listItemEntryMileageRange.text =
                    getOdometerRangeString(
                        this.item.entry.startOdometer,
                        this.item.entry.endOdometer
                    )
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

                setVisibilityFromPayloads(payloads)
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