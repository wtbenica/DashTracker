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

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
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

fun View.showOrHide(
    shouldShow: Boolean,
    targetSize: Int? = null,
    onHiddenVisibility: Int = INVISIBLE,
    animate: Boolean = true,
    onComplete: (() -> Unit)? = null
) {
    if (shouldShow) {
        visibility = VISIBLE
        updateLayoutParams {
            height = max(1, this@showOrHide.height)
        }
    }

    val startHeight = max(1, height)
    val endHeight = if (shouldShow) {
        targetSize ?: targetHeight
    } else {
        0
    }

    val heightDuration = endHeight - startHeight

    if (animate) {
        animate()
            .alpha(if (shouldShow) 1f else 0f)
            .setUpdateListener {
                updateLayoutParams {
                    height = (startHeight + heightDuration * it.animatedFraction).toInt()
                }
            }
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                clearAnimation()
                if (!shouldShow) {
                    visibility = onHiddenVisibility
                    updateLayoutParams {
                        height = 0
                    }
                } else {
                    updateLayoutParams {
                        height = targetSize ?: WRAP_CONTENT
                    }
                }
                requestLayout()
                onComplete?.invoke()
            }
    } else {
        updateLayoutParams {
            height = if (shouldShow) {
                targetSize ?: WRAP_CONTENT
            } else {
                0
            }
        }
        visibility = if (shouldShow) VISIBLE else onHiddenVisibility
        onComplete?.invoke()
    }

}

fun View.animateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)? = null) {
    animation?.cancel()
    clearAnimation()
    if ((targetHeight == 0 && height == 0) || (targetWidth == 0 && width == 0)) {
        visibility = INVISIBLE
        updateLayoutParams {
            targetHeight?.let { height = it }
            targetWidth?.let { width = it }
        }
    } else {
        updateLayoutParams {
            height = max(1, layoutParams.height)
        }

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
                super.applyTransformation(interpolatedTime, t)
                updateLayoutParams {
                    height = (initHeight + deltaHeight * interpolatedTime).toInt()
                    width = (initWidth + deltaWidth * interpolatedTime).toInt()
                }

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
                        updateLayoutParams {
                            height = initHeight + deltaHeight
                            width = initWidth + deltaWidth
                        }

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
        showOrHide(false)
    }
}

fun View.setVisibleIfTrue(boolean: Boolean, elseVisibility: Int = GONE) {
    val newVisibility = if (boolean) VISIBLE else elseVisibility
    if (visibility != newVisibility) {
        visibility = newVisibility
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

val View.marginLayoutParams: MarginLayoutParams
    get() = layoutParams as MarginLayoutParams

fun View.fade(
    fadeIn: Boolean,
    doNext: (() -> Unit)? = null
) {
    animate()
        .alpha(if (fadeIn) 1f else 0f)
        .setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                // Do nothing
            }

            override fun onAnimationEnd(animation: Animator) {
                doNext?.invoke()
            }

            override fun onAnimationCancel(animation: Animator) {
                // Do nothing
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Do nothing
            }

        })
}

fun View.animateMargin(
    expand: Boolean,
    marginFullSize: Float,
    getMarginValue: KFunction1<MarginLayoutParams, Int>,
    setMarginValue: KFunction2<MarginLayoutParams, Int, Unit>,
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
                updateLayoutParams<MarginLayoutParams> {
                    setMarginValue(
                        this,
                        (startingMargin + marginDuration * interpolatedTime).toInt()
                    )
                }

                requestLayout()
            }
        }.apply {
            duration =
                (ANIMATION_DURATION * abs(marginDuration) / marginFullSize).toLong()
            fillAfter = true

            setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    // Do nothing
                }

                override fun onAnimationEnd(animation: Animation?) {
                    updateLayoutParams<MarginLayoutParams> {
                        setMarginValue(this, endMargin)
                    }

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
    getMarginValue: KFunction1<MarginLayoutParams, Int>,
    setMarginValue: KFunction2<MarginLayoutParams, Int, Unit>
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
