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
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.wtb.dashTracker.R
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.reveal
import com.wtb.dashTracker.ui.activity_main.debugLog
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState.*

// TODO: There's got to be a better way of doing this (even though it's a lot better now)
interface ExpandableView {
    var expandedState: ExpandedState?

    /**
     * Prevents calling an onComplete prematurely. expandedState == EXPANDED
     */
    val viewIsExpanded: Boolean
        get() = expandedState == EXPANDED

    /**
     * Prevents calling an onComplete prematurely. expandedState = COLLAPSED
     */
    val viewIsCollapsed: Boolean
        get() = expandedState == COLLAPSED

    val needsExpansion: Boolean
        get() = this is View && height == 0

    val needsCollapsion: Boolean
        get() = this is View && height != 0

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
        addedInfo: Boolean? = null,
        onComplete: (() -> Unit)? = null
    ) {
        if (this is ExpandableTextView) {
            debugLog(
                "Reveal if true $text | show? $shouldShow | View is ${expandedState?.name}. Is it visible? $isVisible. Height? $height. LpHeight? ${layoutParams.height}"
            )
        }

        val shouldExpand = shouldShow && needsExpansion

        val shouldCollapse = !shouldShow && needsCollapsion

        val shouldDoAnyways =
            doAnyways && (shouldShow && viewIsExpanded) || (!shouldShow && viewIsCollapsed)

        if (this is ExpandableTextView) {
            debugLog("Reveal if true  $text | expand? $shouldExpand | collapse? $shouldCollapse | do? $shouldDoAnyways")
        }

        when {
            shouldDoAnyways -> onComplete?.invoke()
            shouldExpand -> mExpand(onComplete)
            shouldCollapse -> mCollapse(endVisibility, onComplete)
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
    override var expandedState: ExpandedState? = null
}

class ExpandableConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override var expandedState: ExpandedState? = null
}

class ExpandableGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : GridLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override var expandedState: ExpandedState? = null
}

class ExpandableAppBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppBarLayout(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState? = null
}

class ExpandableTableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TableLayout(context, attrs), ExpandableView {
    override var expandedState: ExpandedState? = null
}

class ExpandableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState? = null
}

class ExpandableButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatButton(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState? = null
}

class ExpandableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.cardViewStyle
) : CardView(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState? = null
}

class ExpandableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr), ExpandableView {
    override var expandedState: ExpandedState? = null
}

class ExpandableImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageButton(context, attrs, defStyleAttr),
    ExpandableView {
    override var expandedState: ExpandedState? = null
}

