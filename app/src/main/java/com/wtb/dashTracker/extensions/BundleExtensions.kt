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

import android.os.Bundle

fun Bundle.getIntNotZero(key: String): Int? = getInt(key).let {
    if (it != 0) {
        it
    } else {
        null
    }
}

/**
 * Get long not zero - gets the value from bundle for [key]
 *
 * @return the value from bundle for [key] or null if value is 0
 */
fun Bundle.getLongNotZero(key: String): Long? = getLong(key).let {
    if (it != 0L) {
        it
    } else {
        null
    }
}

