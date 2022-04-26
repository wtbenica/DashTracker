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

package com.wtb.dashTracker.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.getAttrColor
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao.Cpm
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.FragChartsBinding
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getDimen
import com.wtb.dashTracker.repository.DeductionType.ALL_EXPENSES
import com.wtb.dashTracker.ui.fragment_base_list.toggleListItemVisibility
import com.wtb.dashTracker.ui.fragment_trends.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@ExperimentalCoroutinesApi
fun List<Cpm>.getByDate(date: LocalDate): Cpm? {
    val index = binarySearch { cpm: Cpm ->
        cpm.date.compareTo(date.endOfWeek)
    }
    return if (index >= 0)
        get(index)
    else
        null
}

@ExperimentalCoroutinesApi
class ChartFragment : Fragment(), DTChartHolder.DTChartHolderCallback {
    private val viewModel: DailyStatsViewModel by viewModels()
    private lateinit var binding: FragChartsBinding

    private lateinit var barChartHourlyByDay: HorizontalBarChart

    private var cpmList = listOf<Cpm>()
    private var entries = listOf<DashEntry>()
    private var weeklies = listOf<FullWeekly>()

    private val dailyStats: List<DailyStats>
        get() = collectDailyStats()

    private val isDailySelected: Boolean
        get() = binding.hourlyTrendButtonGroup.checkedButtonId == R.id.hourly_trend_daily


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_charts, container, false)
    }

    private fun setOnClickListeners() {
        binding.dailyHourlyCard.setOnClickListener {
            toggleListItemVisibility(binding.chartDailyHourly, binding.dailyHourlyWrapper)
        }

        binding.cpmCard.setOnClickListener {
            toggleListItemVisibility(binding.chartCpm, binding.cpmWrapper)
        }

        binding.hourlyTrendCard.setOnClickListener {
            toggleListItemVisibility(binding.chartHourlyTrend, binding.hourlyTrendWrapper)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragChartsBinding.bind(view)

        setOnClickListeners()

        val cpmChartHolder = DTChartHolder(requireContext()).apply {
            initialize(this@ChartFragment, CpmChart(context).apply { init() })
        }
        val hourlyChartHolder = DTChartHolder(requireContext()).apply {
            initialize(this@ChartFragment, HourlyBarChart(context).apply { init() })
        }

        binding.chartList.apply {
            removeAllViews()
            addView(cpmChartHolder)
            addView(hourlyChartHolder)
        }

        initDailyHourlyChart()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entryList.collectLatest {
                    entries = it
                    binding.seekBarNumWeeksHourlyTrend.max = entries.size

                    hourlyChartHolder.updateLists(cpmList = cpmList, entries = entries, weeklies = weeklies)
                }
            }
        }


        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weeklyList.collectLatest {
                    Log.d(TAG, "collectLatest weeklyList")
                    weeklies = it

                    cpmList = it.map { w ->
                        Cpm(
                            w.weekly.date,
                            viewModel.getExpensesAndCostPerMile(w, ALL_EXPENSES).second
                        )
                    }.reversed()

                    cpmChartHolder.updateLists(cpmList)
                    hourlyChartHolder.updateLists(cpmList = cpmList, entries = entries, weeklies = weeklies)
                }
            }
        }
    }

    private fun initDailyHourlyChart() {
        fun HorizontalBarChart.style() {
            fun XAxis.style() {
                setDrawBorders(true)
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
                textColor = getAttrColor(context, R.attr.colorTextPrimary)
                textSize = getDimen(R.dimen.text_size_sm)
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
            setBackgroundColor(getColor(resources, android.R.color.transparent, null))
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

        barChartHourlyByDay = binding.chartBarDailyHourly.apply { style() }
    }

    private fun updateDailyHourlyChart() {
        val barSize = .4f
        val amPmBarOffset = barSize / 2

        val dataSetAm: BarDataSet =
            dailyStats.getBarDataSet(label = "AM", barColor = R.attr.colorDayHeader) { ds ->
                DailyStatsRow.safeDiv(ds.amEarned, ds.amHours)?.let { hourly ->
                    ds.day?.value?.toFloat()
                        ?.let { day -> BarEntry(8 - day - amPmBarOffset, hourly) }
                }
            }

        val dataSetPm: BarDataSet =
            dailyStats.getBarDataSet(label = "PM", barColor = R.attr.colorNightHeader) { ds ->
                DailyStatsRow.safeDiv(ds.pmEarned, ds.pmHours)?.let { hourly ->
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
            entries.filter { entry: DashEntry ->
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
        valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
        color = getAttrColor(requireContext(), barColor)
        valueTextSize = getDimen(R.dimen.text_size_sm)
        valueFormatter = object : ValueFormatter() {
            override fun getBarLabel(barEntry: BarEntry?): String {
                return getCurrencyString(barEntry?.y)
            }
        }
    }

    private val charts = mutableListOf<Int>(1, 2, 3, 4, 5, 6, 56, 4, 63, 6, 3456, 45, 63, 4653)
}