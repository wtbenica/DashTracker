package com.wtb.dashTracker.database

import androidx.room.*
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.MainActivity.Companion.APP
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "BaseDao"

@ExperimentalCoroutinesApi
@Dao
abstract class BaseDao(private val tableName: String) {

    @RawQuery
    protected abstract fun getDataModelByQuery(query: SupportSQLiteQuery): DashEntry?

    @RawQuery(observedEntities = [DashEntry::class])
    protected abstract fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<DashEntry?>

    fun get(id: Int): DashEntry? {
        val query = SimpleSQLiteQuery("SELECT * FROM dashentry WHERE entryId = $id")

        return getDataModelByQuery(query)
    }

    fun getFlow(id: Int): Flow<DashEntry?> {
        val query = SimpleSQLiteQuery("SELECT * FROM dashentry WHERE entryId = $id")

        return getDataModelFlowByQuery(query)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: DashEntry): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: List<DashEntry>): List<Long>

    @Update
    abstract fun update(obj: DashEntry)

    @Delete
    abstract fun delete(obj: DashEntry)

    @Transaction
    open fun upsert(obj: DashEntry) {
        val id = insert(obj)
        if (id == -1L) {
            update(obj)
        }
    }

    companion object {
        internal fun <T : DataModel> modelsToSqlIdString(models: Collection<T>) =
            idsToSqlIdString(models.map { it.id })

        private fun idsToSqlIdString(ids: Collection<Int>) =
            ids.toString().replace("[", "(").replace("]", ")")

        internal fun textFilterToString(text: String) =
            "%${text.replace(' ', '%').replace("'", "\'")}%"
    }
}
