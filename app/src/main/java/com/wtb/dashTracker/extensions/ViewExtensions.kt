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
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
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

private val View.targetHeight: Int
    get() {
        val matchParentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec((parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        return measuredHeight
    }

private const val ANIMATION_DURATION = 300L

fun View.reveal(onComplete: (() -> Unit)? = null) {
    animation?.cancel()
    clearAnimation()
    layoutParams.height = max(1, layoutParams.height)
    visibility = VISIBLE

    var finishedNormally = false
    val expandAnimation =
        object : Animation() {
            override fun willChangeBounds(): Boolean = true

            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                this@reveal.apply {

                    layoutParams.height = (targetHeight * interpolatedTime).toInt()

                    finishedNormally = interpolatedTime >= 1f
                    requestLayout()
                }
            }
        }.apply {
            duration = ANIMATION_DURATION
            fillAfter = true

            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Do nothing
                }

                override fun onAnimationEnd(animation: Animation?) {
                    this@reveal.apply {
                        layoutParams.height = WRAP_CONTENT
                        clearAnimation()
                        requestLayout()
                        if (finishedNormally) {
                            onComplete?.invoke()
                        }
                    }
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // Do nothing
                }
            })
        }

    startAnimation(expandAnimation)
}

fun View.revealToHeight(targetHeight: Int? = WRAP_CONTENT, targetWidth: Int? = MATCH_PARENT) {
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
            duration = ANIMATION_DURATION
        }

        startAnimation(animation)
    }
}

fun View.revealToHeightIfTrue(
    shouldExpand: Boolean = true,
    toHeight: Int? = null,
    toWidth: Int? = null
) {
    if (shouldExpand && !isVisible) {
        revealToHeight(
            targetHeight = toHeight,
            targetWidth = toWidth
        )
    } else if (!shouldExpand && isVisible) {
        collapse()
    }
}

fun View.collapse(onComplete: (() -> Unit)? = null) {
    animation?.cancel()
    clearAnimation()
    val initHeight = measuredHeight

    val collapseAnimation = object : Animation() {
        override fun willChangeBounds(): Boolean = true

        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            this@collapse.apply {
                layoutParams.height = initHeight - (initHeight * interpolatedTime).toInt()

                requestLayout()
            }
        }
    }.apply {
        duration = ANIMATION_DURATION

        setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                // Do nothing
            }

            override fun onAnimationEnd(animation: Animation?) {
                this@collapse.apply {
                    layoutParams.height = 0
                    visibility = GONE
                    clearAnimation()
                    requestLayout()
                    onComplete?.invoke()
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {
                // Do nothing
            }

        })
    }

    if (initHeight > 0)
        startAnimation(collapseAnimation)
    else {
        visibility = GONE
        onComplete?.invoke()
    }
}

fun View.setVisibleIfTrue(boolean: Boolean) {
    visibility = if (boolean) VISIBLE else GONE
    requestLayout()
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
    colorAnimation.duration = ANIMATION_DURATION

    colorAnimation.addUpdateListener {
        if (it.animatedValue is Int) {
            val color = it.animatedValue as Int
            setBackgroundColor(color)
        }
    }

    colorAnimation.start()
}

fun View.transitionBackgroundTo(@AttrRes to: Int) {
    val initHeight = measuredHeight

    val bgColor = when (background) {
        is ColorDrawable -> (background as ColorDrawable?)?.color
        is MaterialShapeDrawable -> (background as MaterialShapeDrawable?)?.fillColor?.defaultColor
        else -> null
    }
    val colorTo = MaterialColors.getColor(this, to)
    val colorFrom: Int = bgColor ?: colorTo

    val colorAnimation: ValueAnimator =
        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            duration = ANIMATION_DURATION
        }

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