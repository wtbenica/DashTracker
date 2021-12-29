package com.wtb.dashTracker.database.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.extensions.weekOfYear
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
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

    val isRecent: Boolean
        get() = LocalDate.now().endOfWeek.minusDays(7) <= date

    val isIncomplete: Boolean
        get() = basePayAdjustment == null
}

@ExperimentalCoroutinesApi
data class CompleteWeekly(
    @Embedded
    val weekly: Weekly,

    @Relation(parentColumn = "date", entityColumn = "week")
    val entries: List<DashEntry>
) {
    val isEmpty: Boolean
        get() = entries.isEmpty() && weekly.isIncomplete
}