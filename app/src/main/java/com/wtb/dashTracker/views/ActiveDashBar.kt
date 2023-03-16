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
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.ActivityMainActiveDashBarBinding
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getElapsedHours
import com.wtb.dashTracker.extensions.getStringOrElse
import com.wtb.dashTracker.extensions.transitionBackgroundTo
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

        animator = ValueAnimator.ofFloat(0.4f, 1f).apply {
            duration = 1000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = -1
            addUpdateListener { animation ->
                binding.trackingStatusOverlay.alpha = animation.animatedValue as Float
            }
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
    fun onServiceStateUpdated(serviceState: ADBState, onComplete: (() -> Unit)? = null) {
        when (serviceState) {
            INACTIVE -> { // Always collapse
                binding.root.revealIfTrue(shouldShow = false, doAnyways = true) {
                    onComplete?.invoke()
                }
                animator.pause()
            }
            TRACKING_FULL -> { // Always show expanded details
                binding.activeDashDetails.revealIfTrue(shouldShow = true, doAnyways = true) {
                    binding.apply {
                        root.apply {
                            revealIfTrue(shouldShow = true, doAnyways = true) {
                                callback?.revealAppBarLayout(shouldShow = true) {
                                    onComplete?.invoke()
                                } ?: onComplete?.invoke()
                            }
                            val hor = resources.getDimension(R.dimen.margin_narrow).toInt()
                            updatePadding(left = hor, top = hor, right = hor, bottom = hor)
                            requestLayout()
                        }

                        val hor = resources.getDimension(R.dimen.margin_wide).toInt()
                        listOf(adbTitleRow, trackingStatusRow).forEach {
                            it.updatePadding(left = hor, right = hor)
                        }
                    }
                }
                startTrackingIndicator()
            }
            TRACKING_COLLAPSED -> { // Show collapsed
                binding.activeDashDetails.revealIfTrue(shouldShow = false, doAnyways = true) {
                    binding.apply {
                        root.apply {
                            revealIfTrue(shouldShow = true, doAnyways = true) {
                                callback?.revealAppBarLayout(shouldShow = true) {
                                    onComplete?.invoke()
                                } ?: onComplete?.invoke()
                            }
                            setPadding(0)
                            requestLayout()
                        }

                        val hor = resources.getDimension(R.dimen.margin_default).toInt()
                        listOf(adbTitleRow, trackingStatusRow).forEach {
                            it.updatePadding(left = hor, right = hor)
                        }
                    }
                }
            }
            NOT_TRACKING -> { // Show collapsed and stop tracking indicator
                binding.activeDashDetails.revealIfTrue(shouldShow = false, doAnyways = true) {
                    binding.root.apply {
                        revealIfTrue(shouldShow = true, doAnyways = true) {
                            callback?.revealAppBarLayout(shouldShow = true) {
                                onComplete?.invoke()
                            } ?: onComplete?.invoke()
                        }
                        transitionBackgroundTo(R.attr.colorActiveDashBarBg)
                    }
                }
                stopTrackingIndicator()
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
        binding.trackingStatusOverlay.alpha = 1f

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
            onComplete: (() -> Unit)? = null
        )
    }

    companion object {
        enum class ADBState {
            TRACKING_FULL, TRACKING_COLLAPSED, NOT_TRACKING, INACTIVE
        }
    }
}
