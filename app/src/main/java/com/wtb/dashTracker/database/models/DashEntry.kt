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

import androidx.room.*
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.extensions.toIntOrNull
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import dev.benica.csvutil.CSVConvertible
import dev.benica.csvutil.CSVConvertible.Column
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.reflect.KProperty1

const val AUTO_ID = 0L

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
    @PrimaryKey(autoGenerate = true) val entryId: Long = AUTO_ID,
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
) : DataModel(), ListItemType {

    override val id: Long
        get() = entryId

    val isIncomplete
        get() = startTime == null || endTime == null || pay == null || mileage == null || numDeliveries == null

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
        get() = numDeliveries?.let { nd ->
            totalEarned?.let { te ->
                if (nd > 0) {
                    te / nd
                } else {
                    0f
                }
            }
        }

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
        var result = entryId.toInt()
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

    fun getHourly(costPerMile: Float): Float? = totalHours?.let { th ->
        totalEarned?.let { te ->
            if (th != 0f) {
                (te - getExpenses(costPerMile)) / th
            } else {
                0f
            }
        }
    }

    fun getAvgDelivery(costPerMile: Float): Float? = numDeliveries?.let { nd ->
        totalEarned?.let { te ->
            if (nd > 0) {
                (te - getExpenses(costPerMile)) / nd
            } else {
                0f
            }
        }
    }

    fun getNet(costPerMile: Float): Float? =
        totalEarned?.let { te -> te - getExpenses(costPerMile) }

    companion object : CSVConvertible<DashEntry> {
        private val daySplitTime = LocalTime.of(17, 0)
        private val nightSplitTime = daySplitTime.minusHours(12)

        override val saveFileName: String
            get() = "entries"

        private enum class Columns(val headerName: String, val getValue: KProperty1<DashEntry, *>) {
            ENTRY_ID("Entry ID", DashEntry::entryId),
            START_DATE("Start Date", DashEntry::date),
            START_TIME("Start Time", DashEntry::startTime),
            END_DATE("End Date", DashEntry::endDate),
            END_TIME("End Time", DashEntry::endTime),
            START_ODO("Start Odometer", DashEntry::startOdometer),
            END_ODO("End Odometer", DashEntry::endOdometer),
            MILEAGE("Total Mileage", DashEntry::totalMileage),
            BASE_PAY("Base Pay", DashEntry::pay),
            CASH_TIPS("Cash Tips", DashEntry::cashTips),
            OTHER_PAY("Other Pay", DashEntry::otherPay),
            NUM_DELIVERIES("Num Deliveries", DashEntry::numDeliveries),
            LAST_UPDATED("Last Updated", DashEntry::lastUpdated)
        }

        @Throws(IllegalStateException::class)
        override fun fromCSV(row: Map<String, String>): DashEntry {
            val idColumnValue = row[Columns.ENTRY_ID.headerName]
            val entryId: Long = idColumnValue?.toLongOrNull()
                ?: throw IllegalStateException("Entry ID must be filled with a valid value, not $idColumnValue")
            val week = LocalDate.parse(row[Columns.START_DATE.headerName]).endOfWeek
            return DashEntry(
                entryId = entryId,
                date = LocalDate.parse(row[Columns.START_DATE.headerName]),
                endDate = LocalDate.parse(row[Columns.END_DATE.headerName]),
                startTime = LocalTime.parse(row[Columns.START_TIME.headerName]),
                endTime = row[Columns.END_TIME.headerName]?.let {
                    if (it == "") null else LocalTime.parse(row[Columns.END_TIME.headerName])
                },
                startOdometer = row[Columns.START_ODO.headerName]?.toFloatOrNull(),
                endOdometer = row[Columns.END_ODO.headerName]?.toFloatOrNull(),
                totalMileage = row[Columns.MILEAGE.headerName]?.toFloatOrNull(),
                pay = row[Columns.BASE_PAY.headerName]?.toFloatOrNull(),
                cashTips = row[Columns.CASH_TIPS.headerName]?.toFloatOrNull(),
                otherPay = row[Columns.OTHER_PAY.headerName]?.toFloatOrNull(),
                numDeliveries = row[Columns.NUM_DELIVERIES.headerName]?.toIntOrNull(),
                week = week
            ).apply {
                lastUpdated = LocalDate.parse(row[Columns.LAST_UPDATED.headerName])
            }
        }

        override fun getColumns(): Array<Column<DashEntry>> =
            Columns.values().map { Column(it.headerName, it.getValue) }.toTypedArray()
    }
}

@ExperimentalCoroutinesApi
data class FullEntry(
    @Embedded
    val entry: DashEntry,

    @Relation(parentColumn = "entryId", entityColumn = "entry")
    val locations: List<LocationData>,
) : ListItemType {
    val netTime: Long?
        get() = entry.startDateTime?.until(LocalDateTime.now(), ChronoUnit.SECONDS)


    /**
     * Total distance from beginning to end of dash.
     */
    val distance: Double
        get() = locations.distance

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FullEntry

        if (entry != other.entry) return false
        if (locations != other.locations) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entry.hashCode()
        result = 31 * result + locations.hashCode()
        return result
    }
}
