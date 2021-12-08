package com.wtb.dashTracker.database.daos

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.models.BasePayAdjustment
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.DataModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

private const val TAG = APP + "BaseDao"

@ExperimentalCoroutinesApi
@Dao
abstract class BaseDao<T : DataModel>(private val tableName: String) {

    @RawQuery
    protected abstract fun getDataModelByQuery(query: SupportSQLiteQuery): T?

    fun get(id: Int): T? {
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName WHERE ${tableName + "ID"} = $id")

        return getDataModelByQuery(query)
    }

    protected abstract fun getDataModelFlowByQuery(query: SupportSQLiteQuery): Flow<T?>

    fun getFlow(id: Int): Flow<T?> {
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName WHERE entryId = $id")

        return getDataModelFlowByQuery(query)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(obj: List<T>): List<Long>

    @Update
    abstract fun update(obj: T)

    @Delete
    abstract fun delete(obj: T)

    @Transaction
    open fun upsert(obj: T) {
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
