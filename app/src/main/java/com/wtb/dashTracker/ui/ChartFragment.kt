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

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.wtb.dashTracker.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.MainActivity.Companion.getAttrColor
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.daos.TransactionDao.Cpm
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.FullWeekly
import com.wtb.dashTracker.databinding.FragChartsBinding
import com.wtb.dashTracker.extensions.dtfMini
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.repository.DeductionType.ALL_EXPENSES
import com.wtb.dashTracker.ui.fragment_base_list.toggleListItemVisibility
import com.wtb.dashTracker.ui.fragment_trends.DailyStats
import com.wtb.dashTracker.ui.fragment_trends.DailyStatsRow.Companion.safeDiv
import com.wtb.dashTracker.ui.fragment_trends.DailyStatsViewModel
import com.wtb.dashTracker.ui.fragment_trends.DailyStatsViewModel.Companion.MIN_NUM_DAYS_HOURLY_TREND
import com.wtb.dashTracker.ui.fragment_trends.DailyStatsViewModel.Companion.MIN_NUM_WEEKS
import com.wtb.dashTracker.ui.fragment_trends.TAG
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Integer.max
import java.time.DayOfWeek
import java.time.LocalDate

fun List<Cpm>.getByDate(date: LocalDate): Cpm? {
    val index = binarySearch { cpm: Cpm ->
        cpm.date.compareTo(date.endOfWeek)
    }
    return if (index >= 0)
        get(index)
    else
        null
}


fun Fragment.getDimen(@DimenRes res: Int) =
    resources.getDimension(res) / resources.displayMetrics.density


@ExperimentalCoroutinesApi
class ChartFragment : Fragment() {
    private val viewModel: DailyStatsViewModel by viewModels()
    private lateinit var binding: FragChartsBinding

