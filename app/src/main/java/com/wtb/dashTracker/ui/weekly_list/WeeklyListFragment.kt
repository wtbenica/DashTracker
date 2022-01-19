package com.wtb.dashTracker.ui.weekly_list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
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
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.databinding.ListItemWeeklyBinding
import com.wtb.dashTracker.databinding.ListItemWeeklyDetailsTableBinding
import com.wtb.dashTracker.extensions.*
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as WeeklyListFragmentCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_entry_list, container, false)

        recyclerView = view.findViewById(R.id.entry_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
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
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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

    interface WeeklyListFragmentCallback

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
                    WeeklyDialog(this@WeeklyHolder.compWeekly.weekly.date).show(
                        parentFragmentManager,
                        "edit_details"
                    )
                }
            }
        }

        override fun onClick(v: View?) {
            val currentVisibility = binding.listItemDetails.visibility
            binding.listItemDetails.visibility = if (currentVisibility == VISIBLE) GONE else VISIBLE
            binding.listItemWrapper.setBackgroundResource(if (currentVisibility == VISIBLE) R.drawable.list_item_background else R.drawable.list_item_expanded_background)
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

            val listItemDetailsVisibility = (payloads?.let {
                if (it.size == 1 && it[0] in listOf(
                        VISIBLE,
                        GONE
                    )
                ) it[0] else null
            } ?: GONE) as Int

            binding.listItemWrapper.setBackgroundResource(if (listItemDetailsVisibility == VISIBLE) R.drawable.list_item_expanded_background else R.drawable.list_item_background)

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

            detailsBinding.listItemWeeklyAdjust.text =
                getStringOrElse(R.string.currency_unit, "-", compWeekly.weekly.basePayAdjustment)

            binding.listItemAlert.visibility = toVisibleIfTrueElseGone(true)

            detailsBinding.listItemRegularPay.text = getCurrencyString(compWeekly.regularPay)

            detailsBinding.listItemWeeklyAdjust.text =
                getCurrencyString(compWeekly.weekly.basePayAdjustment)

            detailsBinding.listItemCashTips.text = getCurrencyString(compWeekly.cashTips)

            detailsBinding.listItemOtherPay.text = getCurrencyString(compWeekly.otherPay)

            detailsBinding.listItemWeeklyHours.text =
                getStringOrElse(R.string.float_fmt, "-", compWeekly.hours)

            detailsBinding.listItemWeeklyHourly.text = getCurrencyString(compWeekly.hourly)

            detailsBinding.listItemWeeklyAvgDel.text = getCurrencyString(compWeekly.avgDelivery)

            detailsBinding.listItemWeeklyHourlyDels.text =
                getStringOrElse(R.string.float_fmt, "-", compWeekly.delPerHour)

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
