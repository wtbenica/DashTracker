package com.wtb.dashTracker.extensions

import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE

fun View.isTouchTarget(ev: MotionEvent?): Boolean {
    val x = ev?.x?.toInt()
    val y = ev?.y?.toInt()
    val coordinates = intArrayOf(0, 0)
    getLocationOnScreen(coordinates)
    val left = coordinates[0]
    val top = coordinates[1]
    val right = left + measuredWidth
    val bottom = top + measuredHeight
    return x in left..right && y in top..bottom
}

fun View.setVisibleIfTrue(boolean: Boolean) {
    visibility = if (boolean) VISIBLE else GONE
}