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

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.wtb.dashTracker.R
import java.time.LocalTime


fun Fragment.getStringOrElse(@StringRes resId: Int, ifNull: String, vararg args: Any?) =
    if (args.map { it != null }.reduce { acc, b -> acc && b })
        getString(resId, *args)
    else
        ifNull

fun Fragment.getCurrencyString(value: Float?): String =
    if (value == null || value == 0f || value.isNaN() || value.isInfinite())
        getString(R.string.blank_currency)
    else
        getStringOrElse(R.string.currency_unit, "-", value)

fun Fragment.getFloatString(value: Float?): String =
    if (value == null || value == 0f || value.isNaN() || value.isInfinite())
        getString(R.string.blank_float)
    else
        getStringOrElse(R.string.float_fmt, "-", value)

fun Fragment.getMileageString(value: Float): String = getString(R.string.mileage_fmt, value)

fun Fragment.getHoursRangeString(start: LocalTime?, end: LocalTime?): String =
    if (start == null && end == null)
        ""
    else
        getString(
            R.string.time_range,
            start?.format(dtfTime) ?: "",
            end?.format(dtfTime) ?: ""
        )

fun Fragment.getOdometerRangeString(start: Float?, end: Float?): String =
    if (start == null && end == null)
        ""
    else
        getString(R.string.odometer_range, start, end)




