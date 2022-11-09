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
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout
import androidx.annotation.AttrRes
import com.wtb.dashTracker.R
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.ActiveDashBarBinding
import com.wtb.dashTracker.extensions.collapse
import com.wtb.dashTracker.extensions.expand
import com.wtb.dashTracker.extensions.getCurrencyString
import com.wtb.dashTracker.extensions.getElapsedHours
import dev.benica.mileagetracker.LocationService.ServiceState
import dev.benica.mileagetracker.LocationService.ServiceState.STOPPED
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ActiveDashBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {

    private var binding: ActiveDashBarBinding
    private var callback: ActiveDashBarCallback? = null
    private var activeEntry: FullEntry? = null

    init {
        val view: View = inflate(context, R.layout.active_dash_bar, this)

        binding = ActiveDashBarBinding.bind(view)
    }

    fun initialize(cb: ActiveDashBarCallback) {
        callback = cb
    }

    fun updateServiceState(serviceState: ServiceState) {
        when (serviceState) {
            STOPPED -> {
                if (binding.root.visibility == VISIBLE) {
                    binding.root.collapse()
                }
            }
            else -> {
                if (binding.root.visibility == GONE) {
                    binding.root.expand()
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

            binding.valElapsedTime.text =
                getElapsedHours(it.netTime)
        }
    }

    interface ActiveDashBarCallback
}

