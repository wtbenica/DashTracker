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
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.wtb.dashTracker.database.DashDatabase
import com.wtb.dashTracker.database.daos.*
import com.wtb.dashTracker.database.models.*
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.util.CSVUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val standardMileageDeductionDao: StandardMileageDeductionDao
        get() = db.standardMileageDeductionDao()

    private val _deductionAmount: MutableStateFlow<Float> = MutableStateFlow(0f)

    val deductionAmount: StateFlow<Float>
        get() = _deductionAmount

    fun setDeductionType(type: DeductionType) {
        val amount = when (type) {
            DeductionType.NONE -> 0f
            DeductionType.GAS_ONLY -> 0f
            DeductionType.ALL_EXPENSES -> 0f
            DeductionType.STD_DEDUCTION -> 0.585f
        }
        _deductionAmount.value = amount
        Log.d("GT_Repository", "setDeductionType: ${type.name} $amount")
    }

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

    fun getWeeklyByDate(date: LocalDate): Flow<CompleteWeekly?> =
        weeklyDao.getWeeklyByDate(date)

    /**
     * Expense
     */
    private suspend fun allExpenses(): List<Expense> = expenseDao.getAllSuspend()

    val allExpensePurposes: Flow<List<ExpensePurpose>> = expensePurposeDao.getAll()

    val allFullPurposes: Flow<List<FullExpensePurpose>> = expensePurposeDao.getAllFull()

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
     * Expense Purpose
     */
    suspend fun getPurposeIdByName(name: String): Int? =
        expensePurposeDao.getPurposeIdByName(name)

    private suspend fun allPurposes(): List<ExpensePurpose> = expensePurposeDao.getAllSuspend()

    fun getExpensePurposeFlowById(id: Int): Flow<ExpensePurpose?> =
        expensePurposeDao.getFlow(id)

    /**
     * Standard Mileage Deductions
     */
    fun getAllStdMileageDeduction(): Flow<List<StandardMileageDeduction>> =
        standardMileageDeductionDao.getAll()

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
            is StandardMileageDeduction -> standardMileageDeductionDao.upsert(model)
        }
    }

    fun saveModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.insert(model)
                is Weekly -> weeklyDao.insert(model)
                is Expense -> expenseDao.insert(model)
                is ExpensePurpose -> expensePurposeDao.insert(model)
                is StandardMileageDeduction -> standardMileageDeductionDao.insert(model)
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
                is StandardMileageDeduction -> standardMileageDeductionDao.delete(model)
            }
        }
    }

    fun export(ctx: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            csvUtil.export(allEntries(), allWeeklies(), allExpenses(), allPurposes(), ctx)
        }
    }

    fun import(ctx: Context) = csvUtil.import(ctx)


    fun importStream(
        entries: List<DashEntry>? = null,
        weeklies: List<Weekly>? = null,
        expenses: List<Expense>? = null,
        purposes: List<ExpensePurpose>? = null
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

            if (expenses != null && purposes != null) {
                expenseDao.clear()
                expensePurposeDao.clear()
                expensePurposeDao.upsertAll(purposes)
                expenseDao.upsertAll(expenses)
            } else if (expenses != null) {
                expenseDao.clear()
                expenseDao.upsertAll(expenses)
            } else if (purposes != null) {
                expensePurposeDao.clear()
                expensePurposeDao.upsertAll(purposes)
            }
        }
    }

    suspend fun getCostPerMile2(date: LocalDate, purpose: DeductionType): Float =
        when (purpose) {
            DeductionType.NONE -> 0f
            DeductionType.GAS_ONLY -> entryDao.getCostPerMile(date, Purpose.GAS)
            DeductionType.ALL_EXPENSES -> entryDao.getCostPerMile(date)
            DeductionType.STD_DEDUCTION -> standardMileageDeductionDao.get(date.year)?.amount ?: 0f
        }


    suspend fun getCostPerMile(date: LocalDate, purpose: Purpose? = null): Float =
        entryDao.getCostPerMile(date, purpose)

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

enum class DeductionType {
    NONE, GAS_ONLY, ALL_EXPENSES, STD_DEDUCTION
}

