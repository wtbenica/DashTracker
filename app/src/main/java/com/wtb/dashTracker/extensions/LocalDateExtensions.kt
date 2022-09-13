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

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val dtfDate: DateTimeFormatter = DateTimeFormatter.ofPattern("eee MMM dd, yyyy")
val dtfDateThisYear: DateTimeFormatter = DateTimeFormatter.ofPattern("eee MMM dd")
val dtfShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
val dtfMini: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
val dtfShortDateThisYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
val dtfTime: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
val dtfDateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
val dtfDateTimeOld: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a")
val dtfDateTime2 = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")
val LocalDate.formatted: String
    get() = if (year == LocalDate.now().year) {
        format(dtfDateThisYear)
    } else {
        format(dtfDate)
    }

val LocalDate.shortFormat: String
    get() = if (year == LocalDate.now().year) {
        format(dtfShortDateThisYear)
    } else {
        format(dtfShortDate)
    }

fun LocalDate.getNextDateFor(untilDay: DayOfWeek): LocalDate {
    val daysUntil = (untilDay.value + 7 - dayOfWeek.value) % 7
    return plusDays(daysUntil.toLong())
}

val LocalDate.endOfWeek: LocalDate
    get() = getNextDateFor(DayOfWeek.SUNDAY)

val LocalDate.weekOfYear: Int
    get() = endOfWeek.dayOfYear / 7 + 1



