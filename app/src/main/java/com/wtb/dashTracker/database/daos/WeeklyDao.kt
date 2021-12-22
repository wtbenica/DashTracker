package com.wtb.dashTracker.database.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.database.models.CompleteWeekly
import com.wtb.dashTracker.database.models.Weekly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@ExperimentalCoroutinesApi
@Dao
abstract class WeeklyDao : BaseDao<Weekly>("weekly", "date") {
    @Query(SQL_GET_ALL)
    abstract fun getAll(): Flow<List<CompleteWeekly>>

    @Query(SQL_GET_ALL)
    abstract fun getAllPagingSource(): PagingSource<Int, Weekly>

    @RawQuery(observedEntities = [Weekly::class])
    abstract override fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<Weekly?>

    @Query("SELECT * FROM Weekly WHERE date = :date LIMIT 1")
    abstract fun getWeeklyByDate(date: LocalDate): Flow<Weekly?>

    companion object {
        private const val SQL_GET_ALL = "SELECT * FROM Weekly ORDER BY date DESC"
    }
}