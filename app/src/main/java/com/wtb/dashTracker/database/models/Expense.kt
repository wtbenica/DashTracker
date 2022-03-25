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

import androidx.room.*
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
    var date: LocalDate = LocalDate.now(),
    var amount: Float? = null,
    var purpose: Int = Purpose.GAS.id,
    var pricePerGal: Float? = null,
) : DataModel() {
    override val id: Int
        get() = expenseId

    val gallons: Float?
        get() = pricePerGal?.let { amount?.let { a -> a / it } }
}

@Entity(
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class ExpensePurpose(
    @PrimaryKey(autoGenerate = true) val purposeId: Int = AUTO_ID,
    var name: String? = null
) : DataModel() {
    override val id: Int
        get() = purposeId

    override fun toString(): String = name ?: ""
}

enum class Purpose(val id: Int, val purposeName: String) {
    GAS(1, "Gas"),
    LOAN(2, "Car Payment"),
    INSURANCE(3, "Insurance"),
    OIL(4, "Oil Change")
}

data class FullExpense(
    @Embedded
    val expense: Expense,

    @Relation(parentColumn = "purpose", entityColumn = "purposeId")
    val purpose: ExpensePurpose
) {
    val id
        get() = expense.expenseId
}

data class FullExpensePurpose(
    @Embedded
    val purpose: ExpensePurpose,

    @Relation(parentColumn = "purposeId", entityColumn = "purpose")
    val expenses: List<Expense>
)