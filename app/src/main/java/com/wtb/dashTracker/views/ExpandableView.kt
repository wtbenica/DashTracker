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
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TableLayout
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.expand
import com.wtb.dashTracker.extensions.expandTo
import com.wtb.dashTracker.ui.activity_main.debugLog

// TODO: There's got to be a better way of doing this
interface ExpandableView {
    val mIsVisible: Boolean
    fun mExpand(onComplete: (() -> Unit)? = null)
    fun mExpandTo(
        targetHeight: Int? = ViewGroup.LayoutParams.WRAP_CONTENT,
        targetWidth: Int? = ViewGroup.LayoutParams.MATCH_PARENT
    )

    fun mCollapse(onComplete: (() -> Unit)? = null)

    var isExpanding: Boolean
    var isCollapsing: Boolean

    fun revealIfTrue(
        shouldShow: Boolean = true,
        doAnyways: Boolean = false,
        onComplete: (() -> Unit)? = null
    ) {
        if (shouldShow && (!mIsVisible || isCollapsing)) {
            isExpanding = true
            isCollapsing = false
            mExpand {
                isExpanding = false
                onComplete?.invoke()
            }
        } else if (!shouldShow && (mIsVisible || isExpanding)) {
            isCollapsing = true
            isExpanding = false
            mCollapse {
                isCollapsing = false
                onComplete?.invoke()
            }
        } else if (doAnyways) {
            onComplete?.invoke()
        }
    }

    fun expandToIfTrue(shouldExpand: Boolean = true, toHeight: Int? = null, toWidth: Int? = null) {
        if (shouldExpand && !mIsVisible) {
            mExpandTo(
                targetHeight = toHeight,
                targetWidth = toWidth
            )
        } else if (!shouldExpand && mIsVisible) {
            mCollapse()
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

    override fun mExpand(onComplete: (() -> Unit)?): Unit = expand { onComplete?.invoke() }

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override fun mExpandTo(targetHeight: Int?, targetWidth: Int?) =
        expandTo(targetHeight, targetWidth)

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
}

class ExpandableGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : GridLayout(context, attrs, defStyleAttr, defStyleRes), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = expand { onComplete?.invoke() }

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override fun mExpandTo(targetHeight: Int?, targetWidth: Int?) =
        expandTo(targetHeight, targetWidth)

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false

    override fun revealIfTrue(shouldShow: Boolean, doAnyways: Boolean, onComplete: (() -> Unit)?) {
        debugLog(
            "revealIfTrue | Summary Bar | should? ${if (shouldShow) "Expand" else "Collapse"}  | will? ${
                when {
                    shouldShow && (!mIsVisible || isCollapsing) -> "Expand"
                    !shouldShow && (mIsVisible || isExpanding) -> "Collapse"
                    else -> "Do Nothing"
                }
            }", this::class.simpleName != "ExpandableTextView"
        )

        super.revealIfTrue(shouldShow, doAnyways, onComplete)
    }
}

class ExpandableAppBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppBarLayout(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = expand { onComplete?.invoke() }

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override fun mExpandTo(targetHeight: Int?, targetWidth: Int?) =
        expandTo(targetHeight, targetWidth)

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false

    override fun revealIfTrue(shouldShow: Boolean, doAnyways: Boolean, onComplete: (() -> Unit)?) {
        debugLog(
            "revealIfTrue | ${this::class.simpleName} | should? ${if (shouldShow) "Expand" else "Collapse"}  | will? ${
                when {
                    shouldShow && (!mIsVisible || isCollapsing) -> "Expand"
                    !shouldShow && (mIsVisible || isExpanding) -> "Collapse"
                    else -> "Do Nothing"
                }
            }", this::class.simpleName != "ExpandableTextView"
        )

        super.revealIfTrue(shouldShow, doAnyways, onComplete)
    }
}

class ExpandableTableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TableLayout(context, attrs), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = expand { onComplete?.invoke() }

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override fun mExpandTo(targetHeight: Int?, targetWidth: Int?) =
        expandTo(targetHeight, targetWidth)

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
}


class ExpandableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = expand { onComplete?.invoke() }

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override fun mExpandTo(targetHeight: Int?, targetWidth: Int?) =
        expandTo(targetHeight, targetWidth)

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
}

class ExpandableButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatButton(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = expand { onComplete?.invoke() }

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override fun mExpandTo(targetHeight: Int?, targetWidth: Int?) =
        expandTo(targetHeight, targetWidth)

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
}

class ExpandableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = expand { onComplete?.invoke() }

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override fun mExpandTo(targetHeight: Int?, targetWidth: Int?) =
        expandTo(targetHeight, targetWidth)

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
}

class ExpandableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr), ExpandableView {
    override val mIsVisible: Boolean
        get() = isVisible

    override fun mExpand(onComplete: (() -> Unit)?): Unit = expand { onComplete?.invoke() }

    override fun mCollapse(onComplete: (() -> Unit)?): Unit = collapse { onComplete?.invoke() }

    override fun mExpandTo(targetHeight: Int?, targetWidth: Int?) =
        expandTo(targetHeight, targetWidth)

    override var isExpanding: Boolean = false
    override var isCollapsing: Boolean = false
}