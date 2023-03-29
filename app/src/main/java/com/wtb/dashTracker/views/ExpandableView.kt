/*
 * Copyright 2023 Wesley T. Benica
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

package com.wtb.dashTracker.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TableLayout
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.ANIMATION_DURATION
import com.wtb.dashTracker.extensions.animateTo
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.reveal
import com.wtb.dashTracker.ui.activity_main.debugLog
import kotlin.math.abs

// TODO: There's got to be a better way of doing this
interface ExpandableView {
    val mIsVisible: Boolean
    fun mExpand(onComplete: (() -> Unit)? = null)
    fun mAnimateTo(
        targetHeight: Int? = WRAP_CONTENT,
        targetWidth: Int? = MATCH_PARENT,
        onComplete: (() -> Unit)? = null
    )

    fun mCollapse(onComplete: (() -> Unit)? = null)

    /**
     * Prevents unnecessary expands
     */
    var isExpanding: Boolean

    /**
     * Prevents unnecessary collapses
     */
    var isCollapsing: Boolean

    /**
     * Prevents calling an onComplete prematurely
     */
    var viewIsExpanded: Boolean

    /**
     * Prevents calling an onComplete prematurely
     */
    var viewIsCollapsed: Boolean

    fun revealIfTrue(
        shouldShow: Boolean = true,
        doAnyways: Boolean = false,
        onComplete: (() -> Unit)? = null
    ) {
        val shouldExpand = shouldShow && !viewIsExpanded && (!isExpanding || isCollapsing)

        val shouldCollapse = !shouldShow && !viewIsCollapsed && (!isCollapsing || isExpanding)

        val shouldDoAnyways =
            doAnyways && (shouldShow && viewIsExpanded) || (!shouldShow && viewIsCollapsed)

        when {
            shouldExpand -> {
                isExpanding = true
                isCollapsing = false
                viewIsCollapsed = false
                mExpand {
                    isExpanding = false
                    isCollapsing = false
                    viewIsExpanded = true
                    viewIsCollapsed = false
                    onComplete?.invoke()
                }
            }
            shouldCollapse -> {
                isCollapsing = true
                isExpanding = false
                viewIsExpanded = false
                mCollapse {
                    isExpanding = false
                    isCollapsing = false
                    viewIsExpanded = false
                    viewIsCollapsed = true
                    onComplete?.invoke()
                }
            }
            shouldDoAnyways -> {
                onComplete?.invoke()
            }
        }
    }

    fun transformTo(
        expand: Boolean,
        toHeight: Int?,
        toWidth: Int?,
        onComplete: (() -> Unit)? = null
    ) {
        val shouldExpand = expand && !viewIsExpanded && (!isExpanding || isCollapsing)

        val shouldCollapse = !expand && !viewIsCollapsed && (!isCollapsing || isExpanding)

        val shouldDoAnyways = (expand && viewIsExpanded) || (!isExpanding && viewIsCollapsed)

        isExpanding = shouldExpand
        isCollapsing = shouldCollapse

        when {
            shouldExpand -> {
                mAnimateTo(
                    targetHeight = toHeight,
                    targetWidth = toWidth
                ) {
                    isExpanding = false
                    isCollapsing = false
                    viewIsExpanded = true
                    viewIsCollapsed = false
                    onComplete?.invoke()
                }
            }
            shouldCollapse -> {
                mAnimateTo(
                    targetHeight = toHeight,
                    targetWidth = toWidth
                ) {
                    isExpanding = false
                    isCollapsing = false
                    viewIsExpanded = false
                    viewIsCollapsed = true
                    onComplete?.invoke()
                }
            }
            shouldDoAnyways -> {
                onComplete?.invoke()
            }
        }
    }

    companion object {
        val View.marginLayoutParams
            get() = layoutParams as MarginLayoutParams

        fun fadeAndGo(
            expand: Boolean,
            expandView: View,
            targetMargin: Float,
            fadingView: View,
            setMargin: (MarginLayoutParams, Int) -> Unit
        ) {
            val shouldExpand = expand &&
                    (expandView !is ExpandableView ||
                            (!expandView.viewIsExpanded &&
                                    (!expandView.isExpanding || expandView.isCollapsing)))

            val shouldCollapse =
                !expand && (expandView !is ExpandableView ||
                        (!expandView.viewIsCollapsed && (!expandView.isCollapsing || expandView.isExpanding)))

            if (expandView is ExpandableView) {
                expandView.isExpanding = shouldExpand
                expandView.isCollapsing = shouldCollapse
            }

            debugLog(
                when {
                    shouldExpand -> "Should Expand"
                    shouldCollapse -> "Should Collapse"
                    else -> "Do nothing"
                },
                shouldExpand || shouldExpand
            )
            when {
                shouldExpand || shouldCollapse -> {
                    listOf(expandView, fadingView).forEach {
                        it.animation?.cancel()
                        it.clearAnimation()
                    }

                    val startAlpha = fadingView.alpha
                    val endAlpha = if (expand) 1f else 0f
                    val alphaDuration = endAlpha - startAlpha

                    fun getFadeAnimation(doNext: (() -> Unit)?) =
                        object : Animation() {
                            override fun applyTransformation(
                                interpolatedTime: Float,
                                t: Transformation?
                            ) {
                                fadingView.apply {
                                    alpha = startAlpha + (alphaDuration * interpolatedTime)
                                    invalidate()
                                }
                            }
                        }.apply {
                            duration =
                                (ANIMATION_DURATION * abs(alphaDuration) / 2).toLong()
                            fillAfter = true

                            setAnimationListener(
                                object : Animation.AnimationListener {
                                    override fun onAnimationStart(animation: Animation?) {
                                        // Do nothing
                                    }

                                    override fun onAnimationEnd(animation: Animation?) {
                                        fadingView.apply {
                                            alpha = endAlpha
                                            clearAnimation()
                                            invalidate()
                                        }

                                        doNext?.invoke()
                                    }

                                    override fun onAnimationRepeat(animation: Animation?) {
                                        // Do nothing
                                    }
                                }
                            )
                        }

                    val startingMargin = expandView.marginLayoutParams.marginEnd
                    val endMargin = if (expand) targetMargin.toInt() else 0
                    val marginDuration = endMargin - startingMargin

                    fun getExpandAnimation(doNext: (() -> Unit)?) =
                        object : Animation() {
                            override fun willChangeBounds(): Boolean = true

                            override fun applyTransformation(
                                interpolatedTime: Float,
                                t: Transformation?
                            ) {
                                expandView.apply {
                                    val lp = marginLayoutParams
                                    val value =
                                        (startingMargin + marginDuration * interpolatedTime).toInt()
                                    setMargin(lp, value)
                                    layoutParams = lp

                                    requestLayout()
                                }
                            }
                        }.apply {
                            duration =
                                (ANIMATION_DURATION * abs(marginDuration) / targetMargin / 2).toLong()
                            fillAfter = true

                            setAnimationListener(object : Animation.AnimationListener {
                                override fun onAnimationStart(animation: Animation?) {
                                    // Do nothing
                                }

                                override fun onAnimationEnd(animation: Animation?) {
                                    expandView.apply {
                                        val lp = marginLayoutParams
                                        setMargin(lp, endMargin)
                                        layoutParams = lp

                                        clearAnimation()
                                        requestLayout()

                                        if (this is ExpandableView) {
                                            isExpanding = false
                                            isCollapsing = false
                                            viewIsExpanded = expand
                                            viewIsCollapsed = !expand
                                        }
                                    }

                                    doNext?.invoke()
                                }

                                override fun onAnimationRepeat(animation: Animation?) {
                                    // Do nothing
                                }
                            })
                        }

                    if (expand) {
                        debugLog("fading away then expanding")
                        expandView.startAnimation(getExpandAnimation {
                            debugLog("faded away then expanding")
                            fadingView.startAnimation(getFadeAnimation {
                                debugLog("faded away and expanded")
                            })
                        })
                    } else {
                        debugLog("collapsing then fading in")
                        fadingView.startAnimation(getFadeAnimation {
                            debugLog("collapsed then fading in")
                            expandView.startAnimation(getExpandAnimation {
                                debugLog("collapsed and faded in")
                            })
                        })

                    }
                }
            }
        }
    }
}

open class ExpandableLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(
        targetHeight: Int?,
        targetWidth: Int?,
        onComplete: (() -> Unit)?
    ): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false

    override var viewIsExpanded: Boolean = isVisible && (layoutParams?.height == WRAP_CONTENT)
    override var viewIsCollapsed: Boolean = !isVisible && (layoutParams?.height == 0)
}

class ExpandableGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : GridLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(
        targetHeight: Int?,
        targetWidth: Int?,
        onComplete: (() -> Unit)?
    ): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
    override var viewIsExpanded: Boolean = isVisible && (layoutParams?.height == WRAP_CONTENT)
    override var viewIsCollapsed: Boolean = !isVisible && (layoutParams?.height == 0)
}

class ExpandableAppBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppBarLayout(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(
        targetHeight: Int?,
        targetWidth: Int?,
        onComplete: (() -> Unit)?
    ): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
    override var viewIsExpanded: Boolean = isVisible && (layoutParams?.height == WRAP_CONTENT)
    override var viewIsCollapsed: Boolean = !isVisible && (layoutParams?.height == 0)
}

class ExpandableTableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TableLayout(context, attrs), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(
        targetHeight: Int?,
        targetWidth: Int?,
        onComplete: (() -> Unit)?
    ): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
    override var viewIsExpanded: Boolean = isVisible && (layoutParams?.height == WRAP_CONTENT)
    override var viewIsCollapsed: Boolean = !isVisible && (layoutParams?.height == 0)
}


class ExpandableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(
        targetHeight: Int?,
        targetWidth: Int?,
        onComplete: (() -> Unit)?
    ): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false

    override var viewIsExpanded: Boolean = isVisible && (layoutParams?.height == WRAP_CONTENT)
    override var viewIsCollapsed: Boolean = !isVisible && (layoutParams?.height == 0)
}

class ExpandableButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatButton(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(
        targetHeight: Int?,
        targetWidth: Int?,
        onComplete: (() -> Unit)?
    ): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
    override var viewIsExpanded: Boolean = isVisible
    override var viewIsCollapsed: Boolean = !isVisible
}

class ExpandableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.cardViewStyle
) : CardView(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(
        targetHeight: Int?,
        targetWidth: Int?,
        onComplete: (() -> Unit)?
    ): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false

    override var viewIsExpanded: Boolean = isVisible && (layoutParams?.height == WRAP_CONTENT)
    override var viewIsCollapsed: Boolean = !isVisible && (layoutParams?.height == 0)
}

class ExpandableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(
        targetHeight: Int?,
        targetWidth: Int?,
        onComplete: (() -> Unit)?
    ): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
    override var viewIsExpanded: Boolean = isVisible && (layoutParams?.height == WRAP_CONTENT)
    override var viewIsCollapsed: Boolean = !isVisible && (layoutParams?.height == 0)
}

class ExpandableImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageButton(context, attrs, defStyleAttr),
    ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(
        targetHeight: Int?,
        targetWidth: Int?,
        onComplete: (() -> Unit)?
    ): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
    override var viewIsExpanded: Boolean = isVisible && (layoutParams?.height == WRAP_CONTENT)
    override var viewIsCollapsed: Boolean = !isVisible && (layoutParams?.height == 0)
}
