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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

@Dao
@ExperimentalCoroutinesApi
abstract class TransactionDao {

    @RawQuery(observedEntities = [Expense::class, DashEntry::class])
    abstract suspend fun getFloatByQuery(query: SupportSQLiteQuery): Float

    suspend fun getAnnualCostPerMile(
        year: Int,
        purpose: Purpose? = null
    ): Float {
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)

        val query = SimpleSQLiteQuery(
            """SELECT (
            SELECT SUM(amount)
            FROM Expense
            WHERE date BETWEEN '$startDate' and '$endDate'"""
                    + if (purpose != null) {
                """ AND purpose = ${purpose.id}) """
            } else {
                ")"
            } +
                    """ / (
                        SELECT max (endOdometer) - min(startOdometer)
                                from DashEntry
                                WHERE date BETWEEN '$startDate' AND '$endDate'
                                AND startOdometer != 0)"""
        )

        return getFloatByQuery(query)
    }
}