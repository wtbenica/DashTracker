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

package com.wtb.dashTracker.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.ui.fragment_trends.TAG
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.lang.Float.max
import java.time.DateTimeException
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@ExperimentalCoroutinesApi
class WeeklyBarChart @JvmOverloads constructor(
    context: Context,
    attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BarChart(context, attrSet, defStyleAttr) {
    var isWeekly: Boolean = false
        set(value) {
            field = value
            (mXAxisRenderer as WeeklyXAxisRenderer).mIsWeekly = value
        }

    override fun init() {
        super.init()
        mXAxisRenderer =
            WeeklyXAxisRenderer(mViewPortHandler, mXAxis, mLeftAxisTransformer, isWeekly)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        super.onInterceptTouchEvent(ev)
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
        return true
    }
}

@ExperimentalCoroutinesApi
class WeeklyLineChart @JvmOverloads constructor(
    context: Context,
    attrSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LineChart(context, attrSet, defStyleAttr) {
    var isWeekly: Boolean = false
        set(value) {
            field = value
            (mXAxisRenderer as WeeklyXAxisRenderer).mIsWeekly = value
        }

    override fun init() {
        super.init()
        mXAxisRenderer =
            WeeklyXAxisRenderer(mViewPortHandler, mXAxis, mLeftAxisTransformer, isWeekly)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        super.onInterceptTouchEvent(ev)
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
        return true
    }
}

@ExperimentalCoroutinesApi
class WeeklyXAxisRenderer(
    mViewPortHandler: ViewPortHandler?,
    mXAxis: XAxis?,
    mLeftAxisTransformer: Transformer?,
    isWeekly: Boolean = false
) : XAxisRenderer(mViewPortHandler, mXAxis, mLeftAxisTransformer) {
    var mIsWeekly: Boolean = isWeekly
        set(value) {
            field = value
            mAxis.granularity = if (field) 7f else 1f
        }

    override fun computeAxisValues(min: Float, max: Float) {
        if (!mIsWeekly) {
            super.computeAxisValues(min, max)
        } else {
            var newMin: Float
            try {
                newMin =
                    LocalDate.ofEpochDay(max(min, 7f).toLong()).minusDays(7L).endOfWeek.toEpochDay()
                        .toFloat()
                Log.d(TAG, "MIN $min $max")
            } catch (e: DateTimeException) {
                Log.d(TAG, "EXCEPTION MIN $min $max")
                newMin = 0f
            }
            var newMax: Float
            try {
                newMax = LocalDate.ofEpochDay(max.toLong()).endOfWeek.toEpochDay().toFloat()
                Log.d(TAG, "MAX $min $max")
            } catch (e: DateTimeException) {
                Log.d(TAG, "EXCEPTION MAX $min $max")
                newMax = 0f
            }

            val range = kotlin.math.abs(newMax - newMin).toDouble()
            val labelCount = (range / 7f).toInt()

            if (labelCount == 0 || range <= 0 || java.lang.Double.isInfinite(range)) {
                mAxis.mEntries = floatArrayOf()
                mAxis.mCenteredEntries = floatArrayOf()
                mAxis.mEntryCount = 0
                return
            }
            Log.d(TAG, "Min: $newMin Max: $newMax Range: $range Labels: $labelCount")
            // Find out how much spacing (in y value space) between axis values

            // Find out how much spacing (in y value space) between axis values
            var interval = 7.0

            // If granularity is enabled, then do not allow the interval to go below specified granularity.
            // This is used to avoid repeated values when rounding values for display.

            // If granularity is enabled, then do not allow the interval to go below specified granularity.
            // This is used to avoid repeated values when rounding values for display.
            if (mAxis.isGranularityEnabled)
                interval =
                    if (interval < mAxis.granularity) mAxis.granularity.toDouble() else interval

            // Normalize interval
            val intervalMagnitude =
                Utils.roundToNextSignificant(10.0.pow(log10(interval))).toDouble()
            val intervalSigDigit = (interval / intervalMagnitude).toInt()
            if (intervalSigDigit > 5) {
                // Use one order of magnitude higher, to avoid intervals like 0.9 or 90
                interval = floor(10 * intervalMagnitude)
            }

            var n = if (mAxis.isCenterAxisLabelsEnabled) 1 else 0

            // force label count
            if (mAxis.isForceLabelsEnabled) {
                interval = (range.toFloat() / (labelCount - 1).toFloat()).toDouble()
                mAxis.mEntryCount = labelCount
                if (mAxis.mEntries.size < labelCount) {
                    // Ensure stops contains at least numStops elements.
                    mAxis.mEntries = FloatArray(labelCount)
                }
                var v = newMin
                for (i in 0 until labelCount) {
                    mAxis.mEntries[i] = v
                    v += interval.toFloat()
                }
                n = labelCount

                // no forced count
            } else {
                var first: Double =
                    if (interval == 0.0) 0.0 else (kotlin.math.ceil(newMin / interval) * interval).adjustToEOW()
                if (mAxis.isCenterAxisLabelsEnabled) {
                    first -= interval
                }
                val last: Double =
                    if (interval == 0.0) 0.0 else (Utils.nextUp(floor(newMax / interval) * interval)
                        .adjustToEOW())
                var f: Double
                if (interval != 0.0) {
                    f = first
                    while (f <= last) {
                        ++n
                        f += interval
                    }
                }
                mAxis.mEntryCount = n
                if (mAxis.mEntries.size < n) {
                    // Ensure stops contains at least numStops elements.
                    mAxis.mEntries = FloatArray(n)
                }
                f = first
                var i = 0
                while (i < n) {
                    if (f == 0.0) // Fix for negative zero case (Where value == -0.0, and 0.0 == -0.0)
                        f = 0.0
                    mAxis.mEntries[i] = f.toFloat()
                    f += interval
                    ++i
                }
            }

            // set decimals

            // set decimals
            if (interval < 1) {
                mAxis.mDecimals = kotlin.math.ceil(-log10(interval)).toInt()
            } else {
                mAxis.mDecimals = 0
            }

            if (mAxis.isCenterAxisLabelsEnabled) {
                if (mAxis.mCenteredEntries.size < n) {
                    mAxis.mCenteredEntries = FloatArray(n)
                }
                val offset = interval.toFloat() / 2f
                for (i in 0 until n) {
                    mAxis.mCenteredEntries[i] = mAxis.mEntries[i] + offset
                }
            }

            computeSize()
        }
    }
}

fun Double.adjustToEOW(): Double {
    return LocalDate.ofEpochDay(toLong()).endOfWeek.toEpochDay().toDouble()
}
