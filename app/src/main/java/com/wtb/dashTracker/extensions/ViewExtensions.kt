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
import android.view.View.*
import android.view.ViewGroup
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
import com.wtb.dashTracker.ui.activity_main.debugLog
import com.wtb.dashTracker.views.ExpandableView
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState.COLLAPSED
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState.EXPANDED
import com.wtb.dashTracker.views.ExpandableView.Companion.marginLayoutParams
import java.lang.Integer.max
import kotlin.math.abs
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

internal const val ANIMATION_DURATION = 300L
private const val COLOR_TRANSITION_ANIMATION = 100L

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
            MeasureSpec.makeMeasureSpec((parent as View).width, MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec =
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        return measuredHeight
    }

fun View.showOrHide(shouldShow: Boolean, animate: Boolean, elseVisibility: Int = INVISIBLE): Unit =
    if (animate && this is ExpandableView) {
        this.revealIfTrue(shouldShow = shouldShow, endVisibility = elseVisibility)
    } else {
        this.setVisibleIfTrue(shouldShow, elseVisibility)
    }

fun View.reveal(onComplete: (() -> Unit)? = null) {
    animation?.cancel()
    clearAnimation()
    val lp = layoutParams
    lp.height = max(1, height)
    layoutParams = lp

    visibility = VISIBLE

    val startHeight = height
    val endHeight = targetHeight
    val heightDuration = endHeight - startHeight

    val expandAnimation =
        object : Animation() {
            override fun willChangeBounds(): Boolean = true

            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                this@reveal.apply {
                    val lp1 = layoutParams
                    lp1.height = (startHeight + heightDuration * interpolatedTime).toInt()
                    layoutParams = lp1

                    requestLayout()
                }
            }
        }.apply {
            duration = ANIMATION_DURATION * abs(heightDuration) / endHeight
            fillAfter = true

            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Do nothing
                }

                override fun onAnimationEnd(animation: Animation?) {
                    this@reveal.apply {
                        val lp2 = layoutParams
                        lp2.height = WRAP_CONTENT
                        layoutParams = lp2

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

    startAnimation(expandAnimation)
}

fun View.animateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)? = null) {
    animation?.cancel()
    clearAnimation()
    if ((targetHeight == 0 && height == 0) || (targetWidth == 0 && width == 0)) {
        visibility = INVISIBLE
        val lp = layoutParams
        targetHeight?.let { lp.height = it }
        targetWidth?.let { lp.width = it }
        layoutParams = lp
    } else {
        var lp = layoutParams
        lp.height = max(1, layoutParams.height)
        layoutParams = lp

        visibility = VISIBLE

        var widthMeasureSpec = layoutParams.width
        targetWidth?.let {
            val specWidth: Int
            val specWidthMode: Int
            when (targetWidth) {
                MATCH_PARENT -> {
                    specWidth = (parent as View).width
                    specWidthMode = MeasureSpec.EXACTLY
                }
                WRAP_CONTENT -> {
                    specWidth = 0
                    specWidthMode = MeasureSpec.UNSPECIFIED
                }
                else -> {
                    specWidth = targetWidth
                    specWidthMode = MeasureSpec.EXACTLY
                }
            }

            widthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, specWidthMode)
        }

        var heightMeasureSpec: Int = layoutParams.height
        targetHeight?.let {
            val specHeight: Int
            val specHeightMode: Int
            when (targetHeight) {
                MATCH_PARENT -> {
                    specHeight = (parent as View).width
                    specHeightMode = MeasureSpec.EXACTLY
                }
                WRAP_CONTENT -> {
                    specHeight = 0
                    specHeightMode = MeasureSpec.UNSPECIFIED
                }
                else -> {
                    specHeight = targetHeight
                    specHeightMode = MeasureSpec.EXACTLY
                }
            }

            heightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, specHeightMode)
        }

        measure(widthMeasureSpec, heightMeasureSpec)

        val initHeight = height
        val initWidth = width

        val deltaHeight = measuredHeight - initHeight
        val deltaWidth = measuredWidth - initWidth

        val animation = object : Animation() {
            override fun willChangeBounds(): Boolean = true

            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                lp = layoutParams
                lp.height = (initHeight + deltaHeight * interpolatedTime).toInt()
                lp.width = (initWidth + deltaWidth * interpolatedTime).toInt()
                layoutParams = lp

                requestLayout()
            }
        }.apply {
            duration = ANIMATION_DURATION

            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Do nothing
                }

                override fun onAnimationEnd(animation: Animation?) {
                    this@animateTo.apply {
                        lp = layoutParams
                        lp.height = initHeight + deltaHeight
                        lp.width = initWidth + deltaWidth
                        layoutParams = lp

                        clearAnimation()
                        if (height == 0 || width == 0) {
                            visibility = INVISIBLE
                        }
                        requestLayout()
                        onComplete?.invoke()
                    }
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // Do nothing
                }
            })
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
        animateTo(
            targetHeight = toHeight,
            targetWidth = toWidth,
        )
    } else if (!shouldExpand && isVisible) {
        collapse()
    }
}

