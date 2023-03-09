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
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.core.view.isVisible
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

fun View.revealIfTrue(shouldShow: Boolean = true, onComplete: (() -> Unit)? = null) {
    if (shouldShow && !isVisible) {
        expand {
            onComplete?.invoke()
        }
    } else if (!shouldShow && isVisible) {
        collapse {
            onComplete?.invoke()
        }
    }
}

fun View.expandToIfTrue(shouldExpand: Boolean = true, toHeight: Int? = null, toWidth: Int? = null) {
    if (shouldExpand && !isVisible) {
        expandTo(
            targetHeight = toHeight,
            targetWidth = toWidth
        )
    } else if (!shouldExpand && isVisible) {
        collapse()
    }
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

fun View.expandTo(targetHeight: Int? = WRAP_CONTENT, targetWidth: Int? = MATCH_PARENT) {
    val mTargetHeight = targetHeight ?: WRAP_CONTENT
    val mTargetWidth = targetWidth ?: MATCH_PARENT

    if (!isVisible) {
        val specWidth: Int
        val specWidthMode: Int
        if (mTargetWidth == MATCH_PARENT) {
            specWidth = (parent as View).width
            specWidthMode = View.MeasureSpec.EXACTLY
        } else if (mTargetWidth == WRAP_CONTENT) {
            specWidth = 0
            specWidthMode = View.MeasureSpec.UNSPECIFIED
        } else {
            specWidth = mTargetWidth
            specWidthMode = View.MeasureSpec.EXACTLY
        }

        val specHeight: Int
        val specHeightMode: Int
        if (mTargetHeight == MATCH_PARENT) {
            specHeight = (parent as View).width
            specHeightMode = View.MeasureSpec.EXACTLY
        } else if (mTargetHeight == WRAP_CONTENT) {
            specHeight = 0
            specHeightMode = View.MeasureSpec.UNSPECIFIED
        } else {
            specHeight = mTargetHeight
            specHeightMode = View.MeasureSpec.EXACTLY
        }

        val widthMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(specWidth, specWidthMode)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(specHeight, specHeightMode)
        measure(widthMeasureSpec, heightMeasureSpec)
        val toHeight = measuredHeight
        val toWidth = measuredWidth

        layoutParams.height = max(1, layoutParams.height)
        visibility = VISIBLE

        val animation = object : Animation() {
            override fun willChangeBounds(): Boolean = true

            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                layoutParams.height = if (interpolatedTime >= 1f) {
                    toHeight
                } else {
                    (toHeight * interpolatedTime).toInt()
                }
                layoutParams.width = if (interpolatedTime >= 1f) {
                    toWidth
                } else {
                    (toWidth * interpolatedTime).toInt()
                }
                requestLayout()
            }
        }.apply {
            duration = (toHeight / context.resources.displayMetrics.density).toLong()
        }

        startAnimation(animation)
    }
}

fun View.collapse(onComplete: (() -> Unit)? = null) {
    val initHeight = measuredHeight

    val animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            layoutParams.height = if (interpolatedTime >= 1f) {
                visibility = GONE
                onComplete?.invoke()
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

fun View.focusAndShowKeyboard() {
    fun showKeyboard() {
        if (isFocused) {
            post {
                val ime =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                ime.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    requestFocus()
    if (hasWindowFocus()) {
        showKeyboard()
    } else {
        viewTreeObserver.addOnWindowFocusChangeListener(
            object : ViewTreeObserver.OnWindowFocusChangeListener {
                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    if (hasFocus) {
                        showKeyboard()
                        viewTreeObserver.removeOnWindowFocusChangeListener(this)
                    }
                }
            }
        )
    }
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

fun View.toggleExpand(
    onCompleteCollapse: (() -> Unit)? = null,
    onCompleteExpand: (() -> Unit)? = null
) {
    if (isVisible) {
        collapse(onCompleteCollapse)
    } else {
        expand(onCompleteExpand)
    }
}

