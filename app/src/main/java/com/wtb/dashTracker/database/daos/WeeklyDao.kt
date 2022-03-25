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

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.database.models.Weekly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@ExperimentalCoroutinesApi
@Dao
abstract class WeeklyDao : BaseDao<Weekly>("weekly", "date") {
    @Query(SQL_GET_ALL)
    abstract override fun getAll(): Flow<List<Weekly>>

    @Transaction
    @Query(SQL_GET_ALL)
    abstract fun getAllCompleteWeekly(): Flow<List<CompleteWeekly>>

    @Transaction
    @Query(SQL_GET_ALL)
    abstract fun getAllPagingSource(): PagingSource<Int, CompleteWeekly>

    @Query(SQL_GET_ALL)
    abstract suspend fun getAllSuspend(): List<Weekly>

    @Query("DELETE FROM Weekly")
    abstract override fun clear()

    @RawQuery(observedEntities = [Weekly::class])
    abstract override fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<Weekly?>

    @Transaction
    @Query("SELECT * FROM Weekly WHERE date = :date LIMIT 1")
    abstract fun getWeeklyByDate(date: LocalDate): Flow<CompleteWeekly?>

    companion object {
        private const val SQL_GET_ALL = "SELECT * FROM Weekly ORDER BY date DESC"
    }
}