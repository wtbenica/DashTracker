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
import android.view.ViewGroup.MarginLayoutParams
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TableLayout
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.appbar.AppBarLayout
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.reveal
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState.*

// TODO: There's got to be a better way of doing this
interface ExpandableView {
    val ticketId: Int

    var expandedState: ExpandedState?

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
        get() = expandedState == COLLAPSING || expandedState == COLLAPSED || expandedState == null

    val isExpandedOrExpanding: Boolean
        get() = expandedState == EXPANDING || expandedState == EXPANDED || expandedState == null

    fun mExpand(onComplete: (() -> Unit)? = null) {
        if (this is View) {
            expandedState = EXPANDING
            reveal {
                expandedState = EXPANDED
                onComplete?.invoke()
            }
        }
    }

    fun mCollapse(endVisibility: Int = INVISIBLE, onComplete: (() -> Unit)? = null) {
        if (this is View) {
            expandedState = COLLAPSING
            collapse(endVisibility) {
                expandedState = COLLAPSED
                onComplete?.invoke()
            }
        }
    }

    fun revealIfTrue(
        shouldShow: Boolean = true,
        doAnyways: Boolean = false,
        endVisibility: Int = INVISIBLE,
        onComplete: (() -> Unit)? = null
    ) {
        val shouldExpand = shouldShow // && isCollapsedOrCollapsing

        val shouldCollapse = !shouldShow // && isExpandedOrExpanding

        val shouldDoAnyways =
            doAnyways && (shouldShow && viewIsExpanded) || (!shouldShow && viewIsCollapsed)

        when {
            shouldExpand -> mExpand(onComplete)
            shouldCollapse -> mCollapse(endVisibility, onComplete)
            shouldDoAnyways -> onComplete?.invoke()
        }
    }

    companion object {
        enum class ExpandedState {
            COLLAPSED, COLLAPSING, EXPANDING, EXPANDED
        }

        val View.marginLayoutParams: MarginLayoutParams
            get() = layoutParams as MarginLayoutParams

    }
}

open class ExpandableLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null

    companion object {
        private var ticketNumber: Int = 1
    }
}

class ExpandableConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null

    companion object {
        private var ticketNumber: Int = 1
    }
}

class ExpandableGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : GridLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null

    companion object {
        private var ticketNumber: Int = 1
    }
}

class ExpandableAppBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppBarLayout(context, attrs, defStyleAttr), ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null

    companion object {
        private var ticketNumber: Int = 1
    }
}

class ExpandableTableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TableLayout(context, attrs), ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null

    companion object {
        private var ticketNumber: Int = 1
    }
}

class ExpandableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr), ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null
        set(value) {
            field = value
        }

    companion object {
        private var ticketNumber: Int = 1
    }
}

class ExpandableButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatButton(context, attrs, defStyleAttr), ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null

    companion object {
        private var ticketNumber: Int = 1
    }
}

class ExpandableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.cardViewStyle
) : CardView(context, attrs, defStyleAttr), ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null

    companion object {
        private var ticketNumber: Int = 1
    }
}

class ExpandableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr), ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null

    companion object {
        private var ticketNumber: Int = 1
    }
}

class ExpandableImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageButton(context, attrs, defStyleAttr),
    ExpandableView {
    override val ticketId: Int = ticketNumber++

    override var expandedState: ExpandedState? = null

    companion object {
        private var ticketNumber: Int = 1
    }
}

