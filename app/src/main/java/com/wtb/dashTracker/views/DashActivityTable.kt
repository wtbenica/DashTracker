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
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TableLayout
import androidx.annotation.AttrRes
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.databinding.PauseStuffBinding

class DashActivityTable @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TableLayout(context, attrs) {
    private var binding: PauseStuffBinding
    private var entry: FullEntry? = null
        set(value) {
            field = value
            updateUI()
        }
    init {
        binding = PauseStuffBinding.inflate(LayoutInflater.from(context))
    }

    fun updateUI() {

    }
}

class DashActivityRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

}