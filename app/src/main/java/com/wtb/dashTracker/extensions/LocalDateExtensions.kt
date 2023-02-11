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

/**
 * format: Mon Jan 01, 2022
 */
val dtfDate: DateTimeFormatter = DateTimeFormatter.ofPattern("eee MMM d, yyyy")
/**
 * format: Mon Jan 01
 */
val dtfDateThisYear: DateTimeFormatter = DateTimeFormatter.ofPattern("eee MMM d")
/**
 * format: Jan 1, 2022
 */
val dtfShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
/**
 * format: Jan 1
 */
val dtfMini: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/**
 * format: Jan 1
 */
val dtfShortDateThisYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
/**
 * format: 1:12 PM
 */
val dtfTime: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
/**
 * format: 01/01/2022 01:12:23
 */
val dtfDateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
/**
 * format: 01/01/2022 01:12 PM
 */
val dtfDateTimeOld: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a")

/**
 * format: if current year: Mon, Jan 1 else: Mon, Jan 1, 2021
 */
val LocalDate.formatted: String
    get() = if (year == LocalDate.now().year) {
        format(dtfDateThisYear)
    } else {
        format(dtfDate)
    }

/**
 * the [LocalDate] of the next [DayOfWeek]. It will return the same date if today is [untilDay]
 */
fun LocalDate.dateOfNextDayOfWeek(untilDay: DayOfWeek): LocalDate {
    val daysUntil = (untilDay.value + 7 - dayOfWeek.value) % 7
    return plusDays(daysUntil.toLong())
}

/**
 * The date of the next Sunday or today, if today is Sunday
 */
val LocalDate.endOfWeek: LocalDate get() = dateOfNextDayOfWeek(DayOfWeek.SUNDAY)

/**
 * The week number [1-53] where week 1 is the M-Su week containing January 1. This means that in
 * some cases, a date from the end of December could be in week 1 of the following year
 */
val LocalDate.weekOfYear: Int get() = endOfWeek.dayOfYear / 7 + 1



