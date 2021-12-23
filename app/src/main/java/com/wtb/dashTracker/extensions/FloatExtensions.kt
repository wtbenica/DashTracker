package com.wtb.dashTracker.extensions

import kotlin.math.pow
import kotlin.math.roundToInt

fun Float.truncate(n: Int): String =
    ((this * 10.0.pow(n.toDouble())).roundToInt().toFloat() / 10.0.pow(n.toDouble())).toString()