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

package com.wtb.dashTracker.ui.entry_list

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.DeductionCallback
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.databinding.ListItemEntryBinding
import com.wtb.dashTracker.databinding.ListItemEntryDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog.Companion.ARG_EXTRA
import com.wtb.dashTracker.ui.dialog_entry.EntryDialog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

@ExperimentalCoroutinesApi
class EntryListFragment : Fragment() {

    private val viewModel: EntryListViewModel by viewModels()
    private var callback: EntryListFragmentCallback? = null

    private lateinit var recyclerView: RecyclerView
    private var deductionType: DeductionType = DeductionType.NONE

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as EntryListFragmentCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.frag_item_list, container, false)

        recyclerView = view.findViewById(R.id.item_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        setDialogListeners()

        return view
    }

    private fun setDialogListeners() {
        setFragmentResultListener(
            ConfirmType.DELETE.key
        ) { _, bundle ->
            val result = bundle.getBoolean(ARG_CONFIRM)
            val id = bundle.getInt(ARG_EXTRA)
            if (result) {
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
                viewModel.entryList.collectLatest {
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

    inner class EntryAdapter :
        PagingDataAdapter<DashEntry, EntryAdapter.EntryHolder>(DIFF_CALLBACK) {
        override fun onBindViewHolder(
            holder: EntryHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            super.onBindViewHolder(holder, position, payloads)
            getItem(position)?.let { holder.bind(it, payloads) }
        }

        override fun onBindViewHolder(holder: EntryHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryHolder =
            EntryHolder(parent)

        inner class EntryHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_entry, parent, false)
        ), View.OnClickListener {
            private lateinit var entry: DashEntry

            private val binding = ListItemEntryBinding.bind(itemView)
            private val detailsBinding = ListItemEntryDetailsTableBinding.bind(itemView)
            private val detailsTable: ConstraintLayout = binding.listItemDetails

            init {
                itemView.setOnClickListener(this)

                itemView.findViewById<ImageButton>(R.id.list_item_btn_edit).apply {
                    setOnClickListener {
                        EntryDialog.newInstance(this@EntryHolder.entry.entryId).show(
                            parentFragmentManager,
                            "edit_details"
                        )
                    }
                }

                itemView.findViewById<ImageButton>(R.id.list_item_btn_delete).apply {
                    setOnClickListener {
                        ConfirmDeleteDialog.newInstance(confirmId = this@EntryHolder.entry.entryId)
                            .show(parentFragmentManager, null)
                    }
                }
            }

            override fun onClick(v: View?) {
                val currentVisibility = detailsTable.visibility
                detailsTable.visibility = if (currentVisibility == VISIBLE) GONE else VISIBLE
                binding.listItemWrapper.setBackgroundResource(if (currentVisibility == VISIBLE) R.drawable.bg_list_item else R.drawable.bg_list_item_expanded)
                bindingAdapter?.notifyItemChanged(bindingAdapterPosition, detailsTable.visibility)
            }

            fun bind(item: DashEntry, payloads: MutableList<Any>? = null) {
                this.entry = item

                var costPerMile: Float?
                CoroutineScope(Dispatchers.Default).launch {
                    withContext(Dispatchers.Default) {
                        when (deductionType) {
                            DeductionType.NONE -> 0f
                            DeductionType.GAS_ONLY -> 0f
                            DeductionType.ALL_EXPENSES -> {
                                Log.d(TAG, "About to call get all expenses")
                                viewModel.getAllExpenseCPM(item.date)
                            }
                            DeductionType.STD_DEDUCTION -> callback?.standardMileageDeductions?.get(
                                this@EntryHolder.entry.date.year
                            )
                        }
                    }.let {
                        costPerMile = it
                        (context as MainActivity).runOnUiThread {
                            Log.d(
                                TAG,
                                "Something Something ${deductionType.name} $costPerMile $it"
                            )
                            binding.listItemSubtitle2.text =
                                getCurrencyString(
                                    (this@EntryHolder.entry.totalEarned
                                        ?: 0f) - this@EntryHolder.entry.getExpenses(
                                        costPerMile ?: 0f
                                    )
                                )
                        }
                    }
                }

                val detailsTableVisibility = (payloads?.let {
                    if (it.size == 1 && it[0] in listOf(VISIBLE, GONE)) {
                        it[0]
                    } else {
                        null
                    }
                } ?: GONE) as Int

                binding.listItemWrapper.setBackgroundResource(
                    if (detailsTableVisibility == VISIBLE) {
                        R.drawable.bg_list_item_expanded
                    } else {
                        R.drawable.bg_list_item
                    }
                )

                binding.listItemTitle.text = this.entry.date.formatted.uppercase()
                binding.listItemTitle2.text = getCurrencyString(this.entry.totalEarned)
                binding.listItemSubtitle.text =
                    getHoursRangeString(this.entry.startTime, this.entry.endTime)
                binding.listItemAlert.visibility = toVisibleIfTrueElseGone(this.entry.isIncomplete)

                detailsBinding.listItemRegularPay.text = getCurrencyString(this.entry.pay)
                detailsBinding.listItemCashTips.text = getCurrencyString(this.entry.cashTips)
                detailsBinding.listItemOtherPay.text = getCurrencyString(this.entry.otherPay)
                detailsBinding.listItemAlertHours.setVisibleIfTrue(
                    this.entry.startTime == null || this.entry.endTime == null
                )
                detailsBinding.listItemEntryHours.text =
                    getStringOrElse(R.string.float_fmt, "-", this.entry.totalHours)
                detailsBinding.listItemEntryMileageRange.text =
                    getOdometerRangeString(this.entry.startOdometer, this.entry.endOdometer)
                detailsBinding.listItemEntryMileage.text = "${this.entry.mileage ?: "-"}"
                detailsBinding.listItemAlertMiles.setVisibleIfTrue(this.entry.mileage == null)
                detailsBinding.listItemEntryNumDeliveries.text =
                    "${this.entry.numDeliveries ?: "-"}"
                detailsBinding.listItemAlertDeliveries.setVisibleIfTrue(this.entry.numDeliveries == null)
                detailsBinding.listItemEntryHourly.text =
                    getCurrencyString(this.entry.hourly)
                detailsBinding.listItemEntryAvgDel.text =
                    getCurrencyString(this.entry.avgDelivery)
                detailsBinding.listItemEntryHourlyDels.text =
                    getFloatString(this.entry.hourlyDeliveries)

                detailsTable.visibility = detailsTableVisibility
            }
        }
    }

    companion object {
        private const val TAG = APP + "EntryListFragment"

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DashEntry>() {
            override fun areItemsTheSame(oldItem: DashEntry, newItem: DashEntry): Boolean =
                oldItem.entryId == newItem.entryId


            override fun areContentsTheSame(
                oldItem: DashEntry,
                newItem: DashEntry
            ): Boolean =
                oldItem == newItem
        }

        fun toVisibleIfTrueElseGone(boolean: Boolean) = if (boolean) VISIBLE else GONE
    }
}