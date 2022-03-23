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

package com.wtb.dashTracker.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.wtb.dashTracker.database.DashDatabase
import com.wtb.dashTracker.database.daos.DashEntryDao
import com.wtb.dashTracker.database.daos.ExpenseDao
import com.wtb.dashTracker.database.daos.ExpensePurposeDao
import com.wtb.dashTracker.database.daos.WeeklyDao
import com.wtb.dashTracker.database.models.*
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.util.CSVUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class Repository private constructor(context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val db = DashDatabase.getInstance(context)
    private val csvUtil: CSVUtils
        get() = CSVUtils()

    private val entryDao: DashEntryDao
        get() = db.entryDao()

    private val weeklyDao: WeeklyDao
        get() = db.weeklyDao()

    private val expenseDao: ExpenseDao
        get() = db.expenseDao()

    private val expensePurposeDao: ExpensePurposeDao
        get() = db.expensePurposeDao()

    /**
     * Dash Entry
     */
    val allEntries: Flow<List<DashEntry>> = entryDao.getAll()

    private suspend fun allEntries(): List<DashEntry> = entryDao.getAllSuspend()

    private var entryPagingSource: PagingSource<Int, DashEntry>? = null

    val allEntriesPaged: Flow<PagingData<DashEntry>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            val ps = entryDao.getAllPagingSource()
            entryPagingSource = ps
            ps
        }
    ).flow

    fun deleteEntryById(id: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Default) {
                entryDao.deleteById(id)
            }.let {
                entryPagingSource?.invalidate()
            }
        }
    }

    fun getEntryFlowById(id: Int) = entryDao.getFlow(id)

    fun getBasePayAdjustFlowById(id: Int) = weeklyDao.getFlow(id)

    /**
     * Weekly
     */
    val allWeeklies: Flow<List<CompleteWeekly>> = weeklyDao.getAllCompleteWeekly()

    private suspend fun allWeeklies(): List<Weekly> = weeklyDao.getAllSuspend()

    val allWeekliesPaged: Flow<PagingData<CompleteWeekly>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            weeklyDao.getAllPagingSource()
        }
    ).flow

    fun getWeeklyByDate(date: LocalDate): Flow<CompleteWeekly?> = weeklyDao.getWeeklyByDate(date)

    /**
     * Expense
     */
    val allExpensePurposes: Flow<List<ExpensePurpose>> = expensePurposeDao.getAll()

    fun getExpenseFlowById(id: Int): Flow<Expense?> = expenseDao.getFlow(id)

    private var expensePagingSource: PagingSource<Int, FullExpense>? = null

    val allExpensesPaged: Flow<PagingData<FullExpense>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            val ps = expenseDao.getAllPagingSource()
            expensePagingSource = ps
            ps
        }
    ).flow

    fun deleteExpenseById(id: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
                expenseDao.deleteById(id)
            }.let {
                expensePagingSource?.invalidate()
            }
        }
    }

    /**
     * Generic<DataModel> functions
     */
    fun upsertModel(model: DataModel): Long {
        return when (model) {
            is DashEntry -> {
                var res: Long = -1L
                db.runInTransaction {
                    weeklyDao.insert(Weekly(model.date.endOfWeek))
                    res = entryDao.upsert(model)
                }
                return res
            }
            is Weekly -> weeklyDao.upsert(model)
            is Expense -> expenseDao.upsert(model)
            is ExpensePurpose -> expensePurposeDao.upsert(model)
        }
    }

    fun saveModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.insert(model)
                is Weekly -> weeklyDao.insert(model)
                is Expense -> expenseDao.insert(model)
            }
        }
    }

    fun deleteModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.delete(model)
                is Weekly -> weeklyDao.delete(model)
                is Expense -> expenseDao.delete(model)
                is ExpensePurpose -> expensePurposeDao.delete(model)
            }
        }
    }

    fun export(ctx: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            csvUtil.export(allEntries(), allWeeklies(), ctx)
        }
    }

    fun import(ctx: Context) = csvUtil.import(ctx)


    fun importStream(
        entries: List<DashEntry>? = null,
        weeklies: List<Weekly>? = null
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            weeklies?.let {
                weeklyDao.clear()
                weeklyDao.upsertAll(it)
            }
            entries?.let {
                entryDao.clear()
                entryDao.upsertAll(it)
            }
        }
    }

    suspend fun getPurposeIdByName(name: String): Int? = db.expensePurposeDao().getPurposeIdByName(name)

    companion object {
        private var INSTANCE: Repository? = null

        fun initialize(context: Context) {
            INSTANCE = Repository(context)
        }

        fun get(): Repository {
            return INSTANCE ?: throw IllegalStateException("Repository must be initialized")
        }
    }
}

