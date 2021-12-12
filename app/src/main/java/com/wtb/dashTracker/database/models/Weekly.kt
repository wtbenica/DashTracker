package com.wtb.dashTracker.database.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.wtb.dashTracker.extensions.weekOfYear
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

@Entity
data class Weekly(
    @PrimaryKey val date: LocalDate,
    var basePayAdjustment: Float? = null,
    val weekNumber: Int = date.weekOfYear
) : DataModel() {
    override val id: Int
        get() = "${date.year}${date.monthValue}${date.dayOfMonth}".toInt()

    val weekOfYear: Int
        get() = date.get(WeekFields.ISO.weekOfWeekBasedYear())
}