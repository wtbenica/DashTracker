package com.wtb.dashTracker.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.dashTracker.database.daos.BasePayAdjustmentDao
import com.wtb.dashTracker.database.daos.DashEntryDao
import com.wtb.dashTracker.database.models.BasePayAdjustment
import com.wtb.dashTracker.database.models.DashEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.Executors


@ExperimentalCoroutinesApi
@Database(
    version = 3,
    entities = [DashEntry::class, BasePayAdjustment::class],
    exportSchema = true,
)
@TypeConverters(DbTypeConverters::class)
abstract class DashDatabase : RoomDatabase() {
    abstract fun entryDao(): DashEntryDao
    abstract fun basePayAdjustDao(): BasePayAdjustmentDao

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
                    }
                )
                .build().also {
                    INSTANCE = it
                }
        }
    }
}