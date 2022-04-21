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
import android.view.View
import android.widget.GridLayout
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.FragInsightsRowBinding
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getFloatString
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*


class DailyStatsRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    stats: DailyStats? = null
) : GridLayout(context, attrs) {
    private var binding: FragInsightsRowBinding

    private val day: DayOfWeek

    init {
        val v: View = inflate(context, R.layout.frag_insights_row, this)

        binding = FragInsightsRowBinding.bind(v)
        day = stats!!.day!!

        binding.dailyStatsRowDayOfWeek.text =
            day.getDisplayName(TextStyle.SHORT, Locale.US).uppercase()

        val amHourly = safeDiv(stats.amEarned, stats.amHours)
        binding.dailyStatsRowAmHourly.text =
            context.getCurrencyString(amHourly)

        val pmHourly = safeDiv(stats.pmEarned, stats.pmHours)
        binding.dailyStatsRowPmHourly.text =
            context.getCurrencyString(pmHourly)

        val amAvgDel = safeDiv(stats.amEarned, stats.amDels)
        binding.dailyStatsRowAmAvgDel.text =
            context.getCurrencyString(amAvgDel)

        binding.dailyStatsRowAmNumShifts.text =
            stats.amHours.let {
                if (it == null || it == 0f) "-" else context.getString(
                    R.string.format_hours,
                    it
                )
            }
//            stats.amNumShifts.let { if (it == 0) "-" else it.toString() }

        val pmAvgDel = safeDiv(stats.pmEarned, stats.pmDels)
        binding.dailyStatsRowPmAvgDel.text =
            context.getCurrencyString(pmAvgDel)

        val amDelsPerHr = safeDiv(stats.amDels, stats.amHours)
        binding.dailyStatsRowAmDph.text =
            context.getFloatString(amDelsPerHr)

        val pmDelsPerHr = safeDiv(stats.pmDels, stats.pmHours)
        binding.dailyStatsRowPmDph.text =
            context.getFloatString(pmDelsPerHr)

        binding.dailyStatsRowPmNumShifts.text =
            stats.pmHours.let {
                if (it == null || it == 0f) "-" else context.getString(
                    R.string.format_hours,
                    it
                )
            }
//            stats.pmNumShifts.let { if (it == 0) "-" else it.toString() }
    }

    fun addToGridLayout(grid: GridLayout, SKIP_ROWS: Int) {
        removeAllViews()
        grid.addView(binding.dailyStatsRowDayOfWeek.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS, 2)
        })
        grid.addView(binding.dailyStatsRowLabelAm.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS)
        })
        grid.addView(binding.dailyStatsRowLabelPm.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS + 1)
        })
        grid.addView(binding.dailyStatsRowAmHourly.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS)
        })
        grid.addView(binding.dailyStatsRowAmAvgDel.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS)
        })
        grid.addView(binding.dailyStatsRowAmDph.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS)
        })
        grid.addView(binding.dailyStatsRowAmNumShifts.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS)
        })
        grid.addView(binding.dailyStatsRowPmHourly.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS + 1)
        })
        grid.addView(binding.dailyStatsRowPmAvgDel.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS + 1)
        })
        grid.addView(binding.dailyStatsRowPmDph.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS + 1)
        })
        grid.addView(binding.dailyStatsRowPmNumShifts.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS + 1)
        })
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

fun DayOfWeek.toRow() = (value - 1) * 2

fun View.setGridLayoutRow(row: Int, span: Int = 1) {
    val lp = layoutParams as GridLayout.LayoutParams
    lp.rowSpec = GridLayout.spec(row, span, GridLayout.CENTER)
    layoutParams = lp
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
)