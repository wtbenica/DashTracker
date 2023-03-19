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
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wtb.dashTracker.R

class DTFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleRes: Int = 0
) : FloatingActionButton(context, attrs, defStyleRes) {

    var currentState: FabState = FabState.DASH_INACTIVE
    fun updateIcon(currFragId: Int, isTracking: Boolean) {
        var playAnimation = true
        currentState = when {
            currFragId == R.id.navigation_expenses -> {
                when (currentState) {
                    FabState.DASH_INACTIVE -> {
                        setImageResource(R.drawable.anim_play_to_plus)
                    }
                    FabState.DASH_ACTIVE -> {
                        setImageResource(R.drawable.anim_stop_to_plus)
                    }
                    FabState.EXPENSE_FRAG -> {
                        setImageResource(R.drawable.anim_plus_to_play)
                        playAnimation = false
                    }
                }

                FabState.EXPENSE_FRAG
            }
            isTracking -> {
                when (currentState) {
                    FabState.EXPENSE_FRAG -> {
                        setImageResource(R.drawable.anim_plus_to_stop)
                    }
                    FabState.DASH_INACTIVE -> {
                        setImageResource(R.drawable.anim_play_to_stop)
                    }
                    FabState.DASH_ACTIVE -> {
                        setImageResource(R.drawable.anim_stop_to_play)
                        playAnimation = false
                    }
                }

                FabState.DASH_ACTIVE
            }
            else -> {
                when (currentState) {
                    FabState.DASH_ACTIVE -> {
                        setImageResource(R.drawable.anim_stop_to_play)
                    }
                    FabState.EXPENSE_FRAG -> {
                        setImageResource(R.drawable.anim_plus_to_play)
                    }
                    FabState.DASH_INACTIVE -> {
                        setImageResource(R.drawable.anim_play_to_stop)
                        playAnimation = false
                    }
                }

                FabState.DASH_INACTIVE
            }
        }

        if (playAnimation) {
            when (val d = drawable) {
                is AnimatedVectorDrawableCompat -> d.start()
                is AnimatedVectorDrawable -> d.start()
            }
        }
    }

    companion object {
        enum class FabState {
            DASH_INACTIVE, DASH_ACTIVE, EXPENSE_FRAG
        }
    }
}