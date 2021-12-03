package com.wtb.dashTracker.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout
import androidx.annotation.StringRes
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DailyStatsRowBinding
import java.time.DayOfWeek

fun Context.getStringOrElse(@StringRes resId: Int, formatArg: Any?): String =
    formatArg?.let {
        getString(resId, it)
    } ?: " - "


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

        binding.dailyStatsRowDayOfWeek.text = day.name

        val amHourly = safeDiv(stats.amEarned, stats.amHours)
        binding.dailyStatsRowAmHourly.text = context.getStringOrElse(R.string.currency_unit, amHourly)

        val pmHourly = safeDiv(stats.pmEarned, stats.pmHours)
        binding.dailyStatsRowPmHourly.text = context.getStringOrElse(R.string.currency_unit, pmHourly)

        val amAvgDel = safeDiv(stats.amEarned, stats.amDels)
        binding.dailyStatsRowAmAvgDelivery.text = context.getStringOrElse(R.string.currency_unit, amAvgDel)

        val pmAvgDel = safeDiv(stats.pmEarned, stats.pmDels)
        binding.dailyStatsRowPmAvgDelivery.text = context.getStringOrElse(R.string.currency_unit, pmAvgDel)

        val amDelsPerHr = safeDiv(stats.amDels, stats.amHours)
        binding.dailyStatsRowAmDelsPerHour.text = context.getStringOrElse(R.string.float_fmt, amDelsPerHr)

        val pmDelsPerHr = safeDiv(stats.pmDels, stats.pmHours)
        binding.dailyStatsRowPmDelsPerHour.text = context.getStringOrElse(R.string.float_fmt, pmDelsPerHr)
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
        grid.addView(binding.dailyStatsRowAmAvgDelivery.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS)
        })
        grid.addView(binding.dailyStatsRowAmDelsPerHour.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS)
        })
        grid.addView(binding.dailyStatsRowPmHourly.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS + 1)
        })
        grid.addView(binding.dailyStatsRowPmAvgDelivery.apply {
            setGridLayoutRow(day.toRow() + SKIP_ROWS + 1)
        })
        grid.addView(binding.dailyStatsRowPmDelsPerHour.apply {
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
)