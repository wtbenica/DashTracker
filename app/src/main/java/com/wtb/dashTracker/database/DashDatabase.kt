/*
 * Copyright 2022 Wesley T. Benica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.dashTracker.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.dashTracker.database.DashDatabase.Companion.autoMigrateSpec_10_11
import com.wtb.dashTracker.database.DashDatabase.Companion.autoMigrateSpec_7_8
import com.wtb.dashTracker.database.daos.*
import com.wtb.dashTracker.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.Executors


@ExperimentalCoroutinesApi
@Database(
    version = 11,
    entities = [DashEntry::class, Weekly::class, Expense::class, ExpensePurpose::class,
        LocationData::class, Drive::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8, spec = autoMigrateSpec_7_8::class),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11, spec = autoMigrateSpec_10_11::class),
    ],
    exportSchema = true,
)


@TypeConverters(DbTypeConverters::class)
abstract class DashDatabase : RoomDatabase() {
    abstract fun entryDao(): DashEntryDao
    abstract fun weeklyDao(): WeeklyDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expensePurposeDao(): ExpensePurposeDao
    abstract fun transactionDao(): TransactionDao
    abstract fun locationDao(): LocationDao
    abstract fun driveDao(): DriveDao

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
                    .addMigrations(MIGRATION_4_5)
                    .addCallback(
                        object : Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)

                                val purposes = Purpose.values().map { purpose ->
                                    ExpensePurpose(purpose.id, purpose.purposeName)
                                }

                                Executors.newSingleThreadScheduledExecutor().execute {
                                    getInstance(context).apply {
                                        expensePurposeDao().upsertAll(purposes)
                                    }

                                }
                            }
                        }
                    )
                    .build().also {
                        INSTANCE = it
                    }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val purposes = Purpose.values().map { purpose ->
                    ExpensePurpose(purpose.id, purpose.purposeName)
                }

                Executors.newSingleThreadScheduledExecutor().execute {
                    INSTANCE?.apply {
                        expensePurposeDao().upsertAll(purposes)
                    }
                }
            }

        }

        @DeleteTable.Entries(DeleteTable(tableName = "StandardMileageDeduction"))
        class autoMigrateSpec_7_8 : AutoMigrationSpec

        @DeleteTable.Entries(DeleteTable(tableName = "Pause"))
        class autoMigrateSpec_10_11 : AutoMigrationSpec

//
//        @DeleteColumn(
//            tableName = "GasExpense",
//            columnName = "numGallons"
//        )
//        class AutoMigration_2_3 : AutoMigrationSpec
    }
}

