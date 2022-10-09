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
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.ExperimentalTextApi
import com.wtb.dashTracker.databinding.FabFlyoutButtonBinding
import com.wtb.dashTracker.ui.activity_main.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalCoroutinesApi
class FabMenuButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val buttonInfo: FabMenuButtonInfo? = null,
    private val callback: FabMenuButtonCallback? = null
) : LinearLayout(context, attrs) {

    init {
        val binding = FabFlyoutButtonBinding.inflate(LayoutInflater.from(context), this, true)
        binding.fabFlyoutLabel.text = buttonInfo?.label

        binding.fabFlyoutButtonButton.apply {
            buttonInfo?.resId?.let {
                val drawable = AppCompatResources.getDrawable(context, it)
                setImageDrawable(drawable)
            }

            setOnClickListener {
                callback?.fabMenuClicked()
                buttonInfo?.action?.invoke(it)
            }

            backgroundTintList = ColorStateList.valueOf(MainActivity.getColorFab(context))
        }
    }

    interface FabMenuButtonCallback {
        fun fabMenuClicked()
    }

    companion object {

        fun newInstance(
            context: Context,
            buttonInfo: FabMenuButtonInfo,
            callback: FabMenuButtonCallback
        ): FabMenuButton {
            return FabMenuButton(context, buttonInfo = buttonInfo, callback = callback)
        }
    }
}

data class FabMenuButtonInfo(
    val label: String,
    @DrawableRes val resId: Int,
    val action: (View) -> Unit,
)