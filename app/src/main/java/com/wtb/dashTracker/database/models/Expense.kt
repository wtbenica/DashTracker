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

package com.wtb.dashTracker.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

sealed class Expense : DataModel() {
    abstract val date: LocalDate
    abstract val amount: Float
}

@Entity
data class GasExpense(
    @PrimaryKey(autoGenerate = true) val gasId: Int = AUTO_ID,
    override val date: LocalDate,
    override val amount: Float,
    val pricePerGal: Float,
) : Expense() {
    override val id: Int
        get() = gasId
}

@Entity
data class MaintenanceExpense(
    @PrimaryKey(autoGenerate = true) val maintenanceId: Int = AUTO_ID,
    override val date: LocalDate,
    override val amount: Float,
    val purpose: String
) : Expense() {
    override val id: Int
        get() = maintenanceId
}