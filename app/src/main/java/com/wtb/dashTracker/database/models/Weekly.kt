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

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.wtb.dashTracker.database.models.Weekly.Companion.Columns.*
import com.wtb.dashTracker.extensions.weekOfYear
import com.wtb.dashTracker.util.CSVConvertible
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import kotlin.reflect.KProperty1

@Entity
data class Weekly(
    @PrimaryKey val date: LocalDate,
    var basePayAdjustment: Float? = null,
    val weekNumber: Int = date.weekOfYear,
    var isNew: Boolean = false
) : DataModel() {
    override val id: Int
        get() = "${date.year}${date.monthValue}${date.dayOfMonth}".toInt()

    val isIncomplete: Boolean
        get() = basePayAdjustment == null

    companion object : CSVConvertible<Weekly> {
        private enum class Columns(val headerName: String) {
            DATE("Start of Week"),
            ADJUST("Base Pay Adjustment"),
            WEEK_NUM("Week Number"),
            IS_NEW("isNew")
        }

        override val headerList: List<String>
            get() = Columns.values().map(Columns::headerName)

        override fun fromCSV(row: Map<String, String>): Weekly =
            Weekly(
                date = LocalDate.parse(row[DATE.headerName]),
                basePayAdjustment = row[ADJUST.headerName]?.toFloatOrNull(),
                isNew = row[IS_NEW.headerName]?.toBoolean() ?: false,
            )

        override fun Weekly.asList(): List<*> =
            listOf(
                date,
                basePayAdjustment,
                weekNumber,
                isNew
            )
    }

}

@ExperimentalCoroutinesApi
data class CompleteWeekly(
    @Embedded
    val weekly: Weekly,

    @Relation(parentColumn = "date", entityColumn = "week")
    val entries: List<DashEntry>
) {
    val isEmpty: Boolean
        get() = entries.isEmpty() && weekly.isIncomplete && !weekly.isNew

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

    private val numDeliveries: Int
        get() = getTotalForWeek(DashEntry::numDeliveries)

    val miles: Float
        get() = getTotalForWeek(DashEntry::mileage)

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

    fun getNet(costPerMile: Float): Float = totalPay - miles * costPerMile

    fun getHourly(cpm: Float): Float = getNet(cpm) / hours

    fun getAvgDelivery(cpm: Float): Float = getNet(cpm) / numDeliveries
}
