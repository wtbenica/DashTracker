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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.databinding.ListItemEntryBinding
import com.wtb.dashTracker.databinding.ListItemEntryDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmDeleteDialog
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog.Companion.ARG_CONFIRM
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog.Companion.ARG_EXTRA
import com.wtb.dashTracker.ui.dialog_entry.EntryDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EntryListFragment : Fragment() {

    private val viewModel: EntryListViewModel by viewModels()
    private var callback: EntryListFragmentCallback? = null

    private lateinit var recyclerView: RecyclerView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as EntryListFragmentCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.frag_item_list, container, false)

        recyclerView = view.findViewById(R.id.entry_list)
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

    inner class EntryAdapter :
        PagingDataAdapter<DashEntry, EntryHolder>(DIFF_CALLBACK) {
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
    }

    interface EntryListFragmentCallback

    inner class EntryHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(context).inflate(R.layout.list_item_entry, parent, false)
    ), View.OnClickListener {
        private lateinit var entry: DashEntry
        private val binding = ListItemEntryBinding.bind(itemView)
        private val detailsBinding = ListItemEntryDetailsTableBinding.bind(itemView)

        private val dateTextView: TextView = itemView.findViewById(R.id.list_item_title)
        private val hoursTextView: TextView = itemView.findViewById(R.id.list_item_subtitle)
        private val totalPayTextView: TextView = itemView.findViewById(R.id.list_item_title_2)
        private val payPlusCCsTextView: TextView =
            itemView.findViewById(R.id.list_item_regular_pay)
        private val cashTipsTextView: TextView = itemView.findViewById(R.id.list_item_cash_tips)
        private val otherPayTextView: TextView = itemView.findViewById(R.id.list_item_other_pay)
        private val incompleteAlertImageView: ImageView =
            itemView.findViewById(R.id.list_item_alert)
        private val detailsTable: ConstraintLayout =
            itemView.findViewById(R.id.list_item_details)
        private val totalHoursTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_hours)
        private val mileageTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_mileage_range)

        private val totalMileageTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_mileage)

        private val numDeliveriesTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_num_deliveries)

        private val hourlyTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_hourly)

        private val avgDeliveryTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_avd_del)

        private val hourlyDeliveriesTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_hourly_dels)


        init {
            itemView.setOnClickListener(this)

            itemView.findViewById<ImageButton>(R.id.list_item_btn_edit).apply {
                setOnClickListener {
                    EntryDialog(this@EntryHolder.entry).show(
                        parentFragmentManager,
                        "edit_details"
                    )
                }
            }

            itemView.findViewById<ImageButton>(R.id.list_item_btn_delete).apply {
                setOnClickListener {
                    ConfirmDeleteDialog(confirmId = this@EntryHolder.entry.entryId)
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

            val detailsTableVisibility = (payloads?.let {
                if (it.size == 1 && it[0] in listOf(
                        VISIBLE,
                        GONE
                    )
                ) it[0] else null
            } ?: GONE) as Int

            binding.listItemWrapper.setBackgroundResource(if (detailsTableVisibility == VISIBLE) R.drawable.bg_list_item_expanded else R.drawable.bg_list_item)

            dateTextView.text = this.entry.date.formatted.uppercase()

            totalPayTextView.text =
                getCurrencyString(this.entry.totalEarned)

            payPlusCCsTextView.text =
                getCurrencyString(this.entry.pay)

            cashTipsTextView.text =
                getCurrencyString(this.entry.cashTips)

            otherPayTextView.text =
                getCurrencyString(this.entry.otherPay)

            incompleteAlertImageView.visibility =
                Companion.toVisibleIfTrueElseGone(this.entry.isIncomplete)

            hoursTextView.text =
                getStringOrElse(
                    R.string.time_range,
                    "",
                    this.entry.startTime?.format(dtfTime),
                    this.entry.endTime?.format(
                        dtfTime
                    )
                )
            detailsBinding.listItemAlertHours.setVisibleIfTrue(
                this.entry.startTime == null || this.entry.endTime == null
            )

            totalHoursTextView.text =
                getStringOrElse(R.string.float_fmt, "-", this.entry.totalHours)

            mileageTextView.text =
                getStringOrElse(
                    R.string.odometer_range,
                    "",
                    this.entry.startOdometer,
                    this.entry.endOdometer
                )

            totalMileageTextView.text = "${this.entry.mileage ?: "-"}"
            detailsBinding.listItemAlertMiles.setVisibleIfTrue(this.entry.mileage == null)

            numDeliveriesTextView.text = "${this.entry.numDeliveries ?: "-"}"
            detailsBinding.listItemAlertDeliveries.setVisibleIfTrue(this.entry.numDeliveries == null)

            hourlyTextView.text =
                getStringOrElse(R.string.currency_unit, "-", this.entry.hourly)

            avgDeliveryTextView.text =
                getStringOrElse(R.string.currency_unit, "-", this.entry.avgDelivery)

            hourlyDeliveriesTextView.text =
                getStringOrElse(R.string.float_fmt, "-", this.entry.hourlyDeliveries)

            detailsTable.visibility = detailsTableVisibility
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