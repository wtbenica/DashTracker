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
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ChartHourlyByDayBinding
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getDimen
import com.wtb.dashTracker.extensions.getFloatString
import com.wtb.dashTracker.ui.fragment_trends.ByDayOfWeekBarChart.Companion.safeDiv
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.DayOfWeek

@ExperimentalCoroutinesApi
class ByDayOfWeekBarChart(
    context: Context,
    attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : DTChart(
    context,
    attrSet,
    defStyleAttr,
    R.string.lbl_hourly_by_day_of_week,
    R.string.frag_title_income
) {
    val binding =
        ChartHourlyByDayBinding.inflate(LayoutInflater.from(context), this)

    private var barChartHourlyByDay: HorizontalBarChart =
        binding.chartBarDailyHourly.apply { style() }

    private val dailyStats: List<DailyStats>
        get() = collectDailyStats()

    @IdRes
    private var selectedGraph: Int = binding.graphSelector.checkedButtonId

    init {
        binding.graphSelector.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                selectedGraph = checkedId
                update(mCpmList, mEntries, mWeeklies)
            }
        }
    }

    fun HorizontalBarChart.style() {
        fun XAxis.style() {
            setDrawBorders(true)
            typeface = ResourcesCompat.getFont(context, R.font.lalezar)
            textColor = MainActivity.getAttrColor(context, R.attr.colorTextPrimary)
            textSize = context.getDimen(R.dimen.text_size_sm)
            position = XAxis.XAxisPosition.BOTTOM
            setDrawBorders(true)
            setDrawGridLines(false)
            setCenterAxisLabels(false)
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    Log.d(TAG, "Axis $value")
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
        setBackgroundColor(
            ResourcesCompat.getColor(
                resources,
                android.R.color.transparent,
                null
            )
        )
        description.isEnabled = false
        isDragEnabled = false
        setScaleEnabled(false)
        setPinchZoom(false)
        legend.isEnabled = false
        xAxis.style()
        axisLeft.style()
        axisRight.isEnabled = false
        setDrawValueAboveBar(false)
    }

    override fun init() {
        barChartHourlyByDay = binding.chartBarDailyHourly.apply { style() }
    }

    override fun update(
        cpmList: List<TransactionDao.Cpm>?,
        entries: List<DashEntry>?,
        weeklies: List<FullWeekly>?
    ) {
        super.update(cpmList, entries, weeklies)

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
                label = "AM",
                barColor = R.attr.colorDayHeader
            ) { ds: DailyStats ->
                amLambda(ds)?.let { hourly ->
                    ds.day?.value?.toFloat()
                        ?.let { day -> BarEntry(8 - day - amPmBarOffset, hourly) }
                }
            }

        val dataSetPm: BarDataSet =
            dailyStats.getBarDataSet(label = "PM", barColor = R.attr.colorNightHeader) { ds ->
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
                    amDels = (a.amDels ?: 0f) + (d.dayDels ?: 0f),
                    pmDels = (a.pmDels ?: 0f) + (d.nightDels ?: 0f),
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

    private fun BarDataSet.style(@AttrRes barColor: Int = R.attr.colorSecondary) {
        valueTypeface = ResourcesCompat.getFont(context, R.font.lalezar)
        color = MainActivity.getAttrColor(context, barColor)
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
    val amHourly = safeDiv(amEarned, amHours)
    val pmHourly = safeDiv(pmEarned, pmHours)
    val amAvgDel = safeDiv(amEarned, amDels)
    val pmAvgDel = safeDiv(pmEarned, pmDels)
    val amDelHr = safeDiv(amDels, amHours)
    val pmDelHr = safeDiv(pmDels, pmHours)
}