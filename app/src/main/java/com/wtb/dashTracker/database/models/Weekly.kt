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
import kotlin.reflect.KProperty1

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

    internal val hours: Float
        get() = getTotalForWeek(DashEntry::totalHours)

    internal val regularPay: Float
        get() = getTotalForWeek(DashEntry::pay)

    internal val cashTips: Float
        get() = getTotalForWeek(DashEntry::cashTips)

    internal val otherPay: Float
        get() = getTotalForWeek(DashEntry::otherPay)

    internal val pay: Float
        get() = getTotalForWeek(DashEntry::totalEarned)

    internal val numDeliveries: Int
        get() = getTotalForWeek(DashEntry::numDeliveries)

    private fun getTotalForWeek(field: KProperty1<DashEntry, Float?>) = entries.map(field)
        .fold(0f) { acc, fl -> acc + (fl ?: 0f) }

    private fun getTotalForWeek(field: KProperty1<DashEntry, Int?>) = entries.map(field)
        .fold(0) { acc, fl -> acc + (fl ?: 0) }

    internal val totalPay: Float
        get() = pay + (weekly.basePayAdjustment ?: 0f)

    val hourly: Float?
        get() = if (hours > 0f) {
            totalPay / hours
        } else {
            null
        }

    val avgDelivery: Float?
        get() = if (numDeliveries > 0) {
            totalPay / numDeliveries
        } else {
            null
        }

    val delPerHour: Float?
        get() = if (numDeliveries > 0 && hours > 0f) {
            numDeliveries / hours
        } else {
            null
        }

}