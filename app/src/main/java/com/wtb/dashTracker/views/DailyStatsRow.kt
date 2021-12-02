package com.wtb.dashTracker.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TableRow
import androidx.annotation.StringRes
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.R
import com.wtb.dashTracker.databinding.DailyStatsRowBinding
import java.time.DayOfWeek

fun Context.getStringOrElse(@StringRes ifNull: Int, @StringRes resId: Int, formatArg: Any?) =
    if (formatArg == null) {
        getString(ifNull)
    } else {
        getString(resId, formatArg)
    }


class DailyStatsRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    stats: DailyStats? = null
) : TableRow(context, attrs) {
    private var binding: DailyStatsRowBinding

    init {
        View.inflate(context, R.layout.daily_stats_row, this)

        binding = DailyStatsRowBinding.bind(this)

        binding.dailyStatsRowDayOfWeek.text = stats?.day?.name

        val amHourly = safeDiv(stats?.amEarned, stats?.amHours)
        binding.dailyStatsRowAmHourly.text =
            context.getStringOrElse(R.string.hourly_rate_null, R.string.hourly_rate, amHourly)

        val pmHourly = safeDiv(stats?.pmEarned, stats?.pmHours)
        binding.dailyStatsRowPmHourly.text =
            context.getStringOrElse(R.string.hourly_rate_null, R.string.hourly_rate, pmHourly)

        val amAvgDel = safeDiv(stats?.amEarned, stats?.amDels)
        binding.dailyStatsRowAmAvgDelivery.text =
            context.getStringOrElse(R.string.avg_delivery_null, R.string.avg_delivery, amAvgDel)

        val pmAvgDel = safeDiv(stats?.pmEarned, stats?.pmDels)
        binding.dailyStatsRowPmAvgDelivery.text =
            context.getStringOrElse(R.string.avg_delivery_null, R.string.avg_delivery, pmAvgDel)

        val amDelsPerHr = safeDiv(stats?.amDels, stats?.amHours)
        binding.dailyStatsRowAmDelsPerHour.text =
            context.getStringOrElse(
                R.string.dels_per_hour_null,
                R.string.dels_per_hour,
                amDelsPerHr
            )

        val pmDelsPerHr = safeDiv(stats?.pmDels, stats?.pmHours)
        binding.dailyStatsRowPmDelsPerHour.text =
            context.getStringOrElse(
                R.string.dels_per_hour_null,
                R.string.dels_per_hour,
                pmDelsPerHr
            )
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

data class DailyStats(
    val day: DayOfWeek? = null,
    val amHours: Float? = null,
    val pmHours: Float? = null,
    val amEarned: Float? = null,
    val pmEarned: Float? = null,
    val amDels: Float? = null,
    val pmDels: Float? = null,
)