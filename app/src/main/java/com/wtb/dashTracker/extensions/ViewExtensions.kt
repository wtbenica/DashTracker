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

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.annotation.AttrRes
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import java.lang.Integer.max

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

fun View.expand(onComplete: (() -> Unit)? = null) {
    val matchParentMeasureSpec =
        View.MeasureSpec.makeMeasureSpec((parent as View).width, View.MeasureSpec.EXACTLY)
    val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    measure(matchParentMeasureSpec, wrapContentMeasureSpec)
    val targetHeight = measuredHeight

    layoutParams.height = max(1, layoutParams.height)
    visibility = VISIBLE

    val animation = object : Animation() {
        override fun willChangeBounds(): Boolean = true

        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            layoutParams.height = if (interpolatedTime >= 1f) {
                onComplete?.invoke()
                WRAP_CONTENT
            } else {
                (targetHeight * interpolatedTime).toInt()
            }
            requestLayout()
        }
    }.apply {
        duration = (targetHeight / context.resources.displayMetrics.density).toLong()
    }

    startAnimation(animation)
}

fun View.collapse(onComplete: (() -> Unit)? = null) {
    val initHeight = measuredHeight

    val animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            layoutParams.height = if (interpolatedTime >= 1f) {
                onComplete?.invoke()
                visibility = GONE
                0
            } else {
                initHeight - (initHeight * interpolatedTime).toInt()
            }
            requestLayout()
        }

        override fun willChangeBounds(): Boolean = true
    }.apply {
        duration = (initHeight / context.resources.displayMetrics.density).toLong()
    }

    startAnimation(animation)
}

fun View.transitionBackground(@AttrRes from: Int, @AttrRes to: Int) {
    val initHeight = measuredHeight

    val colorFrom = MaterialColors.getColor(this, from)
    val colorTo = MaterialColors.getColor(this, to)
    val colorAnimation: ValueAnimator = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
    colorAnimation.duration = (initHeight / context.resources.displayMetrics.density).toLong()

    colorAnimation.addUpdateListener {
        if (it.animatedValue is Int) {
            val color = it.animatedValue as Int
            setBackgroundColor(color)
        }
    }

    colorAnimation.start()
}

fun MaterialButton.rotateDown() {
    val initRotation = rotation

    val animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            rotation = if (interpolatedTime == 1f) {
                0f
            } else {
                initRotation - (180 * interpolatedTime)
            }
        }
    }.apply {
        duration = 300L
    }

    startAnimation(animation)
}

fun MaterialButton.rotateUp() {
    val initRotation = rotation

    val animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            rotation = if (interpolatedTime == 1f) {
                180f
            } else {
                initRotation - (180 * interpolatedTime)
            }
        }
    }.apply {
        duration = 300L
    }

    startAnimation(animation)
}