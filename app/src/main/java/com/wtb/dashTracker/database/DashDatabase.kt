package com.wtb.dashTracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wtb.dashTracker.database.daos.DashEntryDao
import com.wtb.dashTracker.database.daos.WeeklyDao
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Weekly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.Executors


@ExperimentalCoroutinesApi
@Database(
    version = 1,
    entities = [DashEntry::class, Weekly::class],
    exportSchema = true,
)
@TypeConverters(DbTypeConverters::class)
abstract class DashDatabase : RoomDatabase() {
    abstract fun entryDao(): DashEntryDao
    abstract fun weeklyDao(): WeeklyDao

    companion object {
        @Volatile
        private var INSTANCE: DashDatabase? = null
        private const val DATABASE_NAME = "dash-database"

        fun getInstance(context: Context): DashDatabase {
            return INSTANCE ?: synchronized(this) {
                Executors.newSingleThreadExecutor()

                return Room.databaseBuilder(
                    context.applicationContext,
                    DashDatabase::class.java,
                    DATABASE_NAME
                )
                    .build().also {
                        INSTANCE = it
                    }
            }
        }
    }
}