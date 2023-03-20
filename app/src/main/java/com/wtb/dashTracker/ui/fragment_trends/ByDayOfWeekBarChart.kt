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
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ChartByDayOfWeekBinding
import com.wtb.dashTracker.extensions.getAttrColor
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getFloatString
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.ui.fragment_trends.ByDayOfWeekBarChart.Companion.safeDiv
import com.wtb.dashTracker.views.ExpandableTableLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.DayOfWeek

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class ByDayOfWeekBarChart(
    context: Context,
    attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : DTChart(
    context,
    attrSet,
    defStyleAttr,
    R.string.lbl_daily_stats,
    R.string.frag_title_income
) {
    val binding: ChartByDayOfWeekBinding =
        ChartByDayOfWeekBinding.inflate(LayoutInflater.from(context), this)

    private var barChartHourlyByDay: HorizontalBarChart =
        binding.chartBarDailyHourly.apply { style() }

    private val dailyStats: List<DailyStats>
        get() = collectDailyStats()

    @IdRes
    private var selectedGraph: Int = binding.graphSelector.checkedButtonId
        set(value) {
            field = value
            binding.chartTitle.text = when (field) {
                R.id.btn_chart_per_delivery -> context.getString(R.string.chart_title_avg_per_delivery)
                R.id.btn_chart_del_per_hr -> context.getString(R.string.chart_title_avg_dels_per_hour)
                else -> context.getString(R.string.chart_title_avg_per_hour)
            }
        }

    init {
        binding.graphSelector.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                selectedGraph = checkedId
                update(null, mCpmListWeekly, mEntries, mWeeklies)
            }
        }

        barChartHourlyByDay = binding.chartBarDailyHourly.apply { style() }
    }

    fun HorizontalBarChart.style() {
        fun XAxis.style() {
            setDrawBorders(true)
            typeface = ResourcesCompat.getFont(context, R.font.lalezar)
            textColor = context.getAttrColor(R.attr.colorTextPrimary)
            textSize = context.getDimen(R.dimen.text_size_sm)
            position = XAxis.XAxisPosition.BOTTOM
            setDrawBorders(true)
            setDrawGridLines(false)
            setCenterAxisLabels(false)
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return if (value in 1f..7f)
                        DayOfWeek.of(8 - value.toInt()).name.slice(0..2)
                    else ""
                }
            }
        }

        fun YAxis.style() {
            isEnabled = false
            setDrawBorders(true)
            axisMinimum = 0f
        }

        setTouchEnabled(false)
        setBackgroundColor(Color.TRANSPARENT)

        description.isEnabled = false

        legend.isEnabled = true
        legend.typeface = ResourcesCompat.getFont(context, R.font.lalezar)
        legend.textColor = context.getAttrColor(R.attr.colorTextPrimary)
        legend.textSize = 14f

        isDragEnabled = false
        setScaleEnabled(false)
        setPinchZoom(false)

        xAxis.style()
        axisLeft.style()
        axisRight.isEnabled = false
        setDrawValueAboveBar(false)
        setDrawGridBackground(true)
        setGridBackgroundColor(context.getAttrColor(R.attr.colorChartBackground))
    }

    override val filterTable: ExpandableTableLayout
        get() = binding.tableFilters


    override fun update(
        cpmListDaily: List<TransactionDao.NewCpm>?,
        cpmListWeekly: List<TransactionDao.NewCpm>?,
        entries: List<DashEntry>?,
        weeklies: List<FullWeekly>?
    ) {
        super.update(null, cpmListWeekly, entries, weeklies)

        val barSize = .4f
        val amPmBarOffset = barSize / 2

        val amLambda = when (selectedGraph) {
            R.id.btn_chart_per_delivery -> DailyStats::amAvgDel
            R.id.btn_chart_del_per_hr -> DailyStats::amDelHr
            else -> DailyStats::amHourly
        }

        val pmLambda = when (selectedGraph) {
            R.id.btn_chart_per_delivery -> DailyStats::pmAvgDel
            R.id.btn_chart_del_per_hr -> DailyStats::pmDelHr
            else -> DailyStats::pmHourly
        }

        val dataSetAm: BarDataSet =
            dailyStats.getBarDataSet(
                label = context.getString(R.string.ante_meridiem),
                barColor = R.attr.colorDayHeader
            ) { ds: DailyStats ->
                amLambda(ds)?.let { hourly ->
                    ds.day?.value?.toFloat()
                        ?.let { day -> BarEntry(8 - day - amPmBarOffset, hourly) }
                }
            }

        val dataSetPm: BarDataSet =
            dailyStats.getBarDataSet(
                label = context.getString(R.string.post_meridiem),
                barColor = R.attr.colorNightHeader
            ) { ds ->
                pmLambda(ds)?.let { hourly ->
                    ds.day?.value?.toFloat()
                        ?.let { day -> BarEntry(8 - day + amPmBarOffset, hourly) }
                }
            }

        barChartHourlyByDay.data = BarData(dataSetPm, dataSetAm).apply { this.barWidth = barSize }

        (context as MainActivity).runOnUiThread {
            barChartHourlyByDay.animateY(1000)
        }
    }

    private fun collectDailyStats(): List<DailyStats> =
        DayOfWeek.values().map { day ->
            mEntries.filter { entry: DashEntry ->
                entry.startDateTime?.dayOfWeek == day
            }.fold(DailyStats(day = day)) { a: DailyStats, d: DashEntry ->
                DailyStats(
                    day = a.day,
                    amHours = (a.amHours ?: 0f) + (d.dayHours ?: 0f),
                    pmHours = (a.pmHours ?: 0f) + (d.nightHours ?: 0f),
                    amEarned = (a.amEarned ?: 0f) + (d.dayEarned ?: 0f),
                    pmEarned = (a.pmEarned ?: 0f) + (d.nightEarned ?: 0f),
                    amDels = (a.amDels ?: 0f) + (d.dayDeliveries ?: 0f),
                    pmDels = (a.pmDels ?: 0f) + (d.nightDeliveries ?: 0f),
                    amNumShifts = (a.amNumShifts ?: 0) + (if ((d.dayHours
                            ?: 0f) > 0f
                    ) 1 else 0),
                    pmNumShifts = (a.pmNumShifts ?: 0) + (if ((d.nightHours
                            ?: 0f) > 0f
                    ) 1 else 0),
                )
            }
        }

    private fun List<DailyStats>.getBarDataSet(
        label: String, @AttrRes barColor: Int, func: (DailyStats) -> BarEntry?
    ): BarDataSet {
        val barEntries: List<BarEntry> = mapNotNull { func(it) }
        val dataSet = BarDataSet(barEntries, label)
        dataSet.style(barColor)
        return dataSet
    }

    private fun BarDataSet.style(@AttrRes barColor: Int = R.attr.colorBarChartDatasetDefault) {
        valueTypeface = ResourcesCompat.getFont(context, R.font.lalezar)
        color = context.getAttrColor(barColor)
        valueTextSize = context.getDimen(R.dimen.text_size_sm)
        valueFormatter = object : ValueFormatter() {
            override fun getBarLabel(barEntry: BarEntry?): String {
                return when (selectedGraph) {
                    R.id.btn_chart_del_per_hr -> context.getFloatString(barEntry?.y)
                    else -> context.getCurrencyString(barEntry?.y)
                }
            }
        }
    }

    companion object {
        fun safeDiv(a: Float?, b: Float?): Float? =
            a?.let { _a ->
                b?.let { _b ->
                    if (_b == 0f) {
                        null
                    } else {
                        _a / _b
                    }
                }
            }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
data class DailyStats(
    val day: DayOfWeek? = null,
    val amHours: Float? = null,
    val pmHours: Float? = null,
    val amEarned: Float? = null,
    val pmEarned: Float? = null,
    val amDels: Float? = null,
    val pmDels: Float? = null,
    val amNumShifts: Int? = null,
    val pmNumShifts: Int? = null
) {
    val amHourly: Float? = safeDiv(amEarned, amHours)
    val pmHourly: Float? = safeDiv(pmEarned, pmHours)
    val amAvgDel: Float? = safeDiv(amEarned, amDels)
    val pmAvgDel: Float? = safeDiv(pmEarned, pmDels)
    val amDelHr: Float? = safeDiv(amDels, amHours)
    val pmDelHr: Float? = safeDiv(pmDels, pmHours)
}