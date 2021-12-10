package com.wtb.dashTracker.extensions

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

val dtfDate: DateTimeFormatter = DateTimeFormatter.ofPattern("eee MMM dd, yyyy")
val dtfDateThisYear: DateTimeFormatter = DateTimeFormatter.ofPattern("eee MMM dd")
val dtfTime: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

val LocalDate.weekOfYear: Int
    get() {
        val weekFields = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
        return get(weekFields)
    }

val LocalDate.formatted: String
    get() = if (year == LocalDate.now().year) {
        format(dtfDateThisYear)
    } else {
        format(dtfDate)
    }
