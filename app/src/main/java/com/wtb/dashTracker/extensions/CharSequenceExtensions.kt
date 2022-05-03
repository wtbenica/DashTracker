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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.LocalTime

@ExperimentalCoroutinesApi
fun CharSequence.toTimeOrNull() =
    if (this.isNotEmpty()) LocalTime.parse(this, dtfTime) else null

@ExperimentalCoroutinesApi
fun CharSequence.toDateOrNull() =
    if (this.isNotEmpty()) {
        try {
            val df = dtfDate
            LocalDate.parse(this, df)
        } catch (e: Exception) {
            try {
                val df = dtfDateThisYear
                LocalDate.parse(this, df)
            } catch (e: Exception) {
                null
            }
        }
    } else {
        null
    }

fun CharSequence.toFloatOrNull(): Float? =
    if (this.isNotEmpty()) this.padStart(2, '0').toString().toFloat() else null

fun CharSequence.toIntOrNull(): Int? = if (this.isNotEmpty()) this.toString().toInt() else null