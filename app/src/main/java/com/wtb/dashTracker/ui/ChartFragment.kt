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
import android.widget.SeekBar
import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.wtb.dashTracker.databinding.FragChartsBinding
import com.wtb.dashTracker.extensions.dtfMini
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.repository.DeductionType.ALL_EXPENSES
import com.wtb.dashTracker.ui.fragment_trends.DailyStats
import com.wtb.dashTracker.ui.fragment_trends.DailyStatsRow.Companion.safeDiv
import com.wtb.dashTracker.ui.fragment_trends.DailyStatsViewModel
import com.wtb.dashTracker.ui.fragment_trends.DailyStatsViewModel.Companion.MIN_NUM_WEEKS
import com.wtb.dashTracker.ui.fragment_trends.TAG
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Integer.max
import java.time.DayOfWeek
import java.time.LocalDate

fun Fragment.getDimen(@DimenRes res: Int) =
    resources.getDimension(res) / resources.displayMetrics.density


/**
 * A simple [Fragment] subclass.
 * Use the [ChartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@ExperimentalCoroutinesApi
class ChartFragment : Fragment() {
    private val viewModel: DailyStatsViewModel by viewModels()
    private lateinit var binding: FragChartsBinding

    private lateinit var chartOne: LineChart
    private lateinit var chartTwo: HorizontalBarChart

    private var cpmList = listOf<Cpm>()
    private var entries = listOf<DashEntry>()

    private val dailyStats: List<DailyStats>
        get() = collectDailyStats()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragChartsBinding.bind(view)

        initCpmChart()
        initBarChart()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "Trying to get some new entries!")
                viewModel.entryList.collectLatest {
                    Log.d(TAG, "Just got some new entries!")
                    entries = it
                    updateDailyHourlyChart()
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "Trying to get some new weeklies!")
                viewModel.weeklyList.collectLatest {
                    Log.d(TAG, "Just got some new weeklies!")
                    val numWeeklies = it.size
                    binding.seekBarNumWeeks.max = numWeeklies
                    cpmList = it.map { w ->
                        Cpm(
                            w.weekly.date,
                            viewModel.getExpensesAndCostPerMile(w, ALL_EXPENSES).second
                        )
                    }.reversed()
                    if (cpmList.isNotEmpty())
                        updateCpmChart()
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
                setMaxVisibleValueCount(8)
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
                textColor = getAttrColor(context, R.attr.colorTextPrimary)
                textSize = getDimen(R.dimen.text_size_sm)
                position = XAxis.XAxisPosition.BOTTOM
                setCenterAxisLabels(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        val date = LocalDate.ofEpochDay(value.toLong())
                        return date.format(dtfMini)
                    }
                }
            }

            fun YAxis.style() {
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
                textColor = getAttrColor(context, R.attr.colorTextPrimary)
                textSize = getDimen(R.dimen.text_size_sm)
                setDrawGridLines(false)
                this.axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return getString(R.string.cpm_unit, value)
                    }
                }
            }

            setTouchEnabled(false)
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

        chartOne.data = LineData(dataSet)

        (context as MainActivity).runOnUiThread {
            chartOne.animateY(1000)
        }
    }

    private fun initBarChart() {
        fun HorizontalBarChart.style() {
            fun XAxis.style() {
                typeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
                textColor = getAttrColor(context, R.attr.colorTextPrimary)
                textSize = getDimen(R.dimen.text_size_sm)
                position = XAxis.XAxisPosition.BOTTOM
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
                axisMinimum = 0f
            }

            setTouchEnabled(false)
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

    private fun List<DailyStats>.getBarDataSet(
        label: String, @AttrRes barColor: Int, func: (DailyStats) -> BarEntry?
    ): BarDataSet {
        val barEntries: List<BarEntry> = mapNotNull { func(it) }
        val dataSet = BarDataSet(barEntries, label)
        dataSet.style(barColor)
        return dataSet
    }

    private fun BarDataSet.style(@AttrRes barColor: Int) {
        valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.lalezar)
        color = getAttrColor(requireContext(), barColor)
        valueTextSize = getDimen(R.dimen.text_size_sm)
        valueFormatter = object : ValueFormatter() {
            override fun getBarLabel(barEntry: BarEntry?): String {
                return getCurrencyString(barEntry?.y)
            }
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
}