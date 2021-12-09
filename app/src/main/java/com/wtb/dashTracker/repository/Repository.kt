package com.wtb.dashTracker.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.DashDatabase
import com.wtb.dashTracker.database.daos.BasePayAdjustmentDao
import com.wtb.dashTracker.database.daos.DashEntryDao
import com.wtb.dashTracker.database.models.BasePayAdjustment
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.DataModel
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

    private val basePayAdjustDao: BasePayAdjustmentDao
        get() = db.basePayAdjustDao()

    fun getEntriesByDate(
        startDate: LocalDate = LocalDate.MIN,
        endDate: LocalDate = LocalDate.MAX
    ): Flow<List<DashEntry>> =
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
    fun getBasePayAdjustFlowById(id: Int) = basePayAdjustDao.getFlow(id)

    fun upsertModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.upsert(model)
                is BasePayAdjustment -> basePayAdjustDao.upsert(model)
            }
        }
    }

    fun saveModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.insert(model)
                is BasePayAdjustment -> basePayAdjustDao.insert(model)
            }
        }
    }

    fun deleteModel(model: DataModel) {
        executor.execute {
            when (model) {
                is DashEntry -> entryDao.delete(model)
                is BasePayAdjustment -> basePayAdjustDao.delete(model)
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