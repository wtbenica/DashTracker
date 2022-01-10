package com.wtb.dashTracker.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.DashDatabase
import com.wtb.dashTracker.database.daos.DashEntryDao
import com.wtb.dashTracker.database.daos.WeeklyDao
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.DataModel
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.extensions.endOfWeek
import com.wtb.dashTracker.util.CSVUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.time.LocalDate
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class Repository private constructor(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val db = DashDatabase.getInstance(context)

    private val entryDao: DashEntryDao
        get() = db.entryDao()

    private val weeklyDao: WeeklyDao
        get() = db.weeklyDao()

    /**
     * Dash Entry
     */
    val allEntries: Flow<List<DashEntry>> = entryDao.getAll()

    private suspend fun allEntriesLiveData(): List<DashEntry> = entryDao.getAllLiveData()
    private suspend fun allWeekliesLiveData(): List<Weekly> = weeklyDao.getAllLiveData()

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
            withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
                entryDao.deleteById(id)
            }.let {
                entryPagingSource?.invalidate()
            }
//            executor.execute {
//                entryDao.deleteById(id)
//                entryPagingSource?.invalidate()
//            }
        }
    }

    fun getEntryFlowById(id: Int) = entryDao.getFlow(id)

    fun getBasePayAdjustFlowById(id: Int) = weeklyDao.getFlow(id)

    /**
     * Weekly
     */
    val allWeeklies: Flow<List<CompleteWeekly>> = weeklyDao.getAll()

    val allWeekliesPaged: Flow<PagingData<CompleteWeekly>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            weeklyDao.getAllPagingSource()
        }
    ).flow

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
        }
    }

    /**
     * Weekly
     */
    fun getWeeklyByDate(date: LocalDate): Flow<CompleteWeekly?> = weeklyDao.getWeeklyByDate(date)

    fun saveModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.insert(model)
                is Weekly -> weeklyDao.insert(model)
            }
        }
    }

    fun deleteModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.delete(model)
                is Weekly -> weeklyDao.delete(model)
            }
        }
    }

    fun export() {
        CoroutineScope(Dispatchers.Default).launch {
            CSVUtils().exportDb(context, allEntriesLiveData(), allWeekliesLiveData())
        }
    }

    fun import(entriesPath: InputStream? = null, weekliesPath: InputStream? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            val res = CSVUtils().importCsv(entriesPath = entriesPath, weekliesPath = weekliesPath)
            res.first?.let {
                entryDao.clear()
                entryDao.upsertAll(it)
            }
            res.second?.let {
                weeklyDao.clear()
                weeklyDao.upsertAll(it)
            }
        }
    }

    companion object {
        private const val TAG = APP + "Repository"

        @Suppress("StaticFieldLeak")
        private var INSTANCE: Repository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = Repository(context)
            }
        }

        fun get(): Repository {
            return INSTANCE ?: throw IllegalStateException("Repository must be initialized")
        }
    }
}

