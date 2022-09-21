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

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.wtb.dashTracker.database.DashDatabase
import com.wtb.dashTracker.database.daos.*
import com.wtb.dashTracker.database.models.*
import com.wtb.dashTracker.extensions.endOfWeek
import dev.benica.csvutil.CSVUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class Repository private constructor(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val db = DashDatabase.getInstance(context)
    private val csvUtil: CSVUtils
        get() = CSVUtils(context as AppCompatActivity)

    private val entryDao: DashEntryDao
        get() = db.entryDao()

    private val weeklyDao: WeeklyDao
        get() = db.weeklyDao()

    private val expenseDao: ExpenseDao
        get() = db.expenseDao()

    private val expensePurposeDao: ExpensePurposeDao
        get() = db.expensePurposeDao()

    private val transactionDao: TransactionDao
        get() = db.transactionDao()

    private val locationDao: LocationDao
        get() = db.locationDao()

    private val pauseDao: PauseDao
        get() = db.pauseDao()

    private val driveDao: DriveDao
        get() = db.driveDao()

    internal val standardMileageDeductionTable: StandardMileageDeductionTable
        get() = StandardMileageDeductionTable()

    /**
     * Dash Entry
     */
    val allEntries: Flow<List<DashEntry>> = entryDao.getAll()

    private suspend fun allEntries(): List<DashEntry> = entryDao.getAllSuspend()

    private var entryPagingSource: PagingSource<Int, DashEntry>? = null

    private var fullEntryPagingSource: PagingSource<Int, FullEntry>? = null

    val allFullEntriesPaged: Flow<PagingData<FullEntry>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            val ps = entryDao.getAllFullPagingSource()
            fullEntryPagingSource = ps
            ps
        }
    ).flow

    fun deleteEntryById(id: Long) {
        CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Default) {
                entryDao.deleteById(id)
            }.let {
                entryPagingSource?.invalidate()
                fullEntryPagingSource?.invalidate()
            }
        }
    }

    fun getEntryFlowById(id: Long) = entryDao.getFlow(id)

    fun getFullEntryFlowById(id: Long) = entryDao.getFullEntryFlow(id)

    suspend fun getCostPerMile(date: LocalDate, purpose: DeductionType): Float =
        when (purpose) {
            DeductionType.NONE -> 0f
            DeductionType.GAS_ONLY -> transactionDao.getCostPerMileByDate(date, Purpose.GAS)
            DeductionType.ALL_EXPENSES -> transactionDao.getCostPerMileByDate(date)
            DeductionType.IRS_STD -> standardMileageDeductionTable[date]
        }

    fun getCostPerMileFlow(date: LocalDate, purpose: DeductionType): Flow<Float?> =
        when (purpose) {
            DeductionType.NONE -> flow { emit(0f) }
            DeductionType.GAS_ONLY -> transactionDao.getCostPerMileByDateFlow(date, Purpose.GAS)
            DeductionType.ALL_EXPENSES -> transactionDao.getCostPerMileByDateFlow(date)
            DeductionType.IRS_STD -> flow {
                emit(standardMileageDeductionTable[date])
            }
        }


    /**
     * Weekly
     */
    val allWeeklies: Flow<List<FullWeekly>> = weeklyDao.getAllCompleteWeekly()

    private suspend fun allWeeklies(): List<Weekly> = weeklyDao.getAllSuspend()

    val allWeekliesPaged: Flow<PagingData<FullWeekly>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            weeklyDao.getAllPagingSource()
        }
    ).flow

    fun getWeeklyByDate(date: LocalDate): Flow<FullWeekly?> =
        weeklyDao.getWeeklyByDate(date)


    suspend fun getWeeklyByDateSus(date: LocalDate): FullWeekly? =
        weeklyDao.getWeeklyByDateSus(date)

    fun getBasePayAdjustFlowById(id: Long) = weeklyDao.getFlow(id)

    /**
     * Yearly
     */
    suspend fun getAnnualCostPerMile(year: Int, purpose: DeductionType): Map<Int, Float>? =
        when (purpose) {
            DeductionType.NONE -> {
                mapOf(12 to 0f)
            }
            DeductionType.GAS_ONLY -> {
                mapOf(12 to transactionDao.getCostPerMileAnnual(year, Purpose.GAS))
            }
            DeductionType.ALL_EXPENSES -> {
                mapOf(12 to transactionDao.getCostPerMileAnnual(year))
            }
            DeductionType.IRS_STD -> {
                standardMileageDeductionTable[year]
            }
        }

    /**
     * Expense
     */
    private suspend fun allExpenses(): List<Expense> = expenseDao.getAllSuspend()

    private suspend fun allExpensePurposes(): List<ExpensePurpose> =
        expensePurposeDao.getAllSuspend()

    private suspend fun allLocationData(): List<LocationData> = locationDao.getAllSuspend()

    val allExpensePurposes: Flow<List<ExpensePurpose>> = expensePurposeDao.getAll()

    val allFullPurposes: Flow<List<FullExpensePurpose>> = expensePurposeDao.getAllFull()

    fun getExpenseFlowById(id: Long): Flow<Expense?> = expenseDao.getFlow(id)

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

    fun deleteExpenseById(id: Long) {
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

    fun getExpensePurposeFlowById(id: Long): Flow<ExpensePurpose?> =
        expensePurposeDao.getFlow(id)

    /**
     * Pause
     */
    fun getPauseFlowById(id: Long): Flow<Pause?> = pauseDao.getFlow(id)

    /**
     * Drive
     */
    fun getDriveFlowById(id: Long): Flow<Drive?> = driveDao.getFlow(id)

    /**
     * Generic<DataModel> functions
     */
    fun upsertModel(model: DataModel): Long =
        when (model) {
            is DashEntry -> {
                var res: Long = -1L
                db.runInTransaction {
                    weeklyDao.insert(Weekly(model.date.endOfWeek))
                    res = entryDao.upsert(model)
                }
                res
            }
            is Weekly -> weeklyDao.upsert(model)
            is Expense -> expenseDao.upsert(model)
            is ExpensePurpose -> expensePurposeDao.upsert(model)
            is LocationData -> locationDao.upsert(model)
            is Pause -> pauseDao.upsert(model)
            is Drive -> driveDao.upsert(model)
        }

    fun saveModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.insert(model)
                is Weekly -> weeklyDao.insert(model)
                is Expense -> expenseDao.insert(model)
                is ExpensePurpose -> expensePurposeDao.insert(model)
                is LocationData -> locationDao.insert(model)
                is Pause -> pauseDao.insert(model)
                is Drive -> driveDao.insert(model)
            }
        }
    }

    suspend fun saveModelSus(model: DataModel): Long =
        when (model) {
            is DashEntry -> entryDao.insertSus(model)
            is Weekly -> weeklyDao.insertSus(model)
            is Expense -> expenseDao.insertSus(model)
            is ExpensePurpose -> expensePurposeDao.insertSus(model)
            is LocationData -> locationDao.insertSus(model)
            is Pause -> pauseDao.insertSus(model)
            is Drive -> driveDao.insertSus(model)
        }


    fun deleteModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.delete(model)
                is Weekly -> weeklyDao.delete(model)
                is Expense -> expenseDao.delete(model)
                is ExpensePurpose -> expensePurposeDao.delete(model)
                is LocationData -> locationDao.delete(model)
                is Pause -> pauseDao.delete(model)
                is Drive -> driveDao.delete(model)
            }
        }
    }

    fun export() {
        CoroutineScope(Dispatchers.Default).launch {
            csvUtil.export(
                DashEntry.getConvertPackExport(allEntries()),
                Weekly.getConvertPackExport(allWeeklies()),
                Expense.getConvertPackExport(allExpenses()),
                ExpensePurpose.getConvertPackExport(allExpensePurposes()),
                LocationData.getConvertPackExport(allLocationData())
            )
        }
    }

    fun import(activityResultLauncher: ActivityResultLauncher<String>) =
        csvUtil.import(activityResultLauncher)

    /**
     * For only the parameters that are passed arguments, the corresponding database table's 
     * records will be deleted and replaced with the items in the passed list. The default 
     * argument, null, will not make any changes to the corresponding table. An empty list will 
     * delete all records from the table.
     */
    fun insertOrReplace(
        entries: List<DashEntry>? = null,
        weeklies: List<Weekly>? = null,
        expenses: List<Expense>? = null,
        purposes: List<ExpensePurpose>? = null,
        locationData: List<LocationData>? = null
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

            locationData?.let {
                locationDao.clear()
                locationDao.upsertAll(it)
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

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: Repository? = null

        fun initialize(context: Context) {
            INSTANCE = Repository(context)
        }

        fun get(): Repository {
            return INSTANCE ?: throw IllegalStateException("Repository must be initialized")
        }
    }
}

enum class DeductionType(val text: String, val fullDesc: String? = null) {
    NONE("None"),
    GAS_ONLY("Gas", "Gas Cost"),
    ALL_EXPENSES("All", "All Costs"),
    IRS_STD("IRS Rate", "IRS Std.")
}

