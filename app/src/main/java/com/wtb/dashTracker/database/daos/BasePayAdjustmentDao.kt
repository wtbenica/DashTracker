package com.wtb.dashTracker.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.database.models.BasePayAdjustment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Dao
abstract class BasePayAdjustmentDao: BaseDao<BasePayAdjustment>("basepayadjustment") {
    @Query(SQL_GET_ALL)
    abstract fun getAll(): Flow<List<BasePayAdjustment>>

    companion object {
        private const val SQL_GET_ALL = "SELECT * FROM BasePayAdjustment"
    }
}