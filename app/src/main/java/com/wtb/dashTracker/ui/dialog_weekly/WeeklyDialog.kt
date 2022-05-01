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

package com.wtb.dashTracker.ui.dialog_weekly

import android.content.Context
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
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.databinding.DialogFragWeeklyBinding
import com.wtb.dashTracker.databinding.DialogFragWeeklySpinnerItemBinding
import com.wtb.dashTracker.databinding.DialogFragWeeklySpinnerItemSingleLineBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmType
import com.wtb.dashTracker.ui.dialog_confirm.ConfirmationDialog
import com.wtb.dashTracker.ui.dialog_list_item.EditDataModelDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

@ExperimentalCoroutinesApi
class WeeklyDialog : EditDataModelDialog<Weekly, DialogFragWeeklyBinding>() {

    override var item: Weekly? = null
    private var fullWeekly: FullWeekly? = null
    override val viewModel: WeeklyViewModel by viewModels()
    override lateinit var binding: DialogFragWeeklyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val date: LocalDate =
            arguments?.getSerializable(ARG_DATE_ID) as LocalDate? ?: LocalDate.now().endOfWeek
        // it is an insert here instead of upsert bc don't want to replace one if it already exists
        viewModel.insert(Weekly(date))
        viewModel.loadDate(date)
    }

    override fun getViewBinding(inflater: LayoutInflater) =
        DialogFragWeeklyBinding.inflate(layoutInflater).apply {

            val adapter = WeekSpinnerAdapter(
                requireContext(),
                R.layout.dialog_frag_weekly_spinner_item_single_line,
                getListOfWeeks()
            ).apply {
                setDropDownViewResource(R.layout.dialog_frag_weekly_spinner_item)
            }

            fragAdjustDate.adapter = adapter

            fragAdjustDate.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selectedDate =
                            binding.fragAdjustDate.adapter.getItem(position) as LocalDate
                        // inserts if new, otherwise no effect
                        viewModel.insert(Weekly(date = selectedDate, isNew = true))
                        viewModel.loadDate(selectedDate)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Do Nothing
                    }
                }

            fragAdjustAmount.doOnTextChanged { text, start, before, count ->
                updateSaveButtonIsEnabled(text)
            }


            fragAdjustBtnCancel.apply {
                setOnResetPressed()
            }

            fragAdjustBtnSave.apply {
                setOnSavePressed()
            }
        }

    override fun updateUI() {
        val tempWeekly = fullWeekly
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

            binding.fragAdjustTotal.text = getString(R.string.float_fmt, tempWeekly.totalPay)
        }
    }

    override fun setDialogListeners() {
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
            fullWeekly = w
            updateUI()
        }
    }

    override fun onDestroy() {
        saveValues()
        super.onDestroy()
    }

    private fun getSpinnerIndex(date: LocalDate): Int? {
        (0..binding.fragAdjustDate.count).forEach { i ->
            if (binding.fragAdjustDate.adapter.getItem(i) == date) {
                return i
            }
        }
        return null
    }

    override fun saveValues() {
        val selectedDate =
            binding.fragAdjustDate.selectedItem as LocalDate
        val tempWeekly = fullWeekly?.weekly ?: Weekly(selectedDate)
        tempWeekly.apply {
            basePayAdjustment = binding.fragAdjustAmount.text.toFloatOrNull()
            isNew = false
        }
        viewModel.upsert(tempWeekly)
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
                val binding = DialogFragWeeklySpinnerItemSingleLineBinding.inflate(layoutInflater)
                cv = binding.root
                viewHolder = WeekSpinnerViewHolder(
                    null,
                    binding.dialogAdjustWeekSpinnerItemDates
                )
                cv.tag = viewHolder
            } else {
                viewHolder = cv.tag as WeekSpinnerViewHolder
            }
            val endWeek: LocalDate = itemList[position]
            val startWeek = endWeek.minusDays(6)
            viewHolder?.dates?.text =
                getString(R.string.date_range, startWeek.shortFormat, endWeek.shortFormat)

            return cv
        }

        override fun getDropDownView(
            position: Int, convertView: View?, parent: ViewGroup
        ): View {
            var cv = convertView
            if (cv == null) {
                val binding = DialogFragWeeklySpinnerItemBinding.inflate(layoutInflater)
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
        val weekNumber: TextView?,
        val dates: TextView
    )

    companion object {
        private const val ARG_DATE_ID = "date"

        fun newInstance(
            date: LocalDate = LocalDate.now().endOfWeek.minusDays(7)
        ) = WeeklyDialog().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_DATE_ID, date)
            }
        }

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

    override fun clearFields() {
        binding.apply {
            fragAdjustAmount.text.clear()
            fragAdjustDate.setSelection(0)
        }
    }

    override fun isEmpty(): Boolean {
        return binding.fragAdjustDate.selectedItemPosition == 0 &&
                binding.fragAdjustAmount.text.isNullOrBlank()
    }
}