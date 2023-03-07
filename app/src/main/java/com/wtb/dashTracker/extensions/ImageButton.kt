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
import android.view.View
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

/**
 * Toggle button animated vector drawable - plays animation then switches animation drawables,
 * alternating on each call. Sets [View.getTag] and [View.setTag] to keep track of current drawable.
 *
 * @param initialDrawable - the first animation to play
 * @param otherDrawable - the second animation to play
 */
fun ImageButton.toggleButtonAnimatedVectorDrawable(
    @DrawableRes initialDrawable: Int,
    @DrawableRes otherDrawable: Int
) {
    // TODO: Need to add content description as well
    run {
        // If tag is already set to [initialDrawable], switches image resource to [otherDrawable]
        // Otherwise, image resource is set to [initialDrawable]
        tag = when (tag ?: otherDrawable) {
            initialDrawable -> {
                setImageResource(otherDrawable)
                otherDrawable
            }
            else -> {
                setImageResource(initialDrawable)
                initialDrawable
            }
        }
        when (val d = drawable) {
            is AnimatedVectorDrawableCompat -> d.start()
            is AnimatedVectorDrawable -> d.start()
        }
    }
}