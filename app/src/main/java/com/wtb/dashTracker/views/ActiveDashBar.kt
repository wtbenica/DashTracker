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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.ActivityMainActiveDashBarBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.views.ActiveDashBar.Companion.ADBState.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
class ActiveDashBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : ExpandableLinearLayout(context, attrs, defStyleAttr) {

    private val binding: ActivityMainActiveDashBarBinding
    private var callback: ActiveDashBarCallback? = null
    private var activeEntry: FullEntry? = null
    private val flashingIndicator: ValueAnimator

    init {
        val view: View = inflate(context, R.layout.activity_main_active_dash_bar, this)

        binding = ActivityMainActiveDashBarBinding.bind(view)

        flashingIndicator = ValueAnimator.ofInt(26, 255).apply {
            duration = 400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = -1
            addUpdateListener { animation ->
                binding.trackingStatusIndicator.compoundDrawables.getOrNull(2)?.alpha =
                    animation.animatedValue as Int
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
     * [INACTIVE] -> hide both
     * [TRACKING_FULL] -> show both
     * [TRACKING_COLLAPSED] -> show adb, hide details
     */
    fun onServiceStateUpdated(
        serviceState: ADBState,
        onComplete: (() -> Unit)? = null
    ) {
        binding.apply {
            when (serviceState) {
                INACTIVE -> { // Always collapse
                    root.visibility = GONE
                    flashingIndicator.pause()
                    onComplete?.invoke()
                }
                TRACKING_FULL -> { // Always show expanded details
                    startTrackingIndicator()

                    adbTitleBar.fadeAndGo(
                        expandMargin = false,
                        targetMargin = resources.getDimension(R.dimen.min_touch_target),
                        fadingView = btnStopActiveDash,
                        getMarginValue = MarginLayoutParams::getMarginEnd,
                        setMarginValue = MarginLayoutParams::setMarginEnd
                    )

                    activeDashDetailsTopSpacer.setVisibleIfTrue(true)
                    callback?.revealAppBarLayout(shouldShow = true)
                    root.visibility = VISIBLE
                    activeDashDetails.revealIfTrue(
                        shouldShow = true,
                        doAnyways = true
                    ) {
                        onComplete?.invoke()
                    }
                }
                else -> {
                    adbTitleBar.fadeAndGo(
                        expandMargin = true,
                        targetMargin = resources.getDimension(R.dimen.min_touch_target),
                        fadingView = btnStopActiveDash,
                        getMarginValue = MarginLayoutParams::getMarginEnd,
                        setMarginValue = MarginLayoutParams::setMarginEnd
                    )

                    activeDashDetailsTopSpacer.setVisibleIfTrue(false)
                    callback?.revealAppBarLayout(shouldShow = true, lockAppBar = true)
                    root.visibility = VISIBLE
                    activeDashDetails.revealIfTrue(
                        shouldShow = false,
                        doAnyways = true
                    ) {
                        onComplete?.invoke()
                    }


                    when (serviceState) {
                        TRACKING_COLLAPSED -> { // Show collapsed
                            startTrackingIndicator()
                        }

                        TRACKING_DISABLED -> { // Show collapsed and stop tracking indicator
                            stopTrackingIndicator()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun startTrackingIndicator() {
        binding.trackingStatusIndicator.visibility = VISIBLE

        if (!flashingIndicator.isStarted || flashingIndicator.isPaused) flashingIndicator.start()
    }

    private fun stopTrackingIndicator() {
        binding.trackingStatusIndicator.visibility = VISIBLE
        binding.trackingStatusIndicator.alpha = 1f

        flashingIndicator.pause()
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
