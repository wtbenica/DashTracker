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
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.ActiveDashBarBinding
import com.wtb.dashTracker.extensions.*
import com.wtb.dashTracker.ui.activity_main.TAG
import dev.benica.mileagetracker.LocationService.ServiceState
import dev.benica.mileagetracker.LocationService.ServiceState.STOPPED
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

    var isPaused = false
        set(value) {
            field = value
            toto()
        }

    fun toto() {
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

        if (isPaused) {
            Log.d(TAG, "IS PAUSED")
            togglePauseToPlay()
        } else {
            Log.d(TAG, "IS NOT PAUSED")
            togglePlayToPause()
        }
    }

    fun updateServiceState(serviceState: ServiceState) {
        when (serviceState) {
            STOPPED -> {
                if (binding.root.visibility == VISIBLE) {
                    Log.d(TAG, "Stopped | Hiding ActiveDashBar")
                    binding.root.collapse()
                } else {
                    Log.d(TAG, "Stopped | ActiveDashBar Hidden")
                }
            }
            else -> {
                if (binding.root.visibility == GONE) {
                    Log.d(TAG, "Tracking | Expanding ActiveDashBar")
                    binding.root.expand { toto() }
                } else {
                    Log.d(TAG, "Tracking | ActiveDashBar already expanded")
                    toto()
                }
            }
        }
    }

    fun updateEntry(fullEntry: FullEntry?, activeCpm: Float?) {
        Log.d(TAG, "update entry: $fullEntry")
        fullEntry?.let { it ->
            activeEntry = it
            binding.valMileage.text =
                context.getString(R.string.mileage_fmt, it.distance)

            binding.valCost.text =
                context.getCurrencyString(it.distance.toFloat() * (activeCpm ?: 0f))

            binding.valElapsedTime.text =
                getElapsedHours(it.netTime)
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

