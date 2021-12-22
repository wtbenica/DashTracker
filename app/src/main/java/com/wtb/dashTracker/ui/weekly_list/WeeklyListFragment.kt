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
import com.wtb.dashTracker.MainActivityViewModel.Companion.getTotalByField
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.databinding.ListItemWeeklyBinding
import com.wtb.dashTracker.databinding.ListItemWeeklyDetailsTableBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyViewModel
import com.wtb.dashTracker.ui.entry_list.EntryListFragment
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

        val entryAdapter = EntryAdapter()
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
        PagingDataAdapter<Weekly, WeeklyHolder>(DIFF_CALLBACK) {
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
        private val binding: ListItemWeeklyBinding
        private val detailsBinding: ListItemWeeklyDetailsTableBinding
        private lateinit var weekly: Weekly

        init {
            binding = ListItemWeeklyBinding.bind(itemView)
            detailsBinding = ListItemWeeklyDetailsTableBinding.bind(itemView)

            itemView.setOnClickListener(this)

            itemView.findViewById<ImageButton>(R.id.list_item_btn_edit).apply {
                setOnClickListener {
                    WeeklyDialog(this@WeeklyHolder.weekly.date).show(
                        parentFragmentManager,
                        "edit_details"
                    )
                }
            }
        }

        private var weeklyTotal: Float? = null
            set(value) {
                field = value
                updateWeeklyTotalText()
            }

        private var regularPay: Float? = null
            set(value) {
                field = value
                updateWeeklyTotal()
                detailsBinding.listItemRegularPay.text = getCurrencyString(field)
            }

        private var cashTips: Float? = null
            set(value) {
                field = value
                updateWeeklyTotal()
                detailsBinding.listItemCashTips.text = getCurrencyString(field)
            }

        private var otherPay: Float? = null
            set(value) {
                field = value
                updateWeeklyTotal()
                detailsBinding.listItemOtherPay.text = getCurrencyString(field)
            }

        private fun updateWeeklyTotal() {
            weeklyTotal = regularPay?.let { rp ->
                rp + (cashTips ?: 0f) + (otherPay ?: 0f) + (weekly.basePayAdjustment ?: 0f)
            }
        }

        private var weeklyHours: Float? = null
            set(value) {
                field = value
                updateWeeklyHoursText()
            }

        private var weeklyDeliveries: Int? = null
            set(value) {
                field = value
                updateWeeklyDelsTexts()
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

        fun bind(item: Weekly, payloads: MutableList<Any>? = null) {
            this.weekly = item

            val listItemDetailsVisibility = (payloads?.let {
                if (it.size == 1 && it[0] in listOf(
                        VISIBLE,
                        GONE
                    )
                ) it[0] else null
            } ?: GONE) as Int

            binding.listItemWrapper.setBackgroundResource(if (listItemDetailsVisibility == VISIBLE) R.drawable.list_item_expanded_background else R.drawable.list_item_background)

            binding.listItemSubtitle.text =
                getString(R.string.week_number, weekly.date.weekOfYear)

            detailsBinding.listItemWeeklyAdjust.text =
                getStringOrElse(R.string.currency_unit, "-", weekly.basePayAdjustment)

            binding.listItemAlert.visibility = EntryListFragment.toVisibleIfTrueElseGone(true)

            viewModel.getEntriesByDate(this.weekly.date.minusDays(6), this.weekly.date).observe(
                viewLifecycleOwner,
                { entries ->
                    regularPay = getTotalByField(entries, DashEntry::pay)
                    cashTips = getTotalByField(entries, DashEntry::cashTips)
                    otherPay = getTotalByField(entries, DashEntry::otherPay)
                    weeklyDeliveries = getTotalByField(entries, DashEntry::numDeliveries)
                    weeklyHours = getTotalByField(entries, DashEntry::totalHours)
                }
            )

            binding.listItemAlert.visibility = toVisibleIfTrueElseGone(this.weekly.isIncomplete)

            binding.listItemTitle.text =
                getStringOrElse(
                    R.string.time_range,
                    "",
                    this.weekly.date.minusDays(6).formatted.uppercase(),
                    this.weekly.date.formatted.uppercase()
                )

            binding.listItemDetails.visibility = listItemDetailsVisibility
        }

        private fun updateWeeklyDelsTexts() {
            updateAvgDelivery()
            updateHourlyDeliveries()
        }

        private fun updateWeeklyHoursText() {
            detailsBinding.listItemWeeklyHours.text = weeklyHours?.truncate(2) ?: "-"
            updateHourlyText()
            updateAvgDelivery()
        }

        private fun updateWeeklyTotalText() {
            binding.listItemTitle2.text = getStringOrElse(
                R.string.currency_unit,
                "-",
                weeklyTotal
            )
            updateAvgDelivery()
            updateHourlyText()
        }

        private fun updateHourlyText() {
            detailsBinding.listItemWeeklyHourly.text = getStringOrElse(
                R.string.currency_unit,
                "-",
                weeklyHours?.let { hours -> weeklyTotal?.let { total -> total / hours } })
        }

        private fun updateAvgDelivery() {
            detailsBinding.listItemWeeklyAvgDel.text = getStringOrElse(
                R.string.currency_unit,
                "-",
                weeklyDeliveries?.let { dels -> weeklyTotal?.let { total -> total / dels } })
        }

        private fun updateHourlyDeliveries() {
            detailsBinding.listItemWeeklyHourlyDels.text =
                weeklyDeliveries?.let { dels -> weeklyHours?.let { hours -> dels / hours } }
                    ?.truncate(2) ?: "-"
        }
    }

    companion object {
        private const val TAG = APP + "MainFragment"

        fun newInstance() = WeeklyListFragment()

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Weekly>() {
            override fun areItemsTheSame(oldItem: Weekly, newItem: Weekly): Boolean =
                oldItem.id == newItem.id


            override fun areContentsTheSame(
                oldItem: Weekly,
                newItem: Weekly
            ): Boolean =
                oldItem == newItem
        }
    }
}
