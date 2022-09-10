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

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.ActiveDashBarBinding
import com.wtb.dashTracker.extensions.*
import dev.benica.mileagetracker.LocationService.ServiceState
import dev.benica.mileagetracker.LocationService.ServiceState.PAUSED
import dev.benica.mileagetracker.LocationService.ServiceState.STOPPED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@ExperimentalCoroutinesApi
class ActiveDashBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var binding: ActiveDashBarBinding
    private var callback: ActiveDashBarCallback? = null
    private var activeEntry: FullEntry? = null

    init {
        val view: View = inflate(context, R.layout.active_dash_bar, this)

        binding = ActiveDashBarBinding.bind(view)

        binding.pauseButton.apply {
            tag = tag ?: R.drawable.anim_pause_to_play
            setOnClickListener {
                callback?.onPauseResumeButtonClicked()
            }
        }
    }

    fun initialize(cb: ActiveDashBarCallback) {
        callback = cb
    }

    fun updateServiceState(isPaused: ServiceState) {
        when (isPaused) {
            PAUSED -> {
                fun togglePauseToPlay() {
                    binding.pauseButton.apply {
                        if (tag == R.drawable.anim_play_to_pause) {
                            toggleButtonAnimatedVectorDrawable(
                                R.drawable.anim_pause_to_play,
                                R.drawable.anim_play_to_pause
                            )
                        }
                    }
                }

                if (binding.root.visibility == GONE) {
                    binding.root.expand { togglePauseToPlay() }
                } else {
                    togglePauseToPlay()
                }
            }
            STOPPED -> {
                if (binding.root.visibility == VISIBLE) {
                    binding.root.collapse()
                }
            }
            else -> {
                fun updateElapsedTime(): () -> Unit {
                    return setTimer(1000L) {
                        val start = LocalDateTime.of(
                            activeEntry?.entry?.date ?: LocalDate.now(),
                            activeEntry?.entry?.startTime ?: LocalTime.now()
                        )
                        val end = LocalDateTime.now()
                        val elapsedSeconds: Long =
                            start.until(
                                end,
                                ChronoUnit.SECONDS
                            )

                        binding.valElapsedTime.text =
                            getElapsedHours(elapsedSeconds)
                    }
                }

                fun togglePlayToPause() {
                    binding.pauseButton.apply {
                        if (tag == R.drawable.anim_pause_to_play) {
                            toggleButtonAnimatedVectorDrawable(
                                R.drawable.anim_pause_to_play,
                                R.drawable.anim_play_to_pause
                            )
                        }
                    }
                }

                updateElapsedTime()

                if (binding.root.visibility == GONE) {
                    binding.root.expand { togglePlayToPause() }
                } else {
                    togglePlayToPause()
                }
            }
        }
    }

    fun updateEntry(fullEntry: FullEntry?, activeCpm: Float?) {
        fullEntry?.let { it ->
            activeEntry = it
            binding.valMileage.text =
                context.getString(R.string.mileage_fmt, it.distance)

            binding.valCost.text =
                context.getCurrencyString(it.distance.toFloat() * (activeCpm ?: 0f))
        }
    }

    interface ActiveDashBarCallback {
        fun onPauseResumeButtonClicked()
    }

    companion object {
        // TODO: I need to be able to stopp/pause, needs a new onTick after onResume
        private fun setTimer(delay: Long, onTick: () -> Unit): () -> Unit {
            val handler = android.os.Handler(Looper.getMainLooper())

            val r = object : Runnable {
                override fun run() {
                    onTick()
                    handler.postDelayed(this, delay)
                }
            }

            handler.post(r)

            return { handler.removeCallbacks(r) }
        }
    }
}

