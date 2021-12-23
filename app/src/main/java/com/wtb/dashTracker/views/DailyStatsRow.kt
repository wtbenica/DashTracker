package com.wtb.dashTracker.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.GridLayout
import androidx.annotation.StringRes
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DailyStatsRowBinding
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

fun Context.getStringOrElse(@StringRes resId: Int, formatArg: Any?): String =
    formatArg?.let {
        getString(resId, it)
    } ?: "-"


class DailyStatsRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    stats: DailyStats? = null
) : GridLayout(context, attrs) {
    private var binding: DailyStatsRowBinding

    private val day: DayOfWeek

    init {
        val v: View = inflate(context, R.layout.daily_stats_row, this)

        binding = DailyStatsRowBinding.bind(v)
        day = stats!!.day!!

        binding.dailyStatsRowDayOfWeek.text =
            day.getDisplayName(TextStyle.SHORT, Locale.US).uppercase()

        val c = stats.amDels
        val b = stats.amHours
        val a = stats.amEarned
        Log.d(TAG, "HOURLY: ${stats.day} ${stats.amHours} ${stats.amEarned} ${safeDiv(a, b)}")
        Log.d(TAG, "PER DEL: ${stats.day} ${stats.amHours} ${stats.amDels} ${safeDiv(a, c)}")
        val amHourly = safeDiv(a, b)
        binding.dailyStatsRowAmHourly.text =
            context.getStringOrElse(R.string.currency_unit, amHourly)

        val pmHourly = safeDiv(stats.pmEarned, stats.pmHours)
        binding.dailyStatsRowPmHourly.text =
            context.getStringOrElse(R.string.currency_unit, pmHourly)

        val amAvgDel = safeDiv(stats.amEarned, stats.amDels)
        binding.dailyStatsRowAmAvgDel.text =
            context.getStringOrElse(R.string.currency_unit, amAvgDel)

        binding.dailyStatsRowAmNumShifts.text =
            stats.amNumShifts.let { if (it == 0) "-" else it.toString() }


        val pmAvgDel = safeDiv(stats.pmEarned, stats.pmDels)
        binding.dailyStatsRowPmAvgDel.text =
            context.getStringOrElse(R.string.currency_unit, pmAvgDel)

        val amDelsPerHr = safeDiv(stats.amDels, stats.amHours)
        binding.dailyStatsRowAmDph.text =
            context.getStringOrElse(R.string.float_fmt, amDelsPerHr)

        val pmDelsPerHr = safeDiv(stats.pmDels, stats.pmHours)
        binding.dailyStatsRowPmDph.text =
            context.getStringOrElse(R.string.float_fmt, pmDelsPerHr)

        binding.dailyStatsRowPmNumShifts.text =
            stats.pmNumShifts.let { if (it == 0) "-" else it.toString() }
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
        private const val TAG = APP + "DailyStatsRow"

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