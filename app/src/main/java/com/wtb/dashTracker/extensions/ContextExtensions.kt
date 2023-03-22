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

package com.wtb.dashTracker.extensions

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import com.wtb.dashTracker.R
import java.time.LocalTime

fun Context.getStringOrElse(@StringRes resId: Int, ifInvalid: String, vararg args: Any?): String =
    if (args.map { isValid(it) }.reduce { acc, b -> acc && b })
        getString(resId, *args)
    else
        ifInvalid

private fun isValid(it: Any?) = it != null && (it !is Float || it.isFinite())

fun getElapsedHours(seconds: Long?): String =
    if (seconds == null || seconds < 0) {
        "—"
    } else {
        val mHours = seconds / 3600
        val mMinutes = (seconds - 3600 * mHours) / 60

        StringBuilder().run {
            if (mHours > 0L) {
                append("$mHours".format("%2d"))
                append("h")

                if (mMinutes > 0L) {
                    append(" ")
                }
            }
            if (mMinutes > 0L) {
                append("$mMinutes".format(" %2d"))
                append("m")
            }
            if (mHours == 0L && mMinutes == 0L) {
                append("<1m")
            }
            toString()
        }
    }


/**
 * @return '$ -' if [value] is null, 0, NaN, or Infinite, else [value] formatted as '$%.2f'
 */
fun Context.getCurrencyString(value: Float?): String =
    if (value == null || value == 0f || value.isNaN() || value.isInfinite())
        getString(R.string.blank_currency)
    else
        getStringOrElse(R.string.currency_fmt, "-", value)

/**
 * @return '-' if [value] is null, 0, NaN, or Infinite, else [value] formatted as '$%.2f'
 */
fun Context.getFloatString(value: Float?): String =
    if (value == null || value == 0f || value.isNaN() || value.isInfinite())
        getString(R.string.blank_float)
    else
        getStringOrElse(R.string.float_fmt, "-", value)

fun Context.getDimen(@DimenRes res: Int): Float =
    resources.getDimension(res) / resources.displayMetrics.density

/**
 * @return if [start] and [end] are null, '', else formats them with [dtfTime], or '' for either if
 * it is null. E.g. '2:13 PM - 4:15 PM', '10:11 AM - ', ' - 3:15 PM'
 */
fun Context.getHoursRangeString(start: LocalTime?, end: LocalTime?): String =
    if (start == null && end == null)
        ""
    else
        getString(
            R.string.time_range,
            start?.format(dtfTime) ?: "",
            end?.format(dtfTime) ?: ""
        )

@ColorInt
fun Context.getAttrColor(@AttrRes attr: Int): Int {
    val tv = TypedValue()
    theme.resolveAttribute(attr, tv, true)
    return tv.data
}