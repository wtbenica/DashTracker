package com.wtb.dashTracker.database

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.MainActivity.Companion.getThisWeeksDateRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

val AUTO_ID = 0

@ExperimentalCoroutinesApi
@Entity
data class DashEntry(
    @PrimaryKey(autoGenerate = true) val entryId: Int = AUTO_ID,
    val date: LocalDate,
    val endDate: LocalDate = date,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val startOdometer: Float?,
    val endOdometer: Float?,
    var totalMileage: Float?,
    val pay: Float?,
    val otherPay: Float?,
    val cashTips: Float?,
    val numDeliveries: Int?
) : DataModel() {
    override val id: Int
        get() = entryId

    val isIncomplete
        get() = startTime == null || endTime == null || pay == null

    val startDateTime
        get() = startTime?.let { st -> LocalDateTime.of(date, st) }

    val endDateTime
        get() = endTime?.let { et -> LocalDateTime.of(endDate, et) }

    val totalHours: Float?
        get() = startDateTime?.let { st ->
            endDateTime?.let { et ->
                Duration.between(st, et).toMinutes().toFloat() / 60
            }
        }

    val dayHours: Float?
        get() = startTime?.let {
            endTime?.let {
                val startsDuringDay = startTime in nightSplitTime..daySplitTime
                val endsDuringday = endTime in nightSplitTime..daySplitTime
                val startsEarly = startTime < nightSplitTime

                if (date == endDate) {
                    if (startsDuringDay && endsDuringday) totalHours
                    else if (startTime >= daySplitTime) 0f
                    else Duration.between(startTime, daySplitTime).toMinutes() / 60f
                } else {
                    var res = 0f
                    // day 1
                    if (startsDuringDay) {
                        res += Duration.between(startTime, daySplitTime).toMinutes() / 60f
                    } else if (startsEarly) {
                        res += 12f
                    }
                    // day 2
                    if (endTime in nightSplitTime..daySplitTime) {
                        res += Duration.between(nightSplitTime, endTime).toMinutes() / 60f
                    } else if (endTime > daySplitTime) {
                        res += 12f
                    }
                    res
                }
            }
        }

    val nightHours: Float?
        get() = dayHours?.let { dh ->
            totalHours?.let { tot -> tot - dh }
        }

    val totalEarned: Float?
        get() = pay?.let { p -> p + (otherPay ?: 0f) + (cashTips ?: 0f) }

//    val dayEarned: Float?
//        get() = dayHours?.let { dh -> hourly?.let { h -> h * dh } }
//
//    val nightEarned: Float?
//        get() = nightHours?.let { dh -> hourly?.let { h -> h * dh } }

    val hourly: Float?
        get() = totalHours?.let { th ->
            totalEarned?.let { te -> te / th }
        }

    val avgDelivery: Float?
        get() = numDeliveries?.let { nd -> totalEarned?.let { te -> te / nd } }

    val hourlyDeliveries: Float?
        get() = totalHours?.let { th ->
            numDeliveries?.let { nd ->
                nd / th
            }
        }

    val mileage: Float?
        get() = totalMileage ?: startOdometer?.let { so -> endOdometer?.let { eo -> eo - so } }

    fun isThisWeek(): Boolean {
        val (start, end) = getThisWeeksDateRange()
        Log.d(TAG, "D: $date S: $start E: $end")
        return date in start..end
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DashEntry

        if (entryId != other.entryId) return false
        if (date != other.date) return false
        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (startOdometer != other.startOdometer) return false
        if (endOdometer != other.endOdometer) return false
        if (totalMileage != other.totalMileage) return false
        if (pay != other.pay) return false
        if (cashTips != other.cashTips) return false
        if (numDeliveries != other.numDeliveries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entryId
        result = 31 * result + date.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + startOdometer.hashCode()
        result = 31 * result + endOdometer.hashCode()
        result = 31 * result + totalMileage.hashCode()
        result = 31 * result + pay.hashCode()
        result = 31 * result + cashTips.hashCode()
        result = 31 * result + numDeliveries.hashCode()
        return result
    }

    override fun toString(): String = "$date: $startTime - $endTime $$totalEarned"

    companion object {
        private const val TAG = APP + "DashEntry"
        private val daySplitTime = LocalTime.of(17, 0)
        private val nightSplitTime = daySplitTime.minusHours(12)

    }
}