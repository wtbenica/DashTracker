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

package com.wtb.dashTracker.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.IdRes
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.ActivityMainActiveDashBarBinding
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getElapsedHours
import com.wtb.dashTracker.extensions.getStringOrElse
import com.wtb.dashTracker.extensions.setVisibleIfTrue
import com.wtb.dashTracker.views.ActiveDashBar.Companion.ADBState.*
import dev.benica.mileagetracker.LocationService.ServiceState.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ActiveDashBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : ExpandableLinearLayout(context, attrs, defStyleAttr) {

    private val binding: ActivityMainActiveDashBarBinding
    private var callback: ActiveDashBarCallback? = null
    private var activeEntry: FullEntry? = null
    private val animator: ValueAnimator

    init {
        val view: View = inflate(context, R.layout.activity_main_active_dash_bar, this)

        binding = ActivityMainActiveDashBarBinding.bind(view)

        animator = ValueAnimator.ofFloat(0.1f, 1f).apply {
            duration = 400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = -1
            addUpdateListener { animation ->
                binding.trackingStatusIndicator.alpha = animation.animatedValue as Float
            }
        }

        binding.btnStopActiveDash.setOnClickListener {
            callback?.stopDash()
        }
    }

    fun initialize(cb: ActiveDashBarCallback) {
        callback = cb
    }

    /**
     * shows/hides active dash bar and tracking details according to [serviceState].
     * [STOPPED] -> hide both
     * [TRACKING_ACTIVE] -> show both
     * [TRACKING_INACTIVE] or [PAUSED] -> show adb, hide details
     */
    fun onServiceStateUpdated(
        serviceState: ADBState,
        @IdRes currDestination: Int,
        onComplete: (() -> Unit)? = null
    ) {
        val narrowMargin = resources.getDimension(R.dimen.margin_narrow).toInt()

        binding.apply {
            when (serviceState) {
                INACTIVE -> { // Always collapse
                    root.setVisibleIfTrue(false)
                    animator.pause()
                    onComplete?.invoke()
                }
                TRACKING_FULL -> { // Always show expanded details
                    startTrackingIndicator()
                    btnStopActiveDash.setVisibleIfTrue(false)
                    callback?.revealAppBarLayout(shouldShow = true)
                    activeDashDetailsTopSpacer.setVisibleIfTrue(true)
                    root.setVisibleIfTrue(true)

                    activeDashDetails.apply {
                        revealIfTrue(
                            shouldShow = true,
                            doAnyways = true
                        ) {
                            onComplete?.invoke()
                            requestLayout()
                        }
                    }
                }
                TRACKING_COLLAPSED -> { // Show collapsed
                    startTrackingIndicator()
                    btnStopActiveDash.setVisibleIfTrue(true)
                    callback?.revealAppBarLayout(shouldShow = true, lockAppBar = true)
                    activeDashDetailsTopSpacer.setVisibleIfTrue(false)
                    root.setVisibleIfTrue(true)

                    activeDashDetails.apply {
                        revealIfTrue(
                            shouldShow = false,
                            doAnyways = true
                        ) {
                            onComplete?.invoke()
                            requestLayout()
                        }
                    }
                }
                TRACKING_DISABLED -> { // Show collapsed and stop tracking indicator
                    stopTrackingIndicator()
                    btnStopActiveDash.setVisibleIfTrue(true)
                    callback?.revealAppBarLayout(shouldShow = true, lockAppBar = true)
                    activeDashDetailsTopSpacer.setVisibleIfTrue(false)
                    root.setVisibleIfTrue(true)

                    activeDashDetails.revealIfTrue(
                        shouldShow = false,
                        doAnyways = true
                    ) {
                        onComplete?.invoke()
                        requestLayout()
                    }
                }
            }
        }
    }

    private fun startTrackingIndicator() {
        binding.trackingStatusRow.visibility = VISIBLE
        binding.trackingStatusText.apply {
            text = context.getString(R.string.lbl_tracking_active)
        }

        if (!animator.isStarted || animator.isPaused) animator.start()
    }

    private fun stopTrackingIndicator() {
        binding.trackingStatusRow.visibility = GONE
        binding.trackingStatusIndicator.alpha = 1f

        animator.pause()
    }

    fun updateEntry(fullEntry: FullEntry?, activeCpm: Float?) {
        fullEntry?.let { it ->
            activeEntry = it

            binding.valCpm.text =
                context.getStringOrElse(R.string.cpm_unit, "$ - ", callback?.currentCpm)

            binding.valMileage.text =
                context.getString(R.string.mileage_fmt, it.trackedDistance ?: 0.0)

            binding.valCost.text =
                context.getCurrencyString(
                    (it.trackedDistance ?: it.entry.mileage)?.let { miles ->
                        miles.toFloat() * (activeCpm ?: 0f)
                    }
                )

            binding.valElapsedTime.text =
                getElapsedHours(it.netTime)
        }
    }

    interface ActiveDashBarCallback {
        val currentCpm: Float?
        fun revealAppBarLayout(
            shouldShow: Boolean,
            doAnyways: Boolean = true,
            lockAppBar: Boolean = false,
            onComplete: (() -> Unit)? = null
        )

        fun stopDash()
    }

    companion object {
        enum class ADBState {
            TRACKING_FULL, TRACKING_COLLAPSED, TRACKING_DISABLED, INACTIVE
        }
    }
}
