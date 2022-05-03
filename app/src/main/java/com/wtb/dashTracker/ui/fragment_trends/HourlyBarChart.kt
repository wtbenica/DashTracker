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
import android.widget.SeekBar
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.ChartHourlyGrossNetBinding
import com.wtb.dashTracker.extensions.dtfMini
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getDimen
import com.wtb.dashTracker.ui.fragment_trends.ByDayOfWeekBarChart.Companion.safeDiv
import com.wtb.dashTracker.views.WeeklyBarChart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

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
    val binding =
        ChartHourlyGrossNetBinding.inflate(LayoutInflater.from(context), this)
    private var barChartGrossNetHourly: WeeklyBarChart =
        binding.chartLineHourlyTrend.apply { style() }

    private val isDailySelected: Boolean
        get() = binding.hourlyTrendButtonGroup.checkedButtonId == R.id.hourly_trend_daily

    fun BarChart.style() {
        fun XAxis.style() {
            typeface = ResourcesCompat.getFont(context, R.font.lalezar)
            textColor = MainActivity.getAttrColor(context, R.attr.colorTextPrimary)
            textSize = context.getDimen(R.dimen.text_size_sm)
            position = XAxis.XAxisPosition.BOTTOM_INSIDE
            labelRotationAngle = 90f
            setDrawBorders(true)
            setDrawGridLines(false)

            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val date = LocalDate.ofEpochDay(value.toLong())
                    return if (isDailySelected)
                        date.format(dtfMini)
                    else {
                        context.getString(
                            R.string.date_range,
                            date.minusDays(6).format(dtfMini),
                            date.format(dtfMini)
                        )
                    }
                }
            }
        }

        fun YAxis.style() {
            typeface = ResourcesCompat.getFont(context, R.font.lalezar)
            textColor = MainActivity.getAttrColor(context, R.attr.colorTextPrimary)
            textSize = context.getDimen(R.dimen.text_size_sm)
            setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            this.axisMinimum = 0f
            labelCount = 4
            setDrawBorders(true)
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return context.getString(R.string.currency_unit, value)
                }
            }
        }

        setBackgroundColor(
            ResourcesCompat.getColor(
                resources,
                android.R.color.transparent,
                null
            )
        )
        description.isEnabled = false
        legend.isEnabled = false
        legend.typeface = ResourcesCompat.getFont(context, R.font.lalezar)
        isHighlightPerTapEnabled = false
        isHighlightFullBarEnabled = false
        isHighlightPerDragEnabled = false
        xAxis.style()
        axisLeft.style()
        axisRight.isEnabled = false
        setDrawGridBackground(true)
    }

    override fun init() {
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
                    if (mEntries.isNotEmpty())
                        update(entries = mEntries)
                    if (mCpmList.isNotEmpty())
                        update(cpmList = mCpmList)
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
                update(mCpmList, mEntries, mWeeklies)
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
    }

    override fun update(
        cpmList: List<TransactionDao.Cpm>?,
        entries: List<DashEntry>?,
        weeklies: List<FullWeekly>?
    ) {
        super.update(cpmList, entries, weeklies)

        Log.d(TAG, "${mCpmList.size} ${mEntries.size} ${mWeeklies.size}")
        val regBarSize = .8f
        val dailyBarWidth = regBarSize / 2
        val weeklyBarWidth = regBarSize * 7
        val grossNetBarOffset = dailyBarWidth / 2

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
                dec.totalEarned += de.totalEarned ?: 0f
                dec.mileage += de.mileage ?: 0f
                map[de.date] = dec
                map
            }.mapNotNull {
                val hourly = safeDiv(it.value.totalEarned, it.value.totalHours)
                if (hourly != null && hourly !in listOf(Float.NaN, 0f)) {
                    val cpm =
                        if (mCpmList.isNotEmpty()) mCpmList.getByDate(it.key)?.cpm ?: 0f else 0f
                    val expense: Float = safeDiv(
                        it.value.mileage * cpm,
                        it.value.totalHours
                    ) ?: 0f
                    val toFloat = it.key.toEpochDay().toFloat()
                    Pair(
                        BarEntry(toFloat - grossNetBarOffset, hourly),
                        BarEntry(toFloat + grossNetBarOffset, hourly - expense)
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
                    BarDataSet(subListGross, "Gross").apply { style() },
                    BarDataSet(subListNet, "Net").apply { style() })
            }

        fun getWeeklyList(): BarDataSet = mWeeklies.mapNotNull {
            val hourly = it.hourly
            if (hourly != null && hourly !in listOf(Float.NaN, 0f)) {
                BarEntry(it.weekly.date.toEpochDay().toFloat(), hourly)
            } else {
                null
            }
        }.let {
            val startIndex = it.size - binding.seekBarNumWeeksHourlyTrend.progress
            val subList = it.reversed().subList(Integer.max(startIndex, 0), it.size)
            BarDataSet(subList, "Hourly by Week").apply { style() }
        }

        val dataSet: BarData = if (isDailySelected) {
            val gross = getEntryList().first.apply {
                color = MainActivity.getAttrColor(context, R.attr.colorPrimaryDark)
            }
            val net = getEntryList().second.apply {
                color = MainActivity.getAttrColor(context, R.attr.colorPrimary)
            }
            BarData(gross, net).also {
                barChartGrossNetHourly.legend.isEnabled = true
            }
        } else {
            BarData(getWeeklyList()).also {
                barChartGrossNetHourly.legend.isEnabled = false
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

    private fun List<TransactionDao.Cpm>.getByDate(date: LocalDate): TransactionDao.Cpm? {
        val index = binarySearch { cpm: TransactionDao.Cpm ->
            cpm.date.compareTo(date.endOfWeek)
        }
        return if (index >= 0)
            get(index)
        else
            null
    }
}