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

import android.util.Log
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Expense
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@ExperimentalCoroutinesApi
@Dao
abstract class DashEntryDao : BaseDao<DashEntry>("DashEntry", "entryId") {
    @Query(SQL_GET_ALL)
    abstract override fun getAll(): Flow<List<DashEntry>>

    @Query(SQL_GET_ALL)
    abstract suspend fun getAllSuspend(): List<DashEntry>

    @Query("DELETE FROM DashEntry")
    abstract override fun clear()

    @RawQuery(observedEntities = [DashEntry::class])
    abstract override fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<DashEntry?>

    @RawQuery(observedEntities = [DashEntry::class])
    abstract fun getEntriesByQuery(query: SupportSQLiteQuery): Flow<List<DashEntry>>

    @RawQuery(observedEntities = [DashEntry::class])
    abstract fun executeRawQuery(query: SupportSQLiteQuery): Int

    fun deleteById(id: Int): Int {
        val query = SimpleSQLiteQuery("DELETE FROM DashEntry WHERE entryId = $id")

        return executeRawQuery(query)
    }

    @Query("SELECT * FROM DashEntry WHERE date >= :startDate AND date <= :endDate ORDER BY date desc, startTime desc")
    abstract fun getEntriesByDate(
        startDate: LocalDate = LocalDate.MIN,
        endDate: LocalDate = LocalDate.MAX
    ): Flow<List<DashEntry>>

    @Query(SQL_GET_ALL)
    abstract fun getAllPagingSource(): PagingSource<Int, DashEntry>

    @Query(SQL_GET_BY_DATE)
    abstract fun getEntriesByDatePagingSource(
        startDate: LocalDate = LocalDate.MIN,
        endDate: LocalDate = LocalDate.MAX
    ): PagingSource<Int, DashEntry>

    @RawQuery(observedEntities = [Expense::class, DashEntry::class])
    abstract suspend fun getFloatByQuery(query: SupportSQLiteQuery): Float

    suspend fun getCostPerMile(date: LocalDate): Float {
        val startDate = date.minusDays(30)
        val query = SimpleSQLiteQuery(
            """SELECT (
            SELECT SUM(amount)
            FROM Expense
            WHERE date BETWEEN '$startDate' and '$date') 
        / (
            SELECT max(endOdometer) - min(startOdometer)
            from DashEntry
            WHERE date BETWEEN '$startDate' AND '$date'
            AND startOdometer != 0)"""
        )
        Log.d(APP + "DashEntryDao", "q: ${query.sql}")
        return getFloatByQuery(query)
    }

    companion object {
        private const val SQL_GET_ALL =
            """SELECT * FROM DashEntry 
            ORDER BY date desc, startTime desc"""

        private const val SQL_GET_BY_DATE =
            """SELECT * FROM DashEntry 
            WHERE date >= :startDate 
            AND date <= :endDate 
            ORDER BY date desc, startTime desc"""
    }
}