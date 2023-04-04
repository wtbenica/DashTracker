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
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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
import com.wtb.dashTracker.ui.activity_main.debugLog
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState
import com.wtb.dashTracker.views.ExpandableView.Companion.ExpandedState.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        get() = this is View && height == 0 && layoutParams.height != WRAP_CONTENT && expandedState != EXPANDING

    val needsCollapsion: Boolean
        get() = this is View && height != 0 && expandedState != COLLAPSING

    fun mExpand(onComplete: (() -> Unit)? = null) {
        if (this is View) {
            reveal {
                expandedState = EXPANDED
                debugLog(
                    "Setting expandedState to EXPANDED.",
                    this is ExpandableTextView && text == "$46.50"
                )
                onComplete?.invoke()
            }
        }
    }

    fun mCollapse(endVisibility: Int = INVISIBLE, onComplete: (() -> Unit)? = null) {
        if (this is View) {
            collapse(endVisibility) {
                expandedState = COLLAPSED
                onComplete?.invoke()
            }
        }
    }

    fun showOrHide(
        shouldShow: Boolean = true,
        doAnyways: Boolean = false,
        endVisibility: Int = INVISIBLE,
        addedInfo: Boolean? = null,
        onComplete: (() -> Unit)? = null
    ) {
        val shouldExpand = shouldShow

        val shouldCollapse = !shouldShow

        val shouldDoAnyways =
            doAnyways && (shouldShow && viewIsExpanded) || (!shouldShow && viewIsCollapsed)

        val trying = if (shouldShow) "Expand" else "Collapse"
        val doing = when {
            shouldDoAnyways -> "DOING ANYWAYS"
            shouldExpand -> "EXPAND"
            shouldCollapse -> "COLLAPSE"
            else -> "NOTHIN'"
        }
        debugLog(
            "showOrHide | Trying to $trying. Doing $doing. State is ${expandedState?.name}",
            this is ExpandableTextView && text == "$46.50"
        )

        when {
            shouldDoAnyways -> {
                debugLog(
                    "showOrHide | doAnyways. State is ${expandedState?.name}",
                    this is ExpandableTextView && text == "$46.50"
                )
                onComplete?.invoke()
            }
            shouldExpand -> {
                debugLog(
                    "showOrHide | shouldExpand. State is ${expandedState?.name}",
                    this is ExpandableTextView && text == "$46.50"
                )
                expandedState = EXPANDING
                mExpand(onComplete)
            }
            shouldCollapse -> {
                debugLog(
                    "showOrHide | shouldCollapse. State is ${expandedState?.name}",
                    this is ExpandableTextView && text == "$46.50"
                )
                expandedState = COLLAPSING
                CoroutineScope(Dispatchers.Default).launch {
                    delay(500L)
                    expandedState = COLLAPSED
                }
                mCollapse(endVisibility, onComplete)
            }
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
        set(value) {
            if (text == "$46.50") {
                debugLog("Setting Expanded State: $field <- $value")
            }
            field = value
        }
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

