package com.wtb.dashTracker.extensions

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val dtfDate: DateTimeFormatter = DateTimeFormatter.ofPattern("eee MMM dd, yyyy")
val dtfDateThisYear: DateTimeFormatter = DateTimeFormatter.ofPattern("eee MMM dd")
val dtfShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
val dtfShortDateThisYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
val dtfTime: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

//val LocalDate.weekOfYear: Int
//    get() {
//        val weekFields: TemporalField = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
//        return get(weekFields)
//    }

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