    private lateinit var chartOne: LineChart
    private lateinit var chartTwo: HorizontalBarChart
    private lateinit var chartThree: BarChart

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
        initCpmChart()
        initDailyHourlyChart()
        initHourlyTrendChart()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entryList.collectLatest {
                    entries = it
                    binding.seekBarNumWeeksHourlyTrend.max = entries.size

                    if (entries.isNotEmpty()) {
                        updateDailyHourlyChart()
                        updateHourlyTrendChart()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weeklyList.collectLatest {
                    weeklies = it

                    if (weeklies.isNotEmpty()) {
                        updateHourlyTrendChart()
                    }
                    binding.seekBarNumWeeks.max = it.size

                    cpmList = it.map { w ->
                        Cpm(
                            w.weekly.date,
                            viewModel.getExpensesAndCostPerMile(w, ALL_EXPENSES).second
                        )
                    }.reversed()

                    if (cpmList.isNotEmpty()) {
                        updateCpmChart()
                        if (entries.isNotEmpty()) {
                            updateHourlyTrendChart()
                        }
                    }
                }
            }
        }
    }

    private fun initCpmChart() {
        fun SeekBar.initialize() {
            min = MIN_NUM_WEEKS
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.numWeeksTv.text = progress.toString()
                    if (cpmList.isNotEmpty())
                        updateCpmChart()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Do nothing
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Do nothing
                }
            })
            progress = MIN_NUM_WEEKS
        }

        fun LineChart.style() {
            fun XAxis.style() {
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
                textColor = getAttrColor(context, R.attr.colorTextPrimary)
                textSize = getDimen(R.dimen.text_size_sm)
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
                            getString(
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
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
                textColor = getAttrColor(context, R.attr.colorTextPrimary)
                textSize = getDimen(R.dimen.text_size_sm)
                this.axisMinimum = 0f
                setDrawBorders(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return getString(R.string.cpm_unit, value)
                    }
                }
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
        }

        chartOne = binding.chartLineCpm.apply { style() }
        binding.seekBarNumWeeks.initialize()
    }

    private fun updateCpmChart() {
        fun LineDataSet.style() {
            valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
            valueTextSize = getDimen(R.dimen.text_size_sm)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return getCurrencyString(value)
                }
            }
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawCircles(false)
            setDrawIcons(false)
            setDrawCircleHole(false)
            setDrawFilled(true)
            fillDrawable =
                ContextCompat.getDrawable(requireContext(), R.drawable.bad_to_good)
            fillAlpha = 85
            cubicIntensity = 0.2f
            valueTextSize = getDimen(R.dimen.text_size_sm)
        }

        fun getEntryList(): LineDataSet =
            cpmList.mapNotNull { cpm ->
                if (cpm.cpm !in listOf(Float.NaN, 0f))
                    Entry(cpm.date.toEpochDay().toFloat(), cpm.cpm)
                else
                    null
            }.let {
                LineDataSet(
                    it.subList(max(it.size - binding.seekBarNumWeeks.progress, 1), it.size),
                    "CPM"
                ).apply { style() }
            }

        val dataSet = getEntryList()

        chartOne.xAxis.axisMinimum = dataSet.xMin - 1
        chartOne.xAxis.axisMaximum = dataSet.xMax + 1

        chartOne.data = LineData(dataSet)

        (context as MainActivity).runOnUiThread {
            chartOne.animateY(1000)
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

        chartTwo = binding.chartBarDailyHourly.apply { style() }
    }

    private fun updateDailyHourlyChart() {
        val barSize = .4f
        val amPmBarOffset = barSize / 2

        val dataSetAm: BarDataSet =
            dailyStats.getBarDataSet(label = "AM", barColor = R.attr.colorDayHeader) { ds ->
                safeDiv(ds.amEarned, ds.amHours)?.let { hourly ->
                    ds.day?.value?.toFloat()
                        ?.let { day -> BarEntry(8 - day - amPmBarOffset, hourly) }
                }
            }

        val dataSetPm: BarDataSet =
            dailyStats.getBarDataSet(label = "PM", barColor = R.attr.colorNightHeader) { ds ->
                safeDiv(ds.pmEarned, ds.pmHours)?.let { hourly ->
                    ds.day?.value?.toFloat()
                        ?.let { day -> BarEntry(8 - day + amPmBarOffset, hourly) }
                }
            }

        chartTwo.data = BarData(dataSetPm, dataSetAm).apply { this.barWidth = barSize }

        (context as MainActivity).runOnUiThread {
            chartTwo.animateY(1000)
        }
    }

    private fun initHourlyTrendChart() {
        fun SeekBar.initialize() {
            min = MIN_NUM_DAYS_HOURLY_TREND
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.numWeeksHourlyTrend.text = progress.toString()
                    if (entries.isNotEmpty())
                        updateHourlyTrendChart()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Do nothing
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Do nothing
                }
            })
            progress = MIN_NUM_DAYS_HOURLY_TREND
        }

        fun BarChart.style() {
            fun XAxis.style() {
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
                textColor = getAttrColor(context, R.attr.colorTextPrimary)
                textSize = getDimen(R.dimen.text_size_sm)
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
                            if (date.dayOfWeek == DayOfWeek.SUNDAY)
                                getString(
                                    R.string.date_range,
                                    date.minusDays(6).format(dtfMini),
                                    date.format(dtfMini)
                                ) else {
                                ""
                            }
                        }
                    }
                }
            }

            fun YAxis.style() {
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
                textColor = getAttrColor(context, R.attr.colorTextPrimary)
                textSize = getDimen(R.dimen.text_size_sm)
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                this.axisMinimum = 0f
                labelCount = 4
                setDrawBorders(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return getString(R.string.currency_unit, value)
                    }
                }
            }

            setBackgroundColor(getColor(resources, android.R.color.transparent, null))
            description.isEnabled = false
            legend.isEnabled = false
            legend.typeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
            isHighlightPerTapEnabled = false
            isHighlightFullBarEnabled = false
            isHighlightPerDragEnabled = false
            xAxis.style()
            axisLeft.style()
            axisRight.isEnabled = false
        }

        binding.hourlyTrendButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateHourlyTrendChart()
                when (checkedId) {
                    R.id.hourly_trend_daily -> {
                        binding.labelNumWeeksHourlyTrends.text = "Num Days"
                        binding.seekBarNumWeeksHourlyTrend.apply {
                            min = MIN_NUM_DAYS_HOURLY_TREND
                            max = max(MIN_NUM_DAYS_HOURLY_TREND, entries.size)
                            progress = min
                        }
                    }
                    R.id.hourly_trend_weekly -> {
                        binding.labelNumWeeksHourlyTrends.text = "Num Weeks"
                        binding.seekBarNumWeeksHourlyTrend.apply {
                            min = MIN_NUM_WEEKS
                            max = max(MIN_NUM_WEEKS, weeklies.size)
                            progress = min
                        }
                    }
                }
            }
        }

        chartThree = binding.chartLineHourlyTrend.apply { style() }
        binding.seekBarNumWeeksHourlyTrend.initialize()
    }

    private fun updateHourlyTrendChart() {
        val regBarSize = .8f
        val dailyBarWidth = regBarSize / 2
        val weeklyBarWidth = regBarSize * 7
        val grossNetBarOffset = dailyBarWidth / 2

        fun BarDataSet.style() {
            valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
            valueTextSize = getDimen(R.dimen.text_size_sm)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return getCurrencyString(value)
                }
            }
            setDrawIcons(false)
            valueTextSize = getDimen(R.dimen.text_size_sm)
        }

        fun getEntryList(): Pair<BarDataSet, BarDataSet> =
            entries.mapNotNull {
                val hourly = it.hourly
                if (hourly != null && hourly !in listOf(Float.NaN, 0f)) {
                    val cpm =
                        if (cpmList.isNotEmpty()) cpmList.getByDate(it.date)?.cpm ?: 0f else 0f
                    val expense: Float = safeDiv((it.mileage ?: 0f) * cpm, it.totalHours) ?: 0f
                    val toFloat = it.date.toEpochDay().toFloat()
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
                    it.map { it1 -> it1.first }.reversed().subList(max(startIndex, 0), it.size)
                val subListNet =
                    it.map { it1 -> it1.second }.reversed().subList(max(startIndex, 0), it.size)
                Pair(BarDataSet(subListGross, "Gross").apply { style() },
                    BarDataSet(subListNet, "Net").apply { style() })
            }

        fun getWeeklyList(): BarDataSet = weeklies.mapNotNull {
            val hourly = it.hourly
            if (hourly != null && hourly !in listOf(Float.NaN, 0f)) {
                BarEntry(it.weekly.date.toEpochDay().toFloat(), hourly)
            } else {
                null
            }
        }.let {
            val startIndex = it.size - binding.seekBarNumWeeksHourlyTrend.progress
            val subList = it.reversed().subList(max(startIndex, 0), it.size)
            BarDataSet(subList, "Hourly by Week").apply { style() }
        }

        val dataSet: BarData = if (isDailySelected) {
            val gross = getEntryList().first.apply {
                color = getAttrColor(requireContext(), R.attr.colorPrimaryDark)
            }
            val net = getEntryList().second.apply {
                color = getAttrColor(requireContext(), R.attr.colorPrimary)
            }
            BarData(gross, net).also {
                chartThree.legend.isEnabled = true
            }
        } else {
            BarData(getWeeklyList()).also {
                chartThree.legend.isEnabled = false
            }
        }

        val xAxisOffset = if (isDailySelected) 1 else 6
        chartThree.xAxis.axisMinimum = dataSet.xMin - xAxisOffset
        chartThree.xAxis.axisMaximum = dataSet.xMax + xAxisOffset

        chartThree.apply {
            data =
                dataSet.apply {
                    this.barWidth = if (isDailySelected) dailyBarWidth else weeklyBarWidth
                }
            setVisibleXRangeMinimum(if (isDailySelected) 7f else 56f)
        }


        (context as MainActivity).runOnUiThread {
            chartThree.animateY(500)
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
}