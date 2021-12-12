package com.wtb.dashTracker.ui.weekly_list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.wtb.dashTracker.MainActivityViewModel.Companion.getTotalPay
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.databinding.ListItemWeeklyBinding
import com.wtb.dashTracker.extensions.formatted
import com.wtb.dashTracker.extensions.truncate
import com.wtb.dashTracker.extensions.weekOfYear
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyDialog
import com.wtb.dashTracker.ui.dialog_weekly.WeeklyViewModel
import com.wtb.dashTracker.ui.entry_list.EntryListFragment
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

    fun getStringOrElse(@StringRes resId: Int, ifNull: String, vararg args: Any?) =
        if (args.map { it != null }.reduce { acc, b -> acc && b }) getString(
            resId,
            *args
        ) else ifNull

    interface WeeklyListFragmentCallback

    inner class WeeklyHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_weekly, parent, false)
    ), View.OnClickListener {
        private val binding: ListItemWeeklyBinding

        private lateinit var weekly: Weekly

        private var weeklyTotal: Float? = null
            set(value) {
                field = value
                updateWeeklyTotalText()
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

        init {
            binding = ListItemWeeklyBinding.bind(itemView)
        }

        private fun updateWeeklyDelsTexts() {
            updateAvgDelivery()
            updateHourlyDeliveries()
        }

        private fun updateWeeklyHoursText() {
            totalHoursTextView.text = weeklyHours?.truncate(2) ?: "-"
            updateHourlyText()
            updateAvgDelivery()
        }

        private fun updateWeeklyTotalText() {
            regularPayTextView.text = getStringOrElse(
                R.string.currency_unit,
                "-",
                weeklyTotal
            )
            weeklyTotalTextView.text = getStringOrElse(
                R.string.currency_unit,
                "-",
                weeklyTotal?.let { it + (weekly.basePayAdjustment ?: 0f) })
            updateAvgDelivery()
            updateHourlyText()
        }

        private fun updateHourlyText() {
            hourlyTextView.text = getStringOrElse(
                R.string.currency_unit,
                "-",
                weeklyHours?.let { hours -> weeklyTotal?.let { total -> total / hours } })
        }

        private fun updateHourlyDeliveries() {
            hourlyDeliveriesTextView.text =
                weeklyDeliveries?.let { dels -> weeklyHours?.let { hours -> dels / hours } }
                    ?.truncate(2) ?: "-"
        }

        private fun updateAvgDelivery() {
            avgDeliveryTextView.text = getStringOrElse(
                R.string.currency_unit,
                "-",
                weeklyDeliveries?.let { dels -> weeklyTotal?.let { total -> total / dels } })
        }

        private val bg: LinearLayout = itemView.findViewById(R.id.list_item_weekly_wrapper)

        private val datesTextView: TextView = itemView.findViewById(R.id.list_item_weekly_dates)

        private val weeklyTotalTextView: TextView =
            itemView.findViewById(R.id.list_item_weekly_total)

        private val incompleteAlertImageView: ImageView =
            itemView.findViewById(R.id.list_item_incomplete_alert)

        private val detailsTable: ConstraintLayout =
            itemView.findViewById(R.id.list_item_weekly_details)

        private val totalHoursTextView: TextView =
            itemView.findViewById(R.id.list_item_weekly_hours)

        private val regularPayTextView: TextView =
            itemView.findViewById(R.id.list_item_weekly_regular_pay)

        private val adjustTextView: TextView =
            itemView.findViewById(R.id.list_item_weekly_adjust)

        private val hourlyTextView: TextView = itemView.findViewById(R.id.list_item_weekly_hourly)

        private val avgDeliveryTextView: TextView =
            itemView.findViewById(R.id.list_item_weekly_avg_del)

        private val hourlyDeliveriesTextView: TextView =
            itemView.findViewById(R.id.list_item_weekly_hourly_dels)


        init {
            itemView.setOnClickListener(this)

            itemView.findViewById<ImageButton>(R.id.list_item_weekly_btn_edit).apply {
                setOnClickListener {
                    WeeklyDialog(this@WeeklyHolder.weekly.date).show(
                        parentFragmentManager,
                        "edit_details"
                    )
                }
            }

            itemView.findViewById<ImageButton>(R.id.list_item_weekly_btn_delete).apply {
                setOnClickListener {
                    viewModel.delete(this@WeeklyHolder.weekly)
                }
            }
        }

        override fun onClick(v: View?) {
            val currentVisibility = detailsTable.visibility
            detailsTable.visibility = if (currentVisibility == VISIBLE) GONE else VISIBLE
            bindingAdapter?.notifyItemChanged(bindingAdapterPosition, detailsTable.visibility)
        }

        fun bind(item: Weekly, payloads: MutableList<Any>? = null) {
            this.weekly = item

            val detailsTableVisibility = (payloads?.let {
                if (it.size == 1 && it[0] in listOf(
                        VISIBLE,
                        GONE
                    )
                ) it[0] else null
            } ?: GONE) as Int

            binding.listItemWeeklyWeekNum.text =
                getString(R.string.week_number, weekly.date.weekOfYear)

            adjustTextView.text = getStringOrElse(R.string.currency_unit, "-", weekly.basePayAdjustment)

            incompleteAlertImageView.visibility = EntryListFragment.toVisibleIfTrueElseGone(true)

            viewModel.getEntriesByDate(this.weekly.date.minusDays(6), this.weekly.date).observe(
                viewLifecycleOwner,
                { entries ->
                    weeklyTotal = getTotalPay(entries)
                    weeklyDeliveries = getTotalByField(entries, DashEntry::numDeliveries)
                    weeklyHours = getTotalByField(entries, DashEntry::totalHours)
                }
            )
//
//            incompleteAlertImageView.visibility = toVisibleIfTrueElseGone(this.weekly.isIncomplete)
//
            datesTextView.text =
                getStringOrElse(
                    R.string.time_range,
                    "",
                    this.weekly.date.minusDays(6).formatted.uppercase(),
                    this.weekly.date.formatted.uppercase()
                )
//
//            totalHoursTextView.text =
//                getStringOrElse(R.string.format_hours, "-", this.weekly.totalHours)
//
//            mileageTextView.text =
//                getStringOrElse(
//                    R.string.odometer_range,
//                    "",
//                    this.weekly.startOdometer,
//                    this.weekly.endOdometer
//                )
//
//            totalMileageTextView.text = "${this.weekly.mileage ?: "-"}"
//
//            numDeliveriesTextView.text = this.weekly.numDeliveries?.toString() ?: "-"
//
//            hourlyTextView.text = getStringOrElse(R.string.hourly_rate, "-", this.weekly.hourly)
//
//            avgDeliveryTextView.text =
//                getStringOrElse(R.string.avg_delivery, "-", this.weekly.avgDelivery)
//
//            hourlyDeliveriesTextView.text =
//                getStringOrElse(
//                    R.string.dels_per_hour,
//                    "-",
//                    this.weekly.totalHours?.let { this.weekly.hourlyDeliveries })

            detailsTable.visibility = detailsTableVisibility
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