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

package com.wtb.dashTracker.extensions

import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes

fun EditText.onTextChangeUpdateTotal(
    updateView: TextView,
    otherValue: Float?,
    @StringRes stringFormat: Int? = null,
    operation: (Float, Float) -> Float
) {
    val self: Float = text?.toFloatOrNull() ?: 0f
    val newTotal = operation(otherValue ?: 0f, self)
    updateView.text = if (newTotal == 0f) {
        null
    } else {
        if (stringFormat != null) {
            context.getString(stringFormat, newTotal)
        } else {
            context.getFloatString(newTotal).dropLast(1)
        }
    }
}