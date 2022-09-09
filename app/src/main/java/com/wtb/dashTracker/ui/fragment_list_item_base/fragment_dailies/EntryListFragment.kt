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
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.FragItemListBinding
import com.wtb.dashTracker.databinding.ListItemEntryBinding
import com.wtb.dashTracker.databinding.ListItemEntryDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.DeductionCallback
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_EXTRA
import com.wtb.dashTracker.ui.dialog_edit_data_model.dialog_entry.EntryDialog
import com.wtb.dashTracker.ui.fragment_income.IncomeFragment
import com.wtb.dashTracker.ui.fragment_list_item_base.BaseItemHolder
import com.wtb.dashTracker.ui.fragment_list_item_base.BaseItemPagingDataAdapter
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

@ExperimentalCoroutinesApi
class EntryListFragment : ListItemFragment() {

    private val viewModel: EntryListViewModel by viewModels()
    private var callback: IncomeFragment.IncomeFragmentCallback? = null

    private var deductionType: DeductionType = DeductionType.NONE

    private lateinit var binding: FragItemListBinding

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

        setDialogListeners()

        return binding.root
    }

    private fun setDialogListeners() {
        setFragmentResultListener(
            ConfirmType.DELETE.key
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            val id = bundle.getLong(ARG_EXTRA)
            if (result) {
                (activity as MainActivity).stopLocationService()
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
//
//        lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.entryList.collectLatest {
//                    entryAdapter.submitData(it)
//                }
//            }
//        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fullEntryList.collectLatest {
                    entryAdapter.submitData(it)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    interface EntryListFragmentCallback : DeductionCallback

    inner class EntryAdapter : BaseItemPagingDataAdapter<FullEntry>(DIFF_CALLBACK) {
        override fun getViewHolder(parent: ViewGroup, viewType: Int?): BaseItemHolder<FullEntry> =
            EntryHolder(parent)

        inner class EntryHolder(parent: ViewGroup) : BaseItemHolder<FullEntry>(
            LayoutInflater.from(context).inflate(R.layout.list_item_entry, parent, false)
        ) {
            private val binding = ListItemEntryBinding.bind(itemView)
            private val detailsBinding = ListItemEntryDetailsTableBinding.bind(itemView)

            override val collapseArea: ConstraintLayout
                get() = binding.listItemDetails
            override val backgroundArea: LinearLayout
                get() = binding.listItemWrapper

            init {
                binding.listItemBtnEdit.apply {
                    setOnClickListener {
                        EntryDialog.newInstance(this@EntryHolder.item.entry.entryId)
                            .show(parentFragmentManager, "edit_entry_details")
                    }
                }

                binding.listItemBtnDelete.apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(
                            confirmId = this@EntryHolder.item.entry.entryId
                        ).show(parentFragmentManager, "delete_entry")
                    }
                }
            }

            override fun bind(item: FullEntry, payloads: List<Any>?) {
                fun showExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = VISIBLE
                    binding.listItemSubtitle2.visibility = VISIBLE
                    detailsBinding.listItemEntryCpmRow.visibility = VISIBLE
                    detailsBinding.listItemEntryExpensesRow.visibility = VISIBLE
                }

                fun hideExpenseFields() {
                    binding.listItemSubtitle2Label.visibility = GONE
                    binding.listItemSubtitle2.visibility = GONE
                    detailsBinding.listItemEntryCpmRow.visibility = GONE
                    detailsBinding.listItemEntryExpensesRow.visibility = GONE
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

                                    detailsBinding.listItemDeductionType.text =
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

                binding.listItemTitle.text = this.item.entry.date.formatted.uppercase()
                binding.listItemTitle2.text = getCurrencyString(this.item.entry.totalEarned)
                binding.listItemSubtitle.text =
                    getHoursRangeString(this.item.entry.startTime, this.item.entry.endTime)
                binding.listItemAlert.visibility =
                    toVisibleIfTrueElseGone(this.item.entry.isIncomplete)
                val mileageString = getMileageString(item.distance.toFloat())
                binding.trackedValue.text = mileageString

                detailsBinding.listItemRegularPay.text = getCurrencyString(this.item.entry.pay)
                detailsBinding.listItemCashTips.text = getCurrencyString(this.item.entry.cashTips)
                detailsBinding.listItemOtherPay.text = getCurrencyString(this.item.entry.otherPay)
                detailsBinding.listItemAlertHours.setVisibleIfTrue(
                    this.item.entry.startTime == null || this.item.entry.endTime == null
                )
                detailsBinding.listItemEntryHours.text =
                    getStringOrElse(R.string.float_fmt, "-", this.item.entry.totalHours)
                detailsBinding.listItemEntryMileageRange.text =
                    getOdometerRangeString(
                        this.item.entry.startOdometer,
                        this.item.entry.endOdometer
                    )
                detailsBinding.listItemEntryMileage.text = getStringOrElse(
                    R.string.odometer_fmt,
                    "-",
                    this.item.entry.mileage
                )
                detailsBinding.listItemAlertMiles.setVisibleIfTrue(this.item.entry.mileage == null)
                detailsBinding.listItemEntryNumDeliveries.text =
                    "${this.item.entry.numDeliveries ?: "-"}"
                detailsBinding.listItemAlertDeliveries.setVisibleIfTrue(this.item.entry.numDeliveries == null)
                detailsBinding.listItemEntryHourly.text =
                    getCurrencyString(this.item.entry.hourly)
                detailsBinding.listItemEntryAvgDel.text =
                    getCurrencyString(this.item.entry.avgDelivery)
                detailsBinding.listItemEntryHourlyDels.text =
                    getFloatString(this.item.entry.hourlyDeliveries)

                setPayloadVisibility(payloads)
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
                        oldItem.distance == newItem.distance
        }

        fun toVisibleIfTrueElseGone(boolean: Boolean) = if (boolean) VISIBLE else GONE
    }
}