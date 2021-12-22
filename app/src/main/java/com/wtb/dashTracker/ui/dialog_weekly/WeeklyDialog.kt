package com.wtb.dashTracker.ui.dialog_weekly

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.databinding.DialogFragWeeklyBinding
import com.wtb.dashTracker.databinding.DialogWeeklySpinnerItemBinding
import com.wtb.dashTracker.databinding.DialogWeeklySpinnerItemSingleLineBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

@ExperimentalCoroutinesApi
class WeeklyDialog(
    private var date: LocalDate = LocalDate.now().endOfWeek.minusDays(7)
) : FullWidthDialogFragment() {

    private var weekly: Weekly? = null
    private lateinit var binding: DialogFragWeeklyBinding
    private val viewModel: WeeklyViewModel by viewModels()
    private var totalEarned: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.insert(Weekly(date))
        viewModel.loadDate(date)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragWeeklyBinding.inflate(layoutInflater)

        val adapter = WeekSpinnerAdapter(
            context!!,
            R.layout.dialog_weekly_spinner_item_single_line,
            getListOfWeeks()
        ).apply {
            setDropDownViewResource(R.layout.dialog_weekly_spinner_item)
        }

        binding.fragAdjustDate.adapter = adapter

        binding.fragAdjustDate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedDate = binding.fragAdjustDate.adapter.getItem(position) as LocalDate
                    viewModel.insert(Weekly(date = selectedDate))
                    viewModel.loadDate(selectedDate)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do Nothing
                }
            }


        binding.fragAdjustBtnDelete.setOnClickListener {
            weekly?.let { w -> viewModel.delete(w) }
        }

        binding.fragAdjustBtnCancel.setOnClickListener {
            viewModel.clearEntry()
            clearFields()
        }

        binding.fragAdjustBtnSave.setOnClickListener {
            saveValues()
            dismiss()
        }

        weekly?.let { updateUI() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.weekly.observe(
            viewLifecycleOwner,
            { w ->
                Log.d(TAG, "INCOMING WEEK: $w")
                weekly = w
                updateUI()
            }
        )

        viewModel.entriesByWeek.observe(
            viewLifecycleOwner,
        ) { entries: List<DashEntry>? ->
            Log.d(TAG, "INCOMING Entry List")
            var res = 0f
            entries?.forEach { res += it.totalEarned ?: 0f }
            binding.fragAdjustTotal.text = res.truncate(2)
        }
    }

    override fun onDestroy() {
        saveValues()
        super.onDestroy()
    }

    private fun updateUI() {
        val tempWeekly = weekly
        if (tempWeekly != null) {
            binding.fragAdjustDate.apply {
                getSpinnerIndex(tempWeekly.date)?.let { setSelection(it) }
            }

            binding.fragAdjustAmount.setText(tempWeekly.basePayAdjustment?.truncate(2) ?: "")
        }
    }

    private fun getSpinnerIndex(date: LocalDate): Int? {
        (0..binding.fragAdjustDate.count).forEach { i ->
            if (binding.fragAdjustDate.adapter.getItem(i) == date) {
                return i
            }
        }
        return null
    }

    private fun saveValues() {
        weekly?.basePayAdjustment =
            binding.fragAdjustAmount.text.toString().toFloatOrNull()?.truncate(2)?.toFloat()
        Log.d(TAG, "Saving $weekly")
        weekly?.let { viewModel.upsert(it) }
    }

    private fun clearFields() {
        binding.fragAdjustDate.setSelection(0)
        binding.fragAdjustAmount.text.clear()
    }

    inner class WeekSpinnerAdapter(
        context: Context,
        @LayoutRes resId: Int,
        private val itemList: Array<LocalDate>
    ) : ArrayAdapter<LocalDate>(context, resId, itemList) {

        private var viewHolder: WeekSpinnerViewHolder? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cv = convertView
            if (cv == null) {
                val binding = DialogWeeklySpinnerItemSingleLineBinding.inflate(layoutInflater)
                cv = binding.root
                viewHolder = WeekSpinnerViewHolder(
                    binding.dialogAdjustWeekSpinnerItemWeek,
                    binding.dialogAdjustWeekSpinnerItemDates
                )
                cv.tag = viewHolder
            } else {
                viewHolder = cv.tag as WeekSpinnerViewHolder
            }
            val endWeek = itemList[position]
            val startWeek = endWeek.minusDays(6)
            val weekOfYear = endWeek.weekOfYear
            viewHolder?.weekNumber?.text = getString(R.string.week_number, weekOfYear)
            viewHolder?.dates?.text =
                getString(R.string.date_range, startWeek.formatted, endWeek.formatted)

            return cv
        }

        override fun getDropDownView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            var cv = convertView
            if (cv == null) {
                val binding = DialogWeeklySpinnerItemBinding.inflate(layoutInflater)
                cv = binding.root
                viewHolder = WeekSpinnerViewHolder(
                    binding.dialogAdjustWeekSpinnerItemWeek,
                    binding.dialogAdjustWeekSpinnerItemDates
                )
                cv.tag = viewHolder
            } else {
                viewHolder = cv.tag as WeekSpinnerViewHolder
            }
            val endWeek = itemList[position]
            val startWeek = endWeek.minusDays(6)
            val weekOfYear = endWeek.weekOfYear
            viewHolder?.weekNumber?.text = getString(R.string.week_number, weekOfYear)
            viewHolder?.dates?.text =
                getString(R.string.date_range, startWeek.formatted, endWeek.formatted)
            return cv
        }
    }

    data class WeekSpinnerViewHolder(
        val weekNumber: TextView,
        val dates: TextView
    )

    companion object {
        private const val TAG = APP + "BasePayAdjustDialog"
        fun getListOfWeeks(): Array<LocalDate> {
            val res = arrayListOf<LocalDate>()
            var endOfWeek = LocalDate.now().endOfWeek
            while (endOfWeek > LocalDate.now().minusYears(1)) {
                res.add(endOfWeek)
                endOfWeek = endOfWeek.minusDays(7)
            }
            return res.toTypedArray()
        }
    }
}