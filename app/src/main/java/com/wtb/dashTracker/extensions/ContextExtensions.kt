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
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import com.wtb.dashTracker.R

fun Context.getStringOrElse(@StringRes resId: Int, ifNull: String, vararg args: Any?) =
    if (args.map { it != null }.reduce { acc, b -> acc && b })
        getString(resId, *args)
    else
        ifNull

fun getElapsedHours(seconds: Long): String {
    val mHours = seconds / 3600
    val mMinutes = (seconds - 3600 * mHours) / 60

    return StringBuilder().run {
        if (mHours > 0L) {
            append("$mHours".format("%2d"))
            append("h")
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

fun Context.getCurrencyString(value: Float?): String =
    if (value == null || value == 0f || value.isNaN() || value.isInfinite())
        getString(R.string.blank_currency)
    else
        getStringOrElse(R.string.currency_unit, "-", value)

fun Context.getFloatString(value: Float?): String =
    if (value == null || value == 0f || value.isNaN() || value.isInfinite())
        getString(R.string.blank_float)
    else
        getStringOrElse(R.string.float_fmt, "-", value)

fun Context.getDimen(@DimenRes res: Int) =
    resources.getDimension(res) / resources.displayMetrics.density
