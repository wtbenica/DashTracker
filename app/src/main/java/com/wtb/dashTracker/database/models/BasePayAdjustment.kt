package com.wtb.dashTracker.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.temporal.WeekFields

@Entity
data class BasePayAdjustment(
    @PrimaryKey(autoGenerate = true) val adjustmentId: Int = AUTO_ID,
    val date: LocalDate,
    val amount: Float
) : DataModel() {
    override val id: Int
        get() = adjustmentId

    val weekOfYear: Int
        get() = date.get(WeekFields.ISO.weekOfWeekBasedYear())
}
