package com.wtb.dashTracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.Serializable
import java.time.LocalDate
import java.util.concurrent.Executors


@ExperimentalCoroutinesApi
@Database(
    entities = [DashEntry::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DbTypeConverters::class)
abstract class DashDatabase : RoomDatabase() {
    abstract fun entryDao(): DashEntryDao

    companion object {
        @Volatile
        private var INSTANCE: DashDatabase? = null

        fun getInstance(context: Context): DashDatabase = INSTANCE ?: synchronized(this) {
            val executor = Executors.newSingleThreadExecutor()
            return Room.databaseBuilder(
                context.applicationContext,
                DashDatabase::class.java,
                "dash-database"
            )
                .addMigrations(
                    object : Migration(1, 2) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            database.execSQL(
                                """ALTER TABLE dashentry
                                    ADD COLUMN otherPay REAL
                                """
                            )
                        }
                    }
                )
                .build().also {
                    INSTANCE = it
                }
        }
    }
}

sealed class DataModel(var lastUpdated: LocalDate = LocalDate.now()) : Serializable {

    abstract val id: Int
}