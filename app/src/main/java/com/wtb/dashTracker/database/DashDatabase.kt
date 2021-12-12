package com.wtb.dashTracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.dashTracker.database.daos.DashEntryDao
import com.wtb.dashTracker.database.daos.WeeklyDao
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.Weekly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.Executors


@ExperimentalCoroutinesApi
@Database(
    version = 5,
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


        fun getInstance(context: Context): DashDatabase = INSTANCE ?: synchronized(this) {
            Executors.newSingleThreadExecutor()
            return Room.databaseBuilder(
                context.applicationContext,
                DashDatabase::class.java,
                DATABASE_NAME
            )
                .createFromAsset(DATABASE_NAME)
                .addMigrations(
                    object : Migration(1, 2) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            database.execSQL(
                                """ALTER TABLE DashEntry
                                    ADD COLUMN otherPay REAL
                                """
                            )
                        }
                    },
                    object : Migration(2, 3) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            database.execSQL(
                                """CREATE TABLE BasePayAdjustment(
                                        adjustmentId INTEGER PRIMARY KEY NOT NULL,
                                        date TEXT NOT NULL,
                                        amount REAL NOT NULL,
                                        lastUpdated TEXT NOT NULL
                                    )"""
                            )
                        }
                    },
                    object : Migration(3, 4) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            database.execSQL(
                                """DROP TABLE BasePayAdjustment
                                """
                            )
                            database.execSQL(
                                """CREATE TABLE Weekly(
                                        weeklyId INTEGER PRIMARY KEY NOT NULL,
                                        date TEXT NOT NULL,
                                        basePayAdjustment REAL,
                                        weekNumber INTEGER NOT NULL,
                                        lastUpdated TEXT NOT NULL
                                    )"""
                            )
                        }
                    },
                    object : Migration(4, 5) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            database.execSQL("""DROP TABLE WEEKLY""")
                            database.execSQL(
                                """CREATE TABLE Weekly(
                                        date TEXT NOT NULL PRIMARY KEY NOT NULL,
                                        basePayAdjustment REAL,
                                        weekNumber INTEGER NOT NULL,
                                        lastUpdated TEXT NOT NULL
                                    )"""
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