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
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ExpensePurpose::class,
            parentColumns = ["purposeId"],
            childColumns = ["purpose"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["purpose"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val expenseId: Int = AUTO_ID,
    val date: LocalDate,
    val amount: Float,
    val purpose: Int,
    val pricePerGal: Float?,
) : DataModel() {
    override val id: Int
        get() = expenseId
}

@Entity(
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class ExpensePurpose(
    @PrimaryKey(autoGenerate = true) val purposeId: Int = AUTO_ID,
    val name: String
): DataModel() {
    override val id: Int
        get() = purposeId
}