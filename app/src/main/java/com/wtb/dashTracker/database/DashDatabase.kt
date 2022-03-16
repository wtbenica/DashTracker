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
import com.wtb.dashTracker.database.daos.DashEntryDao
import com.wtb.dashTracker.database.daos.GasExpenseDao
import com.wtb.dashTracker.database.daos.MaintenanceExpenseDao
import com.wtb.dashTracker.database.daos.WeeklyDao
import com.wtb.dashTracker.database.models.DashEntry
import com.wtb.dashTracker.database.models.GasExpense
import com.wtb.dashTracker.database.models.MaintenanceExpense
import com.wtb.dashTracker.database.models.Weekly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.Executors


@ExperimentalCoroutinesApi
@Database(
    version = 3,
    entities = [DashEntry::class, Weekly::class, GasExpense::class, MaintenanceExpense::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = DashDatabase.Companion.AutoMigration_2_3::class),
    ],
    exportSchema = true,
)
@TypeConverters(DbTypeConverters::class)
abstract class DashDatabase : RoomDatabase() {
    abstract fun entryDao(): DashEntryDao
    abstract fun weeklyDao(): WeeklyDao
    abstract fun gasExpenseDao(): GasExpenseDao
    abstract fun maintenanceExpenseDao(): MaintenanceExpenseDao

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
                    .build().also {
                        INSTANCE = it
                    }
            }
        }

        @DeleteColumn(
            tableName = "GasExpense",
            columnName = "numGallons"
        )
        class AutoMigration_2_3 : AutoMigrationSpec

    }
}

