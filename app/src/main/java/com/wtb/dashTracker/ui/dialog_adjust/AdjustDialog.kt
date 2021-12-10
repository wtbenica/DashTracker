package com.wtb.dashTracker.ui.dialog_adjust

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.BasePayAdjustment
import com.wtb.dashTracker.databinding.DialogAdjustWeekSpinnerItemBinding
import com.wtb.dashTracker.databinding.DialogAdjustWeekSpinnerItemSingleLineBinding
import com.wtb.dashTracker.databinding.DialogFragAdjustBinding
import com.wtb.dashTracker.extensions.formatted
import com.wtb.dashTracker.extensions.weekOfYear
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

@ExperimentalCoroutinesApi
class AdjustDialog(
    private var basePayAdjustment: BasePayAdjustment? = null
) : FullWidthDialogFragment() {

    private lateinit var binding: DialogFragAdjustBinding
    private val viewModel: AdjustViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = basePayAdjustment?.id

        viewModel.loadDataModel(id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragAdjustBinding.inflate(layoutInflater)

        val adapter = WeekSpinnerAdapter(
            context!!,
            android.R.layout.simple_spinner_item,
            getListOfWeeks()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.fragAdjustDate.adapter = adapter

        binding.fragAdjustBtnDelete.setOnClickListener {
            basePayAdjustment?.let { e -> viewModel.delete(e) }
        }

        binding.fragAdjustBtnCancel.setOnClickListener {
            viewModel.clearEntry()
            clearFields()
        }

        updateUI()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.item.collectLatest {
                Log.d(TAG, "Changing adjust: $it")
                basePayAdjustment = it
                updateUI()
            }
        }
    }

    override fun onDestroy() {
        if (!isEmpty())
            saveValues()
        super.onDestroy()
    }

    private fun updateUI() {
        val tempAdjust = basePayAdjustment
        if (tempAdjust != null) {
            binding.fragAdjustDate.apply {
                getSpinnerIndex(tempAdjust.date)?.let { setSelection(it) }
            }
            binding.fragAdjustAmount.setText(getString(R.string.currency_unit, tempAdjust.amount))
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
        val date = binding.fragAdjustDate.selectedItem as LocalDate
        val adjust = BasePayAdjustment(
            adjustmentId = basePayAdjustment?.adjustmentId ?: AUTO_ID,
            date = date,
            amount = binding.fragAdjustAmount.text.toString().toFloat()
        )
        viewModel.upsert(adjust)
    }

    private fun clearFields() {
        binding.fragAdjustDate.setSelection(0)
        binding.fragAdjustAmount.text.clear()
    }

    private fun isEmpty(): Boolean = binding.fragAdjustAmount.text.isEmpty()

    inner class WeekSpinnerAdapter(
        context: Context,
        @LayoutRes resId: Int,
        private val itemList: Array<LocalDate>
    ) : ArrayAdapter<LocalDate>(context, resId, itemList) {

        var viewHolder: WeekSpinnerViewHolder? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cv = convertView
            if (cv == null) {
                val binding = DialogAdjustWeekSpinnerItemSingleLineBinding.inflate(layoutInflater)
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
            viewHolder?.dates?.text = getString(R.string.date_range, startWeek.formatted, endWeek.formatted)
            return cv
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cv = convertView
            if (cv == null) {
                val binding = DialogAdjustWeekSpinnerItemBinding.inflate(layoutInflater)
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
            viewHolder?.dates?.text = getString(R.string.date_range, startWeek.formatted, endWeek.formatted)
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
            var endOfWeek = MainActivity.getNextEndOfWeek().minusDays(7)
            while (endOfWeek > LocalDate.now().minusYears(1)) {
                res.add(endOfWeek)
                endOfWeek = endOfWeek.minusDays(7)
            }
            return res.toTypedArray()
        }
    }
}