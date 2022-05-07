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

package com.wtb.dashTracker.database.daos

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.database.models.Purpose
import com.wtb.dashTracker.repository.DeductionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

fun <T> List<T>.toStringList(): String {
    val res = StringBuilder()
    forEachIndexed { index, t ->
        res.append(t)
        if (index < size - 1)
            res.append(", ")
    }
    return res.toString()
}

@Dao
@ExperimentalCoroutinesApi
abstract class TransactionDao {

    @RawQuery(observedEntities = [Expense::class, DashEntry::class])
    abstract suspend fun getFloatByQuery(query: SupportSQLiteQuery): Float?

    private suspend fun getCpm(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        vararg purpose: Purpose? = emptyArray()
    ): Float {
        val query = SimpleSQLiteQuery(
            """SELECT (
            SELECT SUM(amount)
            FROM Expense
            WHERE date BETWEEN '$startDate' and '$endDate'"""
                    + if (purpose.isNotEmpty()) {
                """ AND purpose in (${purpose.mapNotNull { it?.id }.toStringList()})) """
            } else {
                ")"
            } +
                    """ / (
                        SELECT max (endOdometer) - min(startOdometer)
                                from DashEntry
                                WHERE date BETWEEN '$startDate' AND '$endDate'
                                AND startOdometer != 0)"""
        )

        return getFloatByQuery(query) ?: 0f
    }

    data class Cpm(
        val date: LocalDate,
        val cpm: Float
    )

    data class NewCpm(
        val date: LocalDate,
        val gasOnlyCpm: Float,
        val actualCpm: Float,
        val irsStdCpm: Float
    ) {
        fun getCpm(deductionType: DeductionType): Float =
            when (deductionType) {
                DeductionType.NONE -> 0f
                DeductionType.GAS_ONLY -> gasOnlyCpm
                DeductionType.ALL_EXPENSES -> actualCpm
                DeductionType.IRS_STD -> irsStdCpm
            }
    }

    suspend fun getCostPerMileByDate(
        date: LocalDate,
        vararg purpose: Purpose? = emptyArray()
    ): Float {
        val startDate = date.minusDays(NUM_DAYS_HISTORY)

        return getCpm(startDate, date, *purpose)
    }

    suspend fun getCostPerMileAnnual(
        year: Int,
        vararg purpose: Purpose? = emptyArray()
    ): Float {
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)

        return getCpm(startDate, endDate, *purpose)
    }

    companion object {
        private const val NUM_DAYS_HISTORY: Long = 30
    }
}