fun View.collapse(endVisibility: Int = INVISIBLE, onComplete: (() -> Unit)? = null) {
    animation?.cancel()
    clearAnimation()

    val startHeight = measuredHeight
    val endHeight = 0
    val heightDuration = endHeight - startHeight

    val collapseAnimation = object : Animation() {
        override fun willChangeBounds(): Boolean = true

        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            this@collapse.apply {
                val lp = layoutParams
                lp.height = (startHeight + heightDuration * interpolatedTime).toInt()
                layoutParams = lp
                requestLayout()
            }
        }
    }.apply {
        duration = if (startHeight == 0) {
            ANIMATION_DURATION
        } else {
            ANIMATION_DURATION * abs(heightDuration) / startHeight
        }

        setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                // Do nothing
            }

            override fun onAnimationEnd(animation: Animation?) {
                this@collapse.apply {
                    val lp = layoutParams
                    lp.height = 0
                    layoutParams = lp

                    visibility = endVisibility
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

    if (startHeight > 0)
        startAnimation(collapseAnimation)
    else {
        visibility = endVisibility
        onComplete?.invoke()
    }
}

fun View.setVisibleIfTrue(boolean: Boolean, elseVisibility: Int = GONE) {
    val newVisibility = if (boolean) VISIBLE else elseVisibility
    if (visibility != newVisibility) {
        visibility = newVisibility
        if (this is ExpandableView) {
            expandedState = if (boolean) EXPANDED else COLLAPSED
        }
        requestLayout()
    }
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
    val bgColor = when (background) {
        is ColorDrawable -> (background as ColorDrawable?)?.color
        is MaterialShapeDrawable -> (background as MaterialShapeDrawable?)?.fillColor?.defaultColor
        else -> null
    }
    val colorTo = MaterialColors.getColor(this, to)
    val colorFrom: Int = bgColor ?: colorTo

    val colorAnimation: ValueAnimator =
        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            duration = COLOR_TRANSITION_ANIMATION
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

fun View.fade(
    fadeIn: Boolean,
    doNext: (() -> Unit)? = null
) {
    animation?.cancel()
    clearAnimation()

    val startAlpha = alpha
    val endAlpha = if (fadeIn) 1f else 0f
    val alphaDuration = endAlpha - startAlpha

    val fadeAnimation =
        object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                alpha = startAlpha + alphaDuration * interpolatedTime
                invalidate()
            }
        }.apply {
            duration =
                (ANIMATION_DURATION * abs(alphaDuration)).toLong()
            fillAfter = true

            setAnimationListener(
                object : AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        // Do nothing
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        alpha = endAlpha
                        clearAnimation()
                        invalidate()
                        doNext?.invoke()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                        // Do nothing
                    }
                }
            )
        }

    startAnimation(fadeAnimation)
}

fun View.animateMargin(
    expand: Boolean,
    marginFullSize: Float,
    getMarginValue: KFunction1<ViewGroup.MarginLayoutParams, Int>,
    setMarginValue: KFunction2<ViewGroup.MarginLayoutParams, Int, Unit>,
    doNext: (() -> Unit)? = null
) {
    animation?.cancel()
    clearAnimation()

    val startingMargin = getMarginValue(marginLayoutParams)
    val endMargin = if (expand) marginFullSize.toInt() else 0
    val marginDuration = endMargin - startingMargin

    val animation =
        object : Animation() {
            override fun willChangeBounds(): Boolean = true

            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                val lp = marginLayoutParams
                setMarginValue(
                    lp,
                    (startingMargin + marginDuration * interpolatedTime).toInt()
                )
                layoutParams = lp

                requestLayout()
            }
        }.apply {
            duration =
                (ANIMATION_DURATION * abs(marginDuration) / marginFullSize).toLong()
            fillAfter = true

            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Do nothing
                    debugLog("Starting margin adjust animation $x $width $endMargin")
                }

                override fun onAnimationEnd(animation: Animation?) {
                    debugLog("Margin adjust animation done $x $width $endMargin")
                    val lp = marginLayoutParams
                    setMarginValue(lp, endMargin)
                    layoutParams = lp

                    clearAnimation()
                    requestLayout()
                    doNext?.invoke()
                }

                override fun onAnimationRepeat(animation: Animation?) {
                    // Do nothing
                }
            })
        }

    startAnimation(animation)
}

fun View.fadeAndGo(
    expandMargin: Boolean,
    targetMargin: Float,
    fadingView: View,
    getMarginValue: KFunction1<ViewGroup.MarginLayoutParams, Int>,
    setMarginValue: KFunction2<ViewGroup.MarginLayoutParams, Int, Unit>
) {
    val shouldExpand =
        expandMargin && (getMarginValue(marginLayoutParams) < targetMargin)

    val shouldCollapse =
        !expandMargin && (getMarginValue(marginLayoutParams) > 0)

    when {
        shouldExpand || shouldCollapse -> {
            animateMargin(
                expand = expandMargin,
                marginFullSize = targetMargin,
                getMarginValue = getMarginValue,
                setMarginValue = setMarginValue
            )
            fadingView.fade(fadeIn = expandMargin)
        }
    }
}
