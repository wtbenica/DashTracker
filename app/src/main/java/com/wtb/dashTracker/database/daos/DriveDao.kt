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
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.database.models.Drive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class DriveDao: BaseDao<Drive>("Drive", "driveId") {
    @Query(SQL_GET_ALL)
    abstract override fun getAll(): Flow<List<Drive>>

    @Query(SQL_GET_ALL)
    abstract suspend fun getAllSuspend(): List<Drive>

    @Query("DELETE FROM Drive")
    abstract override fun clear()

    @RawQuery(observedEntities = [Drive::class])
    abstract override fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<Drive?>

    companion object {
        private const val SQL_GET_ALL =
            """SELECT * 
                FROM Drive
                ORDER BY start"""
    }
}
