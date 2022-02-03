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

package com.wtb.dashTracker.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.util.CSVConvertible
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

const val AUTO_ID = 0

@ExperimentalCoroutinesApi
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Weekly::class,
            parentColumns = ["date"],
            childColumns = ["week"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["week"])
    ]
)
data class DashEntry(
    @PrimaryKey(autoGenerate = true) val entryId: Int = AUTO_ID,
    val date: LocalDate,
    val endDate: LocalDate = date,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val startOdometer: Float?,
    val endOdometer: Float?,
    var totalMileage: Float? = null,
    val pay: Float?,
    val otherPay: Float?,
    val cashTips: Float?,
    val numDeliveries: Int?,
    var week: LocalDate? = date.endOfWeek
) : DataModel() {
    override val id: Int
        get() = entryId

    val isIncomplete
        get() = startTime == null || endTime == null || pay == null || mileage == null || numDeliveries == null

    val startDateTime
        get() = startTime?.let { st -> LocalDateTime.of(date, st) }

    private val endDateTime
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

    val dayEarned: Float?
        get() = dayHours?.let { dh -> hourly?.let { h -> h * dh } }

    val nightEarned: Float?
        get() = nightHours?.let { dh -> hourly?.let { h -> h * dh } }

    val dayDels: Float?
        get() = dayHours?.let { dh ->
            totalHours?.let { th ->
                numDeliveries?.let { nd -> nd * (dh / th) }
            }
        }

    val nightDels: Float?
        get() = nightHours?.let { nh ->
            totalHours?.let { th ->
                numDeliveries?.let { nd -> nd * (nh / th) }
            }
        }

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

    companion object : CSVConvertible<DashEntry> {
        private val daySplitTime = LocalTime.of(17, 0)
        private val nightSplitTime = daySplitTime.minusHours(12)

        override fun fromCSV(row: Map<String, String>): DashEntry = DashEntry(
            date = LocalDate.parse(row["Start Date"]),
            endDate = LocalDate.parse(row["End Date"]),
            startTime = LocalTime.parse(row["Start Time"]),
            endTime = row["End Time"]?.let {
                if (it == "") null else LocalTime.parse(row["End Time"])
            },
            startOdometer = row["Start Odometer"]?.toFloatOrNull(),
            endOdometer = row["End Odometer"]?.toFloatOrNull(),
            totalMileage = row["Total Mileage"]?.toFloatOrNull(),
            pay = row["Base Pay"]?.toFloatOrNull(),
            cashTips = row["Cash Tips"]?.toFloatOrNull(),
            otherPay = row["Other Pay"]?.toFloatOrNull(),
            numDeliveries = row["Num Deliveries"]?.toIntOrNull()
        )

        override val headerList: List<String>
            get() = listOf(
                "Start Date",
                "Start Time",
                "End Date",
                "End Time",
                "Start Odometer",
                "End Odometer",
                "Total Mileage",
                "Base Pay",
                "Cash Tips",
                "Other Pay",
                "Num Deliveries"
            )

        override fun DashEntry.asList(): List<*> =
            listOf(
                date,
                startTime,
                endDate,
                endTime,
                startOdometer,
                endOdometer,
                totalMileage,
                pay,
                cashTips,
                otherPay,
                numDeliveries
            )
    }
}
