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
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao.NewCpm
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ChartHourlyGrossNetBinding
import com.wtb.dashTracker.extensions.dtfShortDateThisYear
import com.wtb.dashTracker.extensions.getAttrColor
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getDimen
import com.wtb.dashTracker.repository.DeductionType
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.fragment_trends.ByDayOfWeekBarChart.Companion.safeDiv
import android.widget.TableLayout
import com.wtb.dashTracker.views.WeeklyBarChart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class HourlyBarChart(
    context: Context,
    attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : DTChart(
    context,
    attrSet,
    defStyleAttr,
    R.string.lbl_hourly_gross_net,
    R.string.frag_title_income
) {
    val binding: ChartHourlyGrossNetBinding =
        ChartHourlyGrossNetBinding.inflate(LayoutInflater.from(context), this)
    private var barChartGrossNetHourly: WeeklyBarChart =
        binding.chartLineHourlyTrend.apply { style() }

    private val isDailySelected: Boolean
        get() = binding.hourlyTrendButtonGroup.checkedButtonId == R.id.hourly_trend_daily
    private val selectedDeductionType: DeductionType
        get() = when (binding.buttonGroupDeductionType.checkedButtonId) {
            R.id.gas_button -> DeductionType.GAS_ONLY
            R.id.actual_button -> DeductionType.ALL_EXPENSES
            R.id.standard_button -> DeductionType.IRS_STD
            else -> DeductionType.NONE
        }

    fun BarChart.style() {
        fun XAxis.style() {
            typeface = ResourcesCompat.getFont(context, R.font.lalezar)
            textColor = context.getAttrColor(R.attr.colorTextPrimary)
            textSize = context.getDimen(R.dimen.text_size_sm)
            position = XAxis.XAxisPosition.BOTTOM_INSIDE
            labelRotationAngle = 90f
            setDrawBorders(true)
            setDrawGridLines(false)

            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val date = LocalDate.ofEpochDay(value.toLong())
                    return if (isDailySelected)
                        date.format(dtfShortDateThisYear)
                    else {
                        context.getString(
                            R.string.date_range,
                            date.minusDays(6).format(dtfShortDateThisYear),
                            date.format(dtfShortDateThisYear)
                        )
                    }
                }
            }
        }

        fun YAxis.style() {
            typeface = ResourcesCompat.getFont(context, R.font.lalezar)
            textColor = context.getAttrColor(R.attr.colorTextPrimary)
            textSize = context.getDimen(R.dimen.text_size_sm)
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            this.axisMinimum = 0f
            labelCount = 4
            setDrawBorders(true)
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return context.getString(R.string.currency_fmt, value)
                }
            }
        }

        setBackgroundColor(Color.TRANSPARENT)

        description.isEnabled = false

        legend.isEnabled = false
        legend.typeface = ResourcesCompat.getFont(context, R.font.lalezar)
        legend.textColor = context.getAttrColor(R.attr.colorTextPrimary)
        legend.textSize = 14f

        isHighlightPerTapEnabled = false
        isHighlightFullBarEnabled = false
        isHighlightPerDragEnabled = false
        xAxis.style()
        axisLeft.style()
        axisRight.isEnabled = false
        setDrawGridBackground(true)
        setGridBackgroundColor(context.getAttrColor(R.attr.colorChartBackground))
    }

    init {
        fun SeekBar.initialize() {
            min = ChartsViewModel.MIN_NUM_DAYS_HOURLY_TREND
            max = if (isDailySelected) {
                mEntries.size
            } else {
                mWeeklies.size
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.numWeeksHourlyTrend.text = progress.toString()
                    update()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Do nothing
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Do nothing
                }
            })
            progress = ChartsViewModel.MIN_NUM_DAYS_HOURLY_TREND
        }

        binding.hourlyTrendButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                update(mCpmListDaily, mCpmListWeekly, mEntries, mWeeklies)
                when (checkedId) {
                    R.id.hourly_trend_daily -> {
                        barChartGrossNetHourly.isWeekly = false
                        binding.labelNumWeeksHourlyTrends.text =
                            context.getString(R.string.lbl_num_days)
                        binding.seekBarNumWeeksHourlyTrend.apply {
                            min = ChartsViewModel.MIN_NUM_DAYS_HOURLY_TREND
                            max = Integer.max(
                                ChartsViewModel.MIN_NUM_DAYS_HOURLY_TREND,
                                mEntries.size
                            )
                            progress = min
                        }
                    }
                    R.id.hourly_trend_weekly -> {
                        barChartGrossNetHourly.isWeekly = true
                        binding.labelNumWeeksHourlyTrends.text =
                            context.getString(R.string.lbl_num_weeks)
                        binding.seekBarNumWeeksHourlyTrend.apply {
                            min = ChartsViewModel.MIN_NUM_WEEKS
                            max = Integer.max(ChartsViewModel.MIN_NUM_WEEKS, mWeeklies.size)
                            progress = min
                        }
                    }
                }
            }
        }

        barChartGrossNetHourly = binding.chartLineHourlyTrend.apply { style() }
        binding.seekBarNumWeeksHourlyTrend.initialize()

        binding.buttonGroupDeductionType.addOnButtonCheckedListener { group, checkedId, isChecked ->
            update()
        }
    }

    override val filterTable: TableLayout
        get() = binding.tableFilters

    override fun update(
        cpmListDaily: List<NewCpm>?,
        cpmListWeekly: List<NewCpm>?,
        entries: List<DashEntry>?,
        weeklies: List<FullWeekly>?
    ) {
        super.update(cpmListDaily, cpmListWeekly, entries, weeklies)

        val regBarSize = .8f
        val dailyBarWidth = regBarSize / 2
        val grossNetBarOffset = dailyBarWidth / 2
        val weeklyBarWidth = regBarSize * 7 / 2
        val weeklyGrossNetBarOffset = weeklyBarWidth / 2

        fun BarDataSet.style() {
            valueTypeface = ResourcesCompat.getFont(context, R.font.lalezar)
            valueTextSize = context.getDimen(R.dimen.text_size_sm)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return context.getCurrencyString(value)
                }
            }
            setDrawIcons(false)
            valueTextSize = context.getDimen(R.dimen.text_size_sm)
            valueTextColor = context.getAttrColor(R.attr.colorTextPrimary)
        }

        data class DashEntryCollector(
            var totalHours: Float,
            var numDeliveries: Int,
            var totalEarned: Float,
            var mileage: Float,
        )

        fun getEntryList(): Pair<BarDataSet, BarDataSet> =
            mEntries.fold(mutableMapOf<LocalDate, DashEntryCollector>()) { map, de ->
                val dec = map[de.date] ?: DashEntryCollector(0f, 0, 0f, 0f)
                dec.totalHours += de.totalHours ?: 0f
                dec.numDeliveries += de.numDeliveries ?: 0
                dec.totalEarned += de.totalEarned
                dec.mileage += de.mileage ?: 0f
                map[de.date] = dec
                map
            }.mapNotNull {
                val hourly = safeDiv(it.value.totalEarned, it.value.totalHours)
                if (hourly != null && hourly !in listOf(Float.NaN, 0f)) {
                    val cpm =
                        if (mCpmListDaily.isNotEmpty()) mCpmListDaily.getByDate(
                            it.key,
                            selectedDeductionType
                        )
                            ?: 0f else 0f
                    val a = it.value.mileage * cpm
                    val expense: Float = safeDiv(a, it.value.totalHours) ?: 0f
                    val date = it.key.toEpochDay().toFloat()
                    Pair(
                        BarEntry(date - grossNetBarOffset, hourly),
                        BarEntry(date + grossNetBarOffset, hourly - expense)
                    )
                } else {
                    null
                }
            }.let {
                val startIndex = it.size - binding.seekBarNumWeeksHourlyTrend.progress
                val subListGross =
                    it.map { it1 -> it1.first }.reversed()
                        .subList(Integer.max(startIndex, 0), it.size)
                val subListNet =
                    it.map { it1 -> it1.second }.reversed()
                        .subList(Integer.max(startIndex, 0), it.size)
                Pair(
                    BarDataSet(
                        subListGross,
                        context.getString(R.string.lbl_gross)
                    ).apply { style() },
                    BarDataSet(subListNet, context.getString(R.string.lbl_net)).apply { style() })
            }

        fun getWeeklyList(): Pair<BarDataSet, BarDataSet> =
            mWeeklies.mapNotNull {
                val hourly = it.hourly
                if (hourly != null && hourly !in listOf(Float.NaN, 0f)) {
                    val cpm = mCpmListWeekly.getByDate(it.weekly.date, selectedDeductionType) ?: 0f
                    val mileageCost = cpm * it.miles
                    val hourlyCost = safeDiv(mileageCost, it.hours) ?: 0f
                    val date = it.weekly.date.toEpochDay().toFloat()
                    Pair(
                        BarEntry(date - weeklyGrossNetBarOffset, hourly),
                        BarEntry(date + weeklyGrossNetBarOffset, hourly - hourlyCost)
                    )
                } else {
                    null
                }
            }.let {
                val startIndex = it.size - binding.seekBarNumWeeksHourlyTrend.progress
                val subList = it.reversed().subList(Integer.max(startIndex, 0), it.size)
                Pair(
                    BarDataSet(
                        subList.map { p -> p.first },
                        context.getString(R.string.lbl_gross)
                    ).apply { style() },
                    BarDataSet(
                        subList.map { p -> p.second },
                        context.getString(R.string.lbl_net)
                    ).apply { style() }
                )
            }

        val dataSet: BarData = if (isDailySelected) {
            val gross = getEntryList().first.apply {
                color = context.getAttrColor(R.attr.colorChartGrossIncome)
            }
            val net = getEntryList().second.apply {
                color = context.getAttrColor(R.attr.colorChartNetIncome)
            }
            BarData(gross, net).also {
                barChartGrossNetHourly.legend.isEnabled = true
            }
        } else {
            val gross = getWeeklyList().first.apply {
                color = context.getAttrColor(R.attr.colorChartGrossIncome)
            }
            val net = getWeeklyList().second.apply {
                color = context.getAttrColor(R.attr.colorChartNetIncome)
            }
            BarData(gross, net).also {
                barChartGrossNetHourly.legend.isEnabled = true
            }
        }

        binding.seekBarNumWeeksHourlyTrend.max = if (isDailySelected) {
            mEntries.size
        } else {
            mWeeklies.size
        }

        val xAxisOffset = if (isDailySelected) 2 else 6
        barChartGrossNetHourly.xAxis.axisMinimum = dataSet.xMin - xAxisOffset
        barChartGrossNetHourly.xAxis.axisMaximum = dataSet.xMax + xAxisOffset

        barChartGrossNetHourly.apply {
            data = dataSet.apply {
                this.barWidth =
                    if (isDailySelected)
                        dailyBarWidth
                    else
                        weeklyBarWidth
            }
            setVisibleXRangeMinimum(if (isDailySelected) 7f else 56f)
        }

        (context as MainActivity).runOnUiThread {
            barChartGrossNetHourly.animateY(500)
        }
    }

    private fun List<NewCpm>.getByDate(date: LocalDate, deductionType: DeductionType): Float? {
        val index = binarySearch { cpm: NewCpm ->
            cpm.date.compareTo(date)
        }
        return if (index >= 0)
            get(index).getCpm(deductionType)
        else
            null
    }
}