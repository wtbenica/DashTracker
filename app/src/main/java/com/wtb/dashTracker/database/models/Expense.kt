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
import com.wtb.dashTracker.ui.fragment_base_list.ListItemType
import com.wtb.dashTracker.util.CSVConvertible
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

    companion object : CSVConvertible<Expense> {
        private enum class Columns(val headerName: String) {
            DATE("Date"),
            AMOUNT("Amount"),
            PURPOSE("Purpose"),
            PRICE_PER_GAL("Price Per Gallon")
        }

        override val headerList: List<String>
            get() = Columns.values().map(Columns::headerName)

        override fun fromCSV(row: Map<String, String>): Expense =
            Expense(
                date = LocalDate.parse(row[Columns.DATE.headerName]),
                amount = row[Columns.AMOUNT.headerName]?.toFloatOrNull(),
                purpose = row[Columns.PURPOSE.headerName]?.toInt() ?: Purpose.GAS.id,
                pricePerGal = row[Columns.PRICE_PER_GAL.headerName]?.toFloatOrNull()
            )

        override fun Expense.asList(): List<*> =
            listOf(
                date,
                amount,
                purpose,
                pricePerGal
            )
    }
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

    companion object: CSVConvertible<ExpensePurpose> {
        private enum class Columns(val headerName: String) {
            ID("Purpose Id"),
            NAME("Name")
        }

        override val headerList: List<String>
            get() = Columns.values().map(Columns::headerName)

        override fun fromCSV(row: Map<String, String>): ExpensePurpose =
            ExpensePurpose(
                purposeId = row[Columns.ID.headerName]?.toInt() ?: AUTO_ID,
                name = row[Columns.NAME.headerName]
            )

        override fun ExpensePurpose.asList(): List<*> =
            listOf(
                purposeId,
                name
            )
    }
}

enum class Purpose(val id: Int, val purposeName: String) {
    GAS(1, "Gas"),
    LOAN(2, "Car Payment"),
    INSURANCE(3, "Insurance"),
    OIL(4, "Oil Change")
}

@Entity
data class StandardMileageDeduction(
    @PrimaryKey val year: Int,
    var amount: Float
): DataModel() {
    override val id: Int
        get() = year

    companion object {
        val STANDARD_DEDUCTIONS = mapOf(2021 to 0.56f, 2022 to 0.585f)
    }
}

data class FullExpense(
    @Embedded
    val expense: Expense,

    @Relation(parentColumn = "purpose", entityColumn = "purposeId")
    val purpose: ExpensePurpose
): ListItemType {
    val id: Int
        get() = expense.expenseId
}

data class FullExpensePurpose(
    @Embedded
    val purpose: ExpensePurpose,

    @Relation(parentColumn = "purposeId", entityColumn = "purpose")
    val expenses: List<Expense>
)