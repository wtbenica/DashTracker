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

package com.wtb.dashTracker.ui.weekly_list

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
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
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.databinding.ListItemWeeklyBinding
import com.wtb.dashTracker.databinding.ListItemWeeklyDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyViewModel
import com.wtb.dashTracker.ui.entry_list.EntryListFragment.Companion.toVisibleIfTrueElseGone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class WeeklyListFragment : Fragment() {

    private val viewModel: WeeklyViewModel by viewModels()
    private var callback: WeeklyListFragmentCallback? = null

    private lateinit var recyclerView: RecyclerView
    private var deductionType: DeductionType = DeductionType.NONE

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as WeeklyListFragmentCallback
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

    interface WeeklyListFragmentCallback: DeductionCallback

    inner class EntryAdapter :
        PagingDataAdapter<CompleteWeekly, WeeklyHolder>(DIFF_CALLBACK) {
        override fun onBindViewHolder(
            holder: WeeklyHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            super.onBindViewHolder(holder, position, payloads)
            getItem(position)?.let { holder.bind(it, payloads) }
        }

        override fun onBindViewHolder(holder: WeeklyHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyHolder =
            WeeklyHolder(parent)

    }

    inner class WeeklyHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_weekly, parent, false)
    ), View.OnClickListener {
        private val binding: ListItemWeeklyBinding = ListItemWeeklyBinding.bind(itemView)
        private val detailsBinding: ListItemWeeklyDetailsTableBinding =
            ListItemWeeklyDetailsTableBinding.bind(itemView)
        private lateinit var compWeekly: CompleteWeekly

        init {
            itemView.setOnClickListener(this)

            itemView.findViewById<ImageButton>(R.id.list_item_btn_edit).apply {
                setOnClickListener {
                    WeeklyDialog.newInstance(this@WeeklyHolder.compWeekly.weekly.date)
                        .show(parentFragmentManager, "edit_details")
                }
            }
        }

        override fun onClick(v: View?) {
            val currentVisibility = binding.listItemDetails.visibility
            binding.listItemDetails.visibility = if (currentVisibility == VISIBLE) GONE else VISIBLE
            binding.listItemWrapper.setBackgroundResource(
                if (currentVisibility == VISIBLE) {
                    R.drawable.bg_list_item
                } else {
                    R.drawable.bg_list_item_expanded
                }
            )
            bindingAdapter?.notifyItemChanged(
                bindingAdapterPosition,
                binding.listItemDetails.visibility
            )
        }

        fun bind(item: CompleteWeekly, payloads: MutableList<Any>? = null) {
            if (item.isEmpty) {
                viewModel.delete(item.weekly)
            }

            this.compWeekly = item

            val costPerMile: Pair<Float, Float> = when (deductionType) {
                DeductionType.NONE -> Pair(0f, 0f)
                DeductionType.GAS_ONLY -> Pair(0f, 0f)
                DeductionType.ALL_EXPENSES -> Pair(0f, 0f)
                DeductionType.STD_DEDUCTION -> Pair(
                    callback?.standardMileageDeductions?.get(this.compWeekly.weekly.date.year)
                        ?: 0f,
                    callback?.standardMileageDeductions?.get(this.compWeekly.weekly.date.plusDays(6).year)
                        ?: 0f
                )
            }

            val listItemDetailsVisibility = (payloads?.let {
                if (it.size == 1 && it[0] in listOf(VISIBLE, GONE)) {
                    it[0]
                } else {
                    null
                }
            } ?: GONE) as Int

            binding.listItemWrapper.setBackgroundResource(
                if (listItemDetailsVisibility == VISIBLE) {
                    R.drawable.bg_list_item_expanded
                } else {
                    R.drawable.bg_list_item
                }
            )

            binding.listItemTitle2.text =
                getCurrencyString(
                    if (compWeekly.totalPay != 0f) {
                        compWeekly.totalPay
                    } else {
                        null
                    }
                )

            binding.listItemSubtitle.text =
                getString(R.string.week_number, compWeekly.weekly.date.weekOfYear)

            binding.listItemSubtitle2.text =
                getCurrencyString(
                    (compWeekly.totalPay - compWeekly.getExpensesForWeek(
                        costPerMile.first,
                        costPerMile.second
                    )).let {
                        if (it != 0f) {
                            it
                        } else {
                            null
                        }
                    }
                )

            detailsBinding.listItemWeeklyAdjust.text =
                getCurrencyString(compWeekly.weekly.basePayAdjustment)

            binding.listItemAlert.visibility = toVisibleIfTrueElseGone(true)

            detailsBinding.listItemRegularPay.text = getCurrencyString(compWeekly.regularPay)

            detailsBinding.listItemWeeklyAdjust.text =
                getCurrencyString(compWeekly.weekly.basePayAdjustment)

            detailsBinding.listItemCashTips.text = getCurrencyString(compWeekly.cashTips)

            detailsBinding.listItemOtherPay.text = getCurrencyString(compWeekly.otherPay)

            detailsBinding.listItemWeeklyHours.text =
                getFloatString(compWeekly.hours)

            detailsBinding.listItemWeeklyHourly.text = getCurrencyString(compWeekly.hourly)

            detailsBinding.listItemWeeklyAvgDel.text = getCurrencyString(compWeekly.avgDelivery)

            detailsBinding.listItemWeeklyHourlyDels.text =
                getFloatString(compWeekly.delPerHour)

            detailsBinding.listItemWeeklyMiles.text =
                getFloatString(compWeekly.miles)

            binding.listItemAlert.visibility =
                toVisibleIfTrueElseGone(this.compWeekly.weekly.isIncomplete)

            binding.listItemTitle.text =
                getStringOrElse(
                    R.string.time_range,
                    "",
                    this.compWeekly.weekly.date.minusDays(6).shortFormat.uppercase(),
                    this.compWeekly.weekly.date.shortFormat.uppercase()
                )

            binding.listItemDetails.visibility = listItemDetailsVisibility
        }
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CompleteWeekly>() {
            override fun areItemsTheSame(
                oldItem: CompleteWeekly,
                newItem: CompleteWeekly
            ): Boolean =
                oldItem.weekly.id == newItem.weekly.id


            override fun areContentsTheSame(
                oldItem: CompleteWeekly,
                newItem: CompleteWeekly
            ): Boolean =
                oldItem.weekly == newItem.weekly
        }
    }
}
