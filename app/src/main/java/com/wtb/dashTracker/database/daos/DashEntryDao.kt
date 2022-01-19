package com.wtb.dashTracker.database.daos

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Weekly
import com.wtb.dashTracker.extensions.endOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@ExperimentalCoroutinesApi
@Dao
abstract class DashEntryDao : BaseDao<DashEntry>("DashEntry", "entryId") {
    @Query(SQL_GET_ALL)
    abstract fun getAll(): Flow<List<DashEntry>>

    @Query(SQL_GET_ALL)
    abstract suspend fun getAllLiveData(): List<DashEntry>

    @Query("DELETE FROM DashEntry")
    abstract override fun clear()

    @RawQuery(observedEntities = [DashEntry::class])
    abstract override fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<DashEntry?>

    @RawQuery(observedEntities = [DashEntry::class])
    abstract fun getEntriesByQuery(query: SupportSQLiteQuery): Flow<List<DashEntry>>

    @RawQuery(observedEntities = [DashEntry::class])
    abstract fun executeRawQuery(query: SupportSQLiteQuery): Int

    fun deleteById(id: Int): Int {
        val query = SimpleSQLiteQuery("DELETE FROM DashEntry WHERE entryId = $id")

        return executeRawQuery(query)
    }

    @Query("SELECT * FROM DashEntry WHERE date >= :startDate AND date <= :endDate ORDER BY date desc, startTime desc")
    abstract fun getEntriesByDate(
        startDate: LocalDate = LocalDate.MIN,
        endDate: LocalDate = LocalDate.MAX
    ): Flow<List<DashEntry>>

    @Query(SQL_GET_ALL)
    abstract fun getAllPagingSource(): PagingSource<Int, DashEntry>

    @Query(SQL_GET_BY_DATE)
    abstract fun getEntriesByDatePagingSource(
        startDate: LocalDate = LocalDate.MIN,
        endDate: LocalDate = LocalDate.MAX
    ): PagingSource<Int, DashEntry>

    companion object {
        private const val SQL_GET_ALL =
            """SELECT * FROM DashEntry 
            ORDER BY date desc, startTime desc"""

        private const val SQL_GET_BY_DATE =
            """SELECT * FROM DashEntry 
            WHERE date >= :startDate 
            AND date <= :endDate 
            ORDER BY date desc, startTime desc"""
    }
}