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
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wtb.dashTracker.database.daos.DashEntryDao
import com.wtb.dashTracker.database.daos.ExpenseDao
import com.wtb.dashTracker.database.daos.ExpensePurposeDao
import com.wtb.dashTracker.database.daos.WeeklyDao
import com.wtb.dashTracker.database.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.Executors


@ExperimentalCoroutinesApi
@Database(
    version = 2,
    entities = [DashEntry::class, Weekly::class, Expense::class, ExpensePurpose::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
    exportSchema = true,
)
@TypeConverters(DbTypeConverters::class)
abstract class DashDatabase : RoomDatabase() {
    abstract fun entryDao(): DashEntryDao
    abstract fun weeklyDao(): WeeklyDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expensePurposeDao(): ExpensePurposeDao

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
                    .addMigrations()
                    .addCallback(
                        object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)

                                val purposes = Purpose.values().map { purpose ->
                                    ExpensePurpose(purpose.id, purpose.purposeName)
                                }

                                Executors.newSingleThreadScheduledExecutor().execute {
                                    getInstance(context).expensePurposeDao().upsertAll(purposes)
                                }
                            }
                        }
                    )
                    .build().also {
                        INSTANCE = it
                    }
            }
        }
//
//        @DeleteColumn(
//            tableName = "GasExpense",
//            columnName = "numGallons"
//        )
//        class AutoMigration_2_3 : AutoMigrationSpec
    }
}

