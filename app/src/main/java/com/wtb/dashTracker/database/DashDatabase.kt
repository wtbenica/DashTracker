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
    version = 8,
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
                                        date TEXT PRIMARY KEY NOT NULL,
                                        basePayAdjustment REAL,
                                        weekNumber INTEGER NOT NULL,
                                        lastUpdated TEXT NOT NULL
                                    )"""
                            )
                        }
                    },
                    object : Migration(5, 6) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            database.execSQL(
                                """CREATE TABLE DashEntry2(
                                    entryId INTEGER PRIMARY KEY NOT NULL,
                                    date TEXT NOT NULL,
                                    endDate TEXT NOT NULL,
                                    startTime TEXT,
                                    endTime TEXT,
                                    startOdometer REAL,
                                    endOdometer REAL,
                                    totalMileage REAL,
                                    pay REAL,
                                    otherPay REAL,
                                    cashTips REAL,
                                    numDeliveries INTEGER,
                                    lastUpdated TEXT NOT NULL,
                                    week TEXT REFERENCES Weekly(date) ON DELETE SET NULL
                                    )"""
                            )
                            database.execSQL(
                                """INSERT INTO DashEntry2(entryId, date, endDate, startTime, endTime, startOdometer, endOdometer, totalMileage, pay, otherPay, cashTips, numDeliveries, lastUpdated)
                                SELECT de.entryId, de.date, de.endDate, de.startTime, de.endTime, de.startOdometer, de.endOdometer, de.totalMileage, de.pay, de.otherPay, de.cashTips, de.numDeliveries, de.lastUpdated
                                FROM DashEntry de
                                """
                            )
                            database.execSQL(
                                """DROP TABLE DashEntry"""
                            )
                            database.execSQL(
                                """ALTER TABLE DashEntry2 RENAME TO DashEntry"""
                            )
                        }
                    },
                    object : Migration(6, 7) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            database.execSQL(
                                """ALTER TABLE Weekly
                                ADD COLUMN isNew INTEGER NOT NULL DEFAULT 0
                                """
                            )
                        }
                    },
                    object : Migration(7, 8) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            database.execSQL(
                                """CREATE INDEX index_DashEntry_date
                                ON DashEntry('week')
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