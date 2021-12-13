package com.wtb.dashTracker.extensions

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.wtb.dashTracker.R

fun Fragment.getStringOrElse(@StringRes resId: Int, ifNull: String, vararg args: Any?) =
    if (args.map { it != null }.reduce { acc, b -> acc && b }) getString(
        resId,
        *args
    ) else ifNull

fun Fragment.getCurrencyString(value: Float?): String = getStringOrElse(R.string.currency_unit, "$-", value)

