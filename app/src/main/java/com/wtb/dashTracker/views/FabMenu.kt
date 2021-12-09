package com.wtb.dashTracker.views

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wtb.dashTracker.MainActivity
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.extensions.isTouchTarget
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class FabMenu @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attributeSet, defStyleAttr), FabMenuButton.FabMenuButtonCallback {
    private var fabMenuViews = mutableListOf<FabMenuButton>()
    private var fabMenuIsVisible = false
    private var fabMenuItems = listOf<FabMenuButtonInfo>()
    private var parentLayout: CoordinatorLayout? = null

    init {
        setOnClickListener {
            if (fabMenuIsVisible)
                hideFabMenu()
            else
                showFabMenu()
        }
    }

    fun initialize(menuItems: List<FabMenuButtonInfo>, parent: CoordinatorLayout) {
        fabMenuItems = menuItems
        parentLayout = parent
        initFabMenu()
    }

    fun hideFabMenu() {
        fadeOutFab()
        saturateFab()
        runFabIconCollapseAnimation()
        fabMenuIsVisible = false
    }

    private fun showFabMenu() {
        fadeInFabMenu()
        desaturateFab()
        runFabIconExpandAnimation()

        fabMenuIsVisible = true
    }

    private fun runFabIconCollapseAnimation() {
        setImageResource(R.drawable.anim_fab_collapse)
        when (val d = drawable) {
            is AnimatedVectorDrawableCompat -> d.start()
            is AnimatedVectorDrawable -> d.start()
        }
    }

    private fun runFabIconExpandAnimation() {
        setImageResource(R.drawable.anim_fab_expand)
        when (val d = drawable) {
            is AnimatedVectorDrawableCompat -> d.start()
            is AnimatedVectorDrawable -> d.start()
        }
    }

    private fun saturateFab() {
        @ColorInt val accent = MainActivity.getColorAccent(context)
        @ColorInt val gray = context.getColor(R.color.shadow)

        animateFabColor(gray, accent)
    }

    private fun animateFabColor(fromColor: Int, toColor: Int) {
        ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
            duration = ANIM_LENGTH
            addUpdateListener {
                val i = animatedValue as Int
                backgroundTintList = ColorStateList.valueOf(i)
            }
            start()
        }
    }

    private fun desaturateFab() {
        @ColorInt val accent = MainActivity.getColorAccent(context)
        @ColorInt val gray = context.getColor(R.color.shadow)

        animateFabColor(accent, gray)
    }

    private fun fadeInFabMenu() {
        fabMenuViews.forEachIndexed { index, item ->
            item.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(ANIM_DELAY * index)
                .setDuration(ANIM_LENGTH)
                .setInterpolator(OvershootInterpolator())
                .withStartAction { item.visibility = VISIBLE }
        }
    }

    private fun fadeOutFab() {
        val dimension = resources.getDimension(R.dimen.fab_menu_offset)
        fabMenuViews.forEachIndexed { index, item ->
            item.animate()
                .alpha(0f)
                .translationY(dimension)
                .setStartDelay(ANIM_DELAY * (fabMenuViews.size - index - 1))
                .setDuration(ANIM_LENGTH)
                .withEndAction { item.visibility = GONE }
        }
    }

    private fun initFabMenu() {
        @IdRes var itemAnchor: Int = id
        for (item in fabMenuItems) {
            val newMenuItem: FabMenuButton =
                FabMenuButton.newInstance(context, item, this).apply {
                    id = View.generateViewId()
                    translationY = resources.getDimension(R.dimen.fab_menu_offset)
                }
            fabMenuViews.add(newMenuItem)
            parentLayout?.addView(
                newMenuItem,
                CoordinatorLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    anchorId = itemAnchor
                    anchorGravity = Gravity.TOP or Gravity.END
                    gravity = Gravity.TOP
                })

            itemAnchor = newMenuItem.id
        }
        for (item in fabMenuViews) {
            item.visibility = GONE
        }
    }

    fun interceptTouchEvent(ev: MotionEvent?) {
        if (fabMenuIsVisible && ev?.action == MotionEvent.ACTION_DOWN) {
            val views: List<View> = fabMenuViews + this
            val menuIsTouchTarget = false
            for (v: View in views) {
                if (v.isTouchTarget(ev)) {
                    menuIsTouchTarget
                }
            }

            if (!menuIsTouchTarget) hideFabMenu()
        }
    }

    override fun fabMenuClicked() {
        hideFabMenu()
    }

    companion object {
        private const val ANIM_LENGTH = 100L
        private const val ANIM_DELAY = 50L
    }
}