package com.wtb.dashTracker.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.DashDatabase
import com.wtb.dashTracker.database.DashEntry
import com.wtb.dashTracker.database.DashEntryDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class Repository private constructor(context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val db = DashDatabase.getInstance(context)
    private val entryDao: DashEntryDao
        get() = db.entryDao()

    fun getEntriesByDate(startDate: LocalDate = LocalDate.MIN, endDate: LocalDate = LocalDate.MAX): Flow<List<DashEntry>> =
        entryDao.getEntriesByDate(startDate, endDate)

    val allEntries: Flow<List<DashEntry>> = entryDao.getAll()

    val allEntriesPaged: Flow<PagingData<DashEntry>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            entryDao.getAllPagingSource()
        }
    ).flow

    fun getEntryFlowById(id: Int) = entryDao.getFlow(id)

    fun upsertEntry(entry: DashEntry) {
        executor.execute {
            entryDao.upsert(entry)
        }
    }

    fun saveEntry(entry: DashEntry) {
        executor.execute {
            entryDao.insert(entry)
        }
    }

    fun deleteEntry(entry: DashEntry) {
        executor.execute {
            entryDao.delete(entry)
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