package com.wtb.dashTracker.ui.dialog_weekly

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.databinding.DialogFragWeeklyBinding
import com.wtb.dashTracker.databinding.DialogWeeklySpinnerItemBinding
import com.wtb.dashTracker.databinding.DialogWeeklySpinnerItemSingleLineBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmResetDialog
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm_delete.ConfirmationDialog
import com.wtb.dashTracker.views.FullWidthDialogFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

@ExperimentalCoroutinesApi
class WeeklyDialog(
    private var date: LocalDate = LocalDate.now().endOfWeek.minusDays(7)
) : FullWidthDialogFragment() {

    private var weekly: CompleteWeekly? = null
    private lateinit var binding: DialogFragWeeklyBinding
    private val viewModel: WeeklyViewModel by viewModels()

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
        setDialogListeners()

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding = DialogFragWeeklyBinding.inflate(layoutInflater)

        val adapter = WeekSpinnerAdapter(
            requireContext(),
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
                    saveValues()
                    val selectedDate = binding.fragAdjustDate.adapter.getItem(position) as LocalDate
                    viewModel.insert(Weekly(date = selectedDate, isNew = true))
                    viewModel.loadDate(selectedDate)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do Nothing
                }
            }

        binding.fragAdjustAmount.doOnTextChanged { text, start, before, count ->
            updateSaveButtonIsEnabled(text)
        }


        binding.fragAdjustBtnCancel.setOnClickListener {
            ConfirmResetDialog().show(parentFragmentManager, null)
        }

        binding.fragAdjustBtnSave.setOnClickListener {
            saveValues()
            dismiss()
        }

        weekly?.let { updateUI() }

        return binding.root
    }

    private fun setDialogListeners() {
        setFragmentResultListener(
            ConfirmType.RESET.key,
        ) { _, bundle ->
            val result = bundle.getBoolean(ConfirmationDialog.ARG_CONFIRM)
            if (result) {
                updateUI()
            }
        }

    }

    private fun updateSaveButtonIsEnabled(text: CharSequence?) {
        if (text == null || text.isEmpty()) {
            binding.fragAdjustBtnSave.alpha = 0.7f
            binding.fragAdjustBtnSave.isClickable = false
        } else {
            binding.fragAdjustBtnSave.alpha = 1.0f
            binding.fragAdjustBtnSave.isClickable = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.weekly.observe(viewLifecycleOwner) { w ->
            weekly = w
            updateUI()
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
                getSpinnerIndex(tempWeekly.weekly.date)?.let { setSelection(it) }
            }

            val text = getStringOrElse(
                R.string.float_fmt,
                "",
                tempWeekly.weekly.basePayAdjustment
            )
            binding.fragAdjustAmount.setText(text)
            updateSaveButtonIsEnabled(text)

            binding.fragAdjustTotal.text = getString(R.string.float_fmt, tempWeekly.totalPay)
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
        weekly?.weekly?.apply {
            basePayAdjustment = binding.fragAdjustAmount.text.toFloatOrNull()
            isNew = false
        }
        weekly?.let { viewModel.upsert(it.weekly) }
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
            val endWeek: LocalDate = itemList[position]
            val startWeek = endWeek.minusDays(6)
            val weekOfYear = endWeek.weekOfYear
            viewHolder?.weekNumber?.text = getString(R.string.week_number, weekOfYear)
            viewHolder?.dates?.text =
                getString(R.string.date_range, startWeek.shortFormat, endWeek.shortFormat)

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
                getString(R.string.date_range, startWeek.shortFormat, endWeek.shortFormat)
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