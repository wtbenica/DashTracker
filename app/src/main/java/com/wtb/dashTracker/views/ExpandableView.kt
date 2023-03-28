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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TableLayout
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.animateTo
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.reveal

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
                mAnimateTo (
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

    override fun mAnimateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)?): Unit =
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

    override fun mAnimateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)?): Unit =
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

    override fun mAnimateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)?): Unit =
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

    override fun mAnimateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)?): Unit =
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

    override fun mAnimateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)?): Unit =
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

    override fun mAnimateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)?): Unit =
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

    override fun mAnimateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)?): Unit =
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

    override fun mAnimateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)?): Unit =
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
) : androidx.appcompat.widget.AppCompatImageButton(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = reveal { onComplete?.invoke() }

    override fun mAnimateTo(targetHeight: Int?, targetWidth: Int?, onComplete: (() -> Unit)?): Unit =
        animateTo(targetHeight, targetWidth)

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
    override var viewIsExpanded: Boolean = isVisible && (layoutParams?.height == WRAP_CONTENT)
    override var viewIsCollapsed: Boolean = !isVisible && (layoutParams?.height == 0)
}
