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
import com.wtb.dashTracker.database.models.DashEntry.Companion.Columns.*
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
    val date: LocalDate = LocalDate.now(),
    val endDate: LocalDate = date,
    val startTime: LocalTime? = LocalTime.now(),
    val endTime: LocalTime? = null,
    val startOdometer: Float? = null,
    val endOdometer: Float? = null,
    var totalMileage: Float? = null,
    val pay: Float? = null,
    val otherPay: Float? = null,
    val cashTips: Float? = null,
    val numDeliveries: Int? = null,
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
                val endsDuringDay = endTime in nightSplitTime..daySplitTime
                val startsEarly = startTime < nightSplitTime

                if (date == endDate) {
                    if (startTime == endTime) 0f
                    else if (startsDuringDay && endsDuringDay) totalHours
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
                numDeliveries?.let { nd -> nd * (dh / (if (th != 0f) th else 1f)) }
            }
        }

    val nightDels: Float?
        get() = nightHours?.let { nh ->
            totalHours?.let { th ->
                numDeliveries?.let { nd -> nd * (nh / (if (th != 0f) th else 1f)) }
            }
        }

    val hourly: Float?
        get() = totalHours?.let { th ->
            totalEarned?.let { te -> if (th != 0f) te / th else 0f }
        }

    val avgDelivery: Float?
        get() = numDeliveries?.let { nd -> totalEarned?.let { te -> te / nd } }

    val hourlyDeliveries: Float?
        get() = totalHours?.let { th ->
            numDeliveries?.let { nd -> if (th != 0f) nd / th else 0f }
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

    fun getExpenses(costPerMile: Float): Float = (mileage ?: 0f) * costPerMile

    companion object : CSVConvertible<DashEntry> {
        private val daySplitTime = LocalTime.of(17, 0)
        private val nightSplitTime = daySplitTime.minusHours(12)

        private enum class Columns(val headerName: String) {
            START_DATE("Start Date"),
            START_TIME("Start Time"),
            END_DATE("End Date"),
            END_TIME("End Time"),
            START_ODO("Start Odometer"),
            END_ODO("End Odometer"),
            MILEAGE("Total Mileage"),
            BASE_PAY("Base Pay"),
            CASH_TIPS("Cash Tips"),
            OTHER_PAY("Other Pay"),
            NUM_DELIVERIES("Num Deliveries")
        }

        override fun fromCSV(row: Map<String, String>): DashEntry = DashEntry(
            date = LocalDate.parse(row[START_DATE.headerName]),
            endDate = LocalDate.parse(row[END_DATE.headerName]),
            startTime = LocalTime.parse(row[START_TIME.headerName]),
            endTime = row[END_TIME.headerName]?.let {
                if (it == "") null else LocalTime.parse(row[END_TIME.headerName])
            },
            startOdometer = row[START_ODO.headerName]?.toFloatOrNull(),
            endOdometer = row[END_ODO.headerName]?.toFloatOrNull(),
            totalMileage = row[MILEAGE.headerName]?.toFloatOrNull(),
            pay = row[BASE_PAY.headerName]?.toFloatOrNull(),
            cashTips = row[CASH_TIPS.headerName]?.toFloatOrNull(),
            otherPay = row[OTHER_PAY.headerName]?.toFloatOrNull(),
            numDeliveries = row[NUM_DELIVERIES.headerName]?.toIntOrNull(),
            week = LocalDate.parse(row[START_DATE.headerName]).endOfWeek
        )

        override val headerList: List<String>
            get() = Columns.values().map(Columns::headerName)

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
