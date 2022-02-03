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

fun Fragment.getStringOrElse(@StringRes resId: Int, ifNull: String, vararg args: Any?) =
    if (args.map { it != null }.reduce { acc, b -> acc && b }) getString(
        resId,
        *args
    ) else ifNull

fun Fragment.getCurrencyString(value: Float?): String {
    val newValue = if (value != null && value <= 0f) null else value
    return getStringOrElse(R.string.currency_unit, "-", newValue)
}

fun Fragment.getMileageString(value: Float): String = getString(R.string.mileage_fmt, value)

