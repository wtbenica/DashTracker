package com.wtb.dashTracker.ui.yearly_list

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.databinding.ListItemYearlyBinding
import com.wtb.dashTracker.databinding.ListItemYearlyDetailsTableBinding
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getMileageString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalCoroutinesApi
class YearlyListFragment : Fragment() {

    private val viewModel: YearlyListViewModel by viewModels()

    private val yearlies = mutableListOf<Yearly>()

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_entry_list, container, false)

        recyclerView = view.findViewById(R.id.entry_list)
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    data class Yearly(
        val year: Int,
        var mileage: Float = 0f,
        var pay: Float = 0f,
        var otherPay: Float = 0f,
        var cashTips: Float = 0f,
        var adjust: Float = 0f,
        var hours: Float = 0f
    ) {
        val reportedPay: Float
            get() = pay + otherPay + adjust

        val hourly: Float
            get() = (reportedPay + cashTips) / hours
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val entryAdapter = YearlyAdapter()
        recyclerView.adapter = entryAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWeeklies.collectLatest { cwList: List<CompleteWeekly> ->
                    yearlies.clear()
                    var numChecked = 0
                    var year =
                        cwList.map { it.weekly.date.year }.maxOrNull() ?: LocalDate.now().year + 1
                    while (numChecked < cwList.size) {
                        val thisYears = cwList.mapNotNull { cw: CompleteWeekly ->
                            if (cw.weekly.date.year == year) cw else null
                        }
                        numChecked += thisYears.size
                        val res = Yearly(year)
                        thisYears.forEach { cw: CompleteWeekly ->
                            res.adjust += cw.weekly.basePayAdjustment ?: 0f
                            res.pay += cw.entries.mapNotNull { entry ->
                                Log.d(TAG, "Pay: ${entry.pay}")
                                entry.pay
                            }
                                .reduceOrNull { acc, fl -> acc + fl } ?: 0f
                            res.otherPay += cw.entries.mapNotNull { entry ->
                                Log.d(TAG, "Other: ${entry.otherPay}")
                                entry.otherPay
                            }
                                .reduceOrNull { acc, fl -> acc + fl } ?: 0f
                            res.cashTips += cw.entries.mapNotNull { entry -> entry.cashTips }
                                .reduceOrNull { acc, fl -> acc + fl } ?: 0f
                            res.mileage += cw.entries.mapNotNull { entry -> entry.mileage }
                                .reduceOrNull { acc, fl -> acc + fl } ?: 0f
                            res.hours += cw.entries.mapNotNull { entry -> entry.totalHours }
                                .reduceOrNull { acc, fl -> acc + fl } ?: 0f
                        }
                        yearlies.add(res)
                        year -= 1
                    }
                    entryAdapter.submitList(yearlies)
                }
            }
        }
    }

    inner class YearlyAdapter : ListAdapter<Yearly, YearlyHolder>(DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearlyHolder =
            YearlyHolder(parent)

        override fun onBindViewHolder(holder: YearlyHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onBindViewHolder(
            holder: YearlyHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            super.onBindViewHolder(holder, position, payloads)
            holder.bind(getItem(position), payloads)
        }
    }

    inner class YearlyHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.list_item_yearly, parent, false)
    ),
        View.OnClickListener {
        private val binding: ListItemYearlyBinding = ListItemYearlyBinding.bind(itemView)
        private val detailsBinding: ListItemYearlyDetailsTableBinding =
            ListItemYearlyDetailsTableBinding.bind(itemView)
        private lateinit var yearly: Yearly

        init {
            itemView.setOnClickListener(this)
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

        fun bind(item: Yearly, payloads: MutableList<Any>? = null) {
            this.yearly = item

            val listItemDetailsVisibility = (payloads?.let {
                if (it.size == 1 && it[0] in listOf(
                        VISIBLE,
                        GONE
                    )
                ) it[0] else null
            } ?: GONE) as Int

            binding.listItemWrapper.setBackgroundResource(
                if (listItemDetailsVisibility == VISIBLE)
                    R.drawable.list_item_expanded_background
                else
                    R.drawable.list_item_background
            )

            binding.listItemTitle.text = this.yearly.year.toString()
            binding.listItemTitle2.text =
                getCurrencyString(this.yearly.reportedPay + this.yearly.cashTips)
            detailsBinding.listItemReportedIncome.text =
                getCurrencyString(yearly.reportedPay)
            detailsBinding.listItemCashTips.text = getCurrencyString(yearly.cashTips)
            detailsBinding.listItemYearlyMileage.text = getMileageString(yearly.mileage)
            detailsBinding.listItemYearlyHours.text = getString(R.string.format_hours, yearly.hours)
            detailsBinding.listItemYearlyHourly.text = getCurrencyString(yearly.hourly)
            binding.listItemDetails.visibility = listItemDetailsVisibility
        }
    }

    companion object {
        private const val TAG = APP + "MainFragment"

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Yearly>() {
            override fun areItemsTheSame(oldItem: Yearly, newItem: Yearly): Boolean =
                oldItem.year == newItem.year


            override fun areContentsTheSame(
                oldItem: Yearly,
                newItem: Yearly
            ): Boolean =
                oldItem == newItem
        }
    }
}
