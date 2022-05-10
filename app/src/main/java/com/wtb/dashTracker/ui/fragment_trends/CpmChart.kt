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

package com.wtb.dashTracker.ui.fragment_trends

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ChartCpmBinding
import com.wtb.dashTracker.extensions.dtfMini
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getDimen
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.activity_main.MainActivity.Companion.getAttrColor
import com.wtb.dashTracker.views.WeeklyLineChart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.Integer.min
import java.time.DayOfWeek
import java.time.LocalDate

@ExperimentalCoroutinesApi
class CpmChart(
    context: Context,
    attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : DTChart(
    context,
    attrSet,
    defStyleAttr,
    R.string.lbl_cost_per_mile,
    R.string.frag_title_expenses
) {
    val binding = ChartCpmBinding.inflate(LayoutInflater.from(context), this)
    private var lineChartCpm: WeeklyLineChart = binding.cpmChartLineCpm.apply { style() }
    private val selectedDeductionType: DeductionType
        get() = when (binding.buttonGroupDeductionType.checkedButtonId) {
            R.id.gas_button -> DeductionType.GAS_ONLY
            R.id.actual_button -> DeductionType.ALL_EXPENSES
            R.id.standard_button -> DeductionType.IRS_STD
            else -> DeductionType.NONE
        }

    fun WeeklyLineChart.style() {
        fun XAxis.style() {
            typeface = ResourcesCompat.getFont(context, R.font.lalezar)
            textColor = getAttrColor(context, R.attr.colorTextPrimary)
            textSize = context.getDimen(R.dimen.text_size_sm)
            position = XAxis.XAxisPosition.BOTTOM_INSIDE
            labelRotationAngle = 90f
            labelCount = 8
            setDrawBorders(true)
            setDrawGridLines(false)
            setCenterAxisLabels(false)
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val date = LocalDate.ofEpochDay(value.toLong())
                    return if (date.dayOfWeek == DayOfWeek.SUNDAY)
                        context.getString(
                            R.string.date_range,
                            date.minusDays(6).format(dtfMini),
                            date.format(dtfMini)
                        )
                    else
                        ""
                }
            }
        }

        fun YAxis.style() {
            setCenterAxisLabels(true)
            setDrawTopYLabelEntry(false)
            typeface = ResourcesCompat.getFont(context, R.font.lalezar)
            textColor = getAttrColor(context, R.attr.colorTextPrimary)
            textSize = context.getDimen(R.dimen.text_size_sm)
            this.axisMinimum = 0f
            setDrawBorders(true)
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return context.getString(R.string.cpm_unit, value)
                }
            }
        }

        isWeekly = true
        setBackgroundColor(
            ResourcesCompat.getColor(
                resources,
                android.R.color.transparent,
                null
            )
        )
        setDrawGridBackground(true)
        setGridBackgroundColor(getAttrColor(context, R.attr.colorListItem))
        description.isEnabled = false
        legend.isEnabled = false
        legend.typeface = ResourcesCompat.getFont(context, R.font.lalezar)
        isHighlightPerTapEnabled = false
        isHighlightPerDragEnabled = false
        xAxis.style()
        axisLeft.style()
        axisRight.isEnabled = false
    }

    init {
        fun SeekBar.initialize() {
            min = ChartsViewModel.MIN_NUM_WEEKS
            max = mCpmListWeekly.size

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.cpmNumWeeksTv.text = progress.toString()
                    if (mCpmListWeekly.isNotEmpty())
                        update(cpmListDaily = null, cpmListWeekly = mCpmListWeekly)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Do nothing
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Do nothing
                }
            })
            progress = ChartsViewModel.MIN_NUM_WEEKS
        }

        lineChartCpm = binding.cpmChartLineCpm.apply { style() }
        binding.cpmSeekBarNumWeeks.initialize()

        binding.buttonGroupDeductionType.addOnButtonCheckedListener { _, _, _ -> update() }
    }

    override val filterTable: ViewGroup
        get() = binding.tableFilters

    override fun update(
        cpmListDaily: List<TransactionDao.NewCpm>?,
        cpmListWeekly: List<TransactionDao.NewCpm>?,
        entries: List<DashEntry>?,
        weeklies: List<FullWeekly>?
    ) {
        super.update(null, cpmListWeekly, entries, weeklies)

        fun LineDataSet.style() {
            valueTypeface = ResourcesCompat.getFont(context, R.font.lalezar)
            valueTextSize = context.getDimen(R.dimen.text_size_sm)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return context.getCurrencyString(value)
                }
            }
            valueTextColor = getAttrColor(context, R.attr.colorTextPrimary)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawCircles(false)
            setDrawIcons(false)
            setDrawCircleHole(false)
            setDrawFilled(true)
            fillDrawable =
                ContextCompat.getDrawable(context, R.drawable.bad_to_good)
            fillAlpha = 85
            cubicIntensity = 0.2f
            valueTextSize = context.getDimen(R.dimen.text_size_sm)
        }

        fun getEntryList(): LineDataSet =
            mCpmListWeekly.mapNotNull { cpm ->
                if (cpm.getCpm(selectedDeductionType) !in listOf(Float.NaN, 0f)) {
                    Entry(cpm.date.toEpochDay().toFloat(), cpm.getCpm(selectedDeductionType))
                } else {
                    null
                }
            }.let {
                Log.d(TAG, "getEntryList: $it")
                val fromIndex =
                    Integer.max(it.size - binding.cpmSeekBarNumWeeks.progress, min(it.size, 1))
                LineDataSet(it.subList(fromIndex, it.size), "CPM").apply { style() }
            }

        val dataSet = getEntryList()

        binding.cpmSeekBarNumWeeks.max = mCpmListWeekly.size

        lineChartCpm.xAxis?.axisMinimum = dataSet.xMin - 2
        lineChartCpm.xAxis?.axisMaximum = dataSet.xMax + 2

        lineChartCpm.apply {
            data = LineData(dataSet)
            setVisibleXRangeMinimum(56f)
        }

        (context as MainActivity).runOnUiThread {
            lineChartCpm.animateY(1000)
        }
    }
}