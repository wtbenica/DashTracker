package com.wtb.dashTracker.ui.entry_list

import android.content.Context
import android.graphics.Color
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
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.extensions.dtfTime
import com.wtb.dashTracker.extensions.formatted
import com.wtb.dashTracker.ui.dialog_entry.EntryDialog
import com.wtb.dashTracker.ui.weekly_list.WeeklyListFragment
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

    fun getStringOrElse(@StringRes resId: Int, ifNull: String, vararg args: Any?) =
        if (args.map { it != null }.reduce { acc, b -> acc && b }) getString(
            resId,
            *args
        ) else ifNull

    interface EntryListFragmentCallback

    inner class EntryHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_entry, parent, false)
    ), View.OnClickListener {
        private lateinit var entry: DashEntry

        private val bg: LinearLayout = itemView.findViewById(R.id.list_item_entry_wrapper)

        private val dateTextView: TextView = itemView.findViewById(R.id.list_item_entry_date)

        private val payTextView: TextView = itemView.findViewById(R.id.list_item_entry_total)

        private val incompleteAlertImageView: ImageView =
            itemView.findViewById(R.id.list_item_incomplete_alert)

        private val detailsTable: ConstraintLayout =
            itemView.findViewById(R.id.list_item_entry_details)

        private val hoursTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_hours_range)

        private val totalHoursTextView: TextView = itemView.findViewById(R.id.list_item_entry_hours)

        private val mileageTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_mileage_range)

        private val totalMileageTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_mileage)

        private val numDeliveriesTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_num_deliveries)

        private val hourlyTextView: TextView = itemView.findViewById(R.id.list_item_entry_hourly)

        private val avgDeliveryTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_avd_del)

        private val hourlyDeliveriesTextView: TextView =
            itemView.findViewById(R.id.list_item_entry_hourly_dels)


        init {
            itemView.setOnClickListener(this)

            itemView.findViewById<ImageButton>(R.id.list_item_entry_btn_edit).apply {
                setOnClickListener {
                    EntryDialog(this@EntryHolder.entry).show(
                        parentFragmentManager,
                        "edit_details"
                    )
                }
            }

            itemView.findViewById<ImageButton>(R.id.list_item_entry_btn_delete).apply {
                setOnClickListener {
                    viewModel.delete(this@EntryHolder.entry)
                }
            }
        }

        override fun onClick(v: View?) {
            val currentVisibility = detailsTable.visibility
            detailsTable.visibility = if (currentVisibility == VISIBLE) GONE else VISIBLE
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

            val color =
                if (this.entry.isXWeeksAgo(0))
                    Color.WHITE
                else
                    Color.parseColor("#EEEEEE")
            bg.setBackgroundColor(color)

            dateTextView.text = this.entry.date.formatted.uppercase()

            payTextView.text = getStringOrElse(R.string.currency_unit, "$-", this.entry.totalEarned)

            incompleteAlertImageView.visibility = Companion.toVisibleIfTrueElseGone(this.entry.isIncomplete)

            hoursTextView.text =
                getStringOrElse(
                    R.string.time_range,
                    "",
                    this.entry.startTime?.format(dtfTime),
                    this.entry.endTime?.format(
                        dtfTime
                    )
                )

            totalHoursTextView.text =
                getStringOrElse(R.string.format_hours, "-", this.entry.totalHours)

            mileageTextView.text =
                getStringOrElse(
                    R.string.odometer_range,
                    "",
                    this.entry.startOdometer,
                    this.entry.endOdometer
                )

            totalMileageTextView.text = "${this.entry.mileage ?: "-"}"

            numDeliveriesTextView.text = this.entry.numDeliveries?.toString() ?: "-"

            hourlyTextView.text = getStringOrElse(R.string.hourly_rate, "-", this.entry.hourly)

            avgDeliveryTextView.text =
                getStringOrElse(R.string.avg_delivery, "-", this.entry.avgDelivery)

            hourlyDeliveriesTextView.text =
                getStringOrElse(
                    R.string.dels_per_hour,
                    "-",
                    this.entry.totalHours?.let { this.entry.hourlyDeliveries })

            detailsTable.visibility = detailsTableVisibility
        }
    }

    companion object {
        private const val TAG = APP + "MainFragment"

        fun newInstance() = WeeklyListFragment()

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