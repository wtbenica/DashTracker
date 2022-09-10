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

package com.wtb.dashTracker.extensions

import android.graphics.drawable.AnimatedVectorDrawable
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

fun ImageButton.toggleButtonAnimatedVectorDrawable(
    @DrawableRes initialDrawable: Int,
    @DrawableRes otherDrawable: Int
) {
    // TODO: Need to add content description as well
    run {
        when (tag ?: otherDrawable) {
            otherDrawable -> {
                setImageResource(initialDrawable)
                tag = initialDrawable
            }
            initialDrawable -> {
                setImageResource(otherDrawable)
                tag = otherDrawable
            }
        }
        when (val d = drawable) {
            is AnimatedVectorDrawableCompat -> d.start()
            is AnimatedVectorDrawable -> d.start()
        }
    }
}