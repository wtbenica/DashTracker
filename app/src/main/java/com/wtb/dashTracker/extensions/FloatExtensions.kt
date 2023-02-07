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

import kotlin.math.floor

/**
 * @return 2.83234f -> "2.83", 2f -> "2", 2.0f -> "2", "2.1f" -> "2.10"
 */
fun Float.toCurrencyString(): String =
    if (this != floor(this)) {
        "%.2f".format(this)
    } else {
        "%.0f".format(this)
    }