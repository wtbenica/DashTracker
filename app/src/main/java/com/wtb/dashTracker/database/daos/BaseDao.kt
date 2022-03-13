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

import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.database.models.DataModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class BaseDao<T : DataModel>(private val tableName: String, private val idName: String = tableName + "Id") {

    @RawQuery
    protected abstract fun getDataModelByQuery(query: SupportSQLiteQuery): T?

    fun get(id: Int): T? {
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName WHERE $idName = $id")

        return getDataModelByQuery(query)
    }

    abstract fun clear()

    protected abstract fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<T?>

    fun getFlow(id: Int): Flow<T?> {
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName WHERE $idName = $id")

        return getDataModelFlowByQuery(query)
    }

    @Insert(onConflict = IGNORE)
    abstract fun insert(obj: T): Long

    @Insert(onConflict = IGNORE)
    abstract fun insert(obj: List<T>): List<Long>

    @Update
    abstract fun update(obj: T)

    @Delete
    abstract fun delete(obj: T)

    @Transaction
    open fun upsert(obj: T): Long {
        val id = insert(obj)
        if (id == -1L) {
            update(obj)
        }
        return if (id != -1L) id else obj.id.toLong()
    }

    fun upsertAll(models: List<T>) {
        models.forEach {
            upsert(it)
        }
    }
}
