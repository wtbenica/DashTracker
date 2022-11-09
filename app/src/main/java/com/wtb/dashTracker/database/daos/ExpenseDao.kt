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
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.database.models.FullExpense
import com.wtb.dashTracker.database.models.FullExpensePurpose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class ExpenseDao : BaseDao<Expense>("Expense", "expenseId") {
    @Query("DELETE FROM Expense")
    abstract override fun clear()

    @RawQuery(observedEntities = [Expense::class])
    abstract override fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<Expense?>

    @Query(SQL_GET_ALL)
    abstract override fun getAll(): Flow<List<Expense>>

    @Query(SQL_GET_ALL)
    abstract suspend fun getAllSuspend(): List<Expense>

    @Transaction
    @Query(SQL_GET_ALL)
    abstract fun getAllPagingSource(): PagingSource<Int, FullExpense>

    @RawQuery(observedEntities = [Expense::class])
    abstract fun executeRawQuery(query: SupportSQLiteQuery): Int

    fun deleteById(id: Int): Int {
        val query = SimpleSQLiteQuery("DELETE FROM Expense WHERE expenseId = $id")

        return executeRawQuery(query)
    }


    companion object {
        private const val SQL_GET_ALL =
            """SELECT * FROM Expense
                ORDER BY date desc"""
    }
}

@ExperimentalCoroutinesApi
@Dao
abstract class ExpensePurposeDao : BaseDao<ExpensePurpose>("ExpensePurpose", "purposeId") {

    @Query("DELETE FROM ExpensePurpose")
    abstract override fun clear()

    @RawQuery(observedEntities = [ExpensePurpose::class])
    abstract override fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<ExpensePurpose?>

    @Query(
        """
        SELECT purposeId
        FROM ExpensePurpose
        WHERE name = :name
    """
    )
    abstract suspend fun getPurposeIdByName(name: String): Int?

    @Query(SQL_GET_ALL)
    abstract override fun getAll(): Flow<List<ExpensePurpose>>

    @Query(SQL_GET_ALL)
    abstract suspend fun getAllSuspend(): List<ExpensePurpose>

    @Transaction
    @Query(SQL_GET_ALL)
    abstract fun getAllFull(): Flow<List<FullExpensePurpose>>

    companion object {
        private const val SQL_GET_ALL = "SELECT * FROM ExpensePurpose ORDER BY name"
    }
}