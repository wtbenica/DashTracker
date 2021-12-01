package com.wtb.dashTracker.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@ExperimentalCoroutinesApi
@Dao
abstract class DashEntryDao : BaseDao("dashentry") {
    @Query(SQL_GET_ALL)
    abstract fun getAll(): Flow<List<DashEntry>>

    @Query("SELECT * FROM dashentry WHERE date >= :startDate AND date <= :endDate ORDER BY date desc, startTime desc")
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
        private const val SQL_GET_ALL = "SELECT * FROM dashentry ORDER BY date desc, startTime desc"
        private const val SQL_GET_BY_DATE =
            "SELECT * FROM dashentry WHERE date >= :startDate AND date <= :endDate ORDER BY date desc, startTime desc"
    }
}