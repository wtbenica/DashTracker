package com.wtb.dashTracker.extensions

import kotlin.math.roundToInt

fun Float.truncate(n: Int): String =
    ((this * Math.pow(10.0, n.toDouble())).roundToInt().toFloat() / Math.pow(
        10.0, n.toDouble()
    )).toString()