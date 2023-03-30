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
import android.view.ViewGroup.INVISIBLE
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TableLayout
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.animateTo
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.reveal
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState.*

// TODO: There's got to be a better way of doing this
interface ExpandableView {
    fun mExpand(onComplete: (() -> Unit)? = null) {
        if (this is View) {
            reveal { onComplete?.invoke() }
        }
    }

    fun mAnimateTo(
        targetHeight: Int? = WRAP_CONTENT,
        targetWidth: Int? = MATCH_PARENT,
        onComplete: (() -> Unit)? = null
    ) {
        if (this is View) {
            animateTo(targetHeight, targetWidth, onComplete)
        }
    }

    fun mCollapse(endVisibility: Int = INVISIBLE, onComplete: (() -> Unit)? = null) {
        if (this is View) {
            collapse(endVisibility, onComplete)
        }
    }

    var expandedState: ExpandedState

    /**
     * Prevents calling an onComplete prematurely
     */
    val viewIsExpanded: Boolean
        get() = expandedState == EXPANDED

    /**
     * Prevents calling an onComplete prematurely
     */
    val viewIsCollapsed: Boolean
        get() = expandedState == COLLAPSED

    val isCollapsedOrCollapsing: Boolean
        get() = expandedState == COLLAPSING || expandedState == COLLAPSED

    val isExpandedOrExpanding: Boolean
        get() = expandedState == EXPANDING || expandedState == EXPANDED

    fun revealIfTrue(
        shouldShow: Boolean = true,
        doAnyways: Boolean = false,
        endVisibility: Int = INVISIBLE,
        onComplete: (() -> Unit)? = null
    ) {
        val shouldExpand = shouldShow && isCollapsedOrCollapsing

        val shouldCollapse = !shouldShow && isExpandedOrExpanding

        val shouldDoAnyways =
            doAnyways && (shouldShow && viewIsExpanded) || (!shouldShow && viewIsCollapsed)

        when {
            shouldExpand -> {
                expandedState = EXPANDING
                mExpand {
                    expandedState = EXPANDED
                    onComplete?.invoke()
                }
            }
            shouldCollapse -> {
                expandedState = COLLAPSING
                mCollapse(endVisibility) {
                    expandedState = COLLAPSED
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
        val shouldExpand = expand && isCollapsedOrCollapsing

        val shouldCollapse = !expand && isExpandedOrExpanding

        val shouldDoAnyways = (expand && viewIsExpanded) || (!expand && viewIsCollapsed)

        when {
            shouldExpand -> {
                expandedState = EXPANDING
                mAnimateTo(
                    targetHeight = toHeight,
                    targetWidth = toWidth
                ) {
                    expandedState = EXPANDED
                    onComplete?.invoke()
                }
            }
            shouldCollapse -> {
                expandedState = COLLAPSING
                mAnimateTo(
                    targetHeight = toHeight,
                    targetWidth = toWidth
                ) {
                    expandedState = COLLAPSED
                    onComplete?.invoke()
                }
            }
            shouldDoAnyways -> {
                onComplete?.invoke()
            }
        }
    }

    companion object {
        enum class ExpandedState {
            COLLAPSED, COLLAPSING, EXPANDING, EXPANDED
        }

        val View.marginLayoutParams
            get() = layoutParams as MarginLayoutParams

    }
}

open class ExpandableLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

class ExpandableConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

class ExpandableGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : GridLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

class ExpandableAppBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppBarLayout(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

class ExpandableTableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TableLayout(context, attrs), ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

class ExpandableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

class ExpandableButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatButton(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

class ExpandableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.cardViewStyle
) : CardView(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

class ExpandableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

class ExpandableImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageButton(context, attrs, defStyleAttr),
    ExpandableView {
    override var expandedState: ExpandedState = if (isVisible) {
        EXPANDED
    } else {
        COLLAPSED
    }
}

