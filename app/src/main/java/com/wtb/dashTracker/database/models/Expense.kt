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
import com.wtb.dashTracker.ui.fragment_list_item_base.ListItemType
import dev.benica.csvutil.CSVConvertible
import dev.benica.csvutil.CSVConvertible.Column
import java.time.LocalDate
import kotlin.reflect.KProperty1

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
    @PrimaryKey(autoGenerate = true) val expenseId: Long = AUTO_ID,
    var date: LocalDate = LocalDate.now(),
    var amount: Float? = null,
    var purpose: Long = Purpose.GAS.id,
    var pricePerGal: Float? = null,
    @ColumnInfo(defaultValue = "0")
    var isNew: Boolean = false
) : DataModel() {
    override val id: Long
        get() = expenseId

    val gallons: Float?
        get() = pricePerGal?.let { amount?.let { a -> a / it } }

    companion object : CSVConvertible<Expense> {
        private enum class Columns(val headerName: String, val getValue: KProperty1<Expense, *>) {
            DATE("Date", Expense::date),
            AMOUNT("Amount", Expense::amount),
            PURPOSE("Purpose", Expense::purpose),
            PRICE_PER_GAL("Price Per Gallon", Expense::pricePerGal),
            IS_NEW("isNew", Expense::isNew)
        }

        override val saveFileName: String
            get() = "expenses"

        override fun getColumns(): Array<Column<Expense>> =
            Columns.values().map { Column(it.headerName, it.getValue) }.toTypedArray()

        override fun fromCSV(row: Map<String, String>): Expense =
            Expense(
                date = LocalDate.parse(row[Columns.DATE.headerName]),
                amount = row[Columns.AMOUNT.headerName]?.toFloatOrNull(),
                purpose = row[Columns.PURPOSE.headerName]?.toLong() ?: Purpose.GAS.id,
                pricePerGal = row[Columns.PRICE_PER_GAL.headerName]?.toFloatOrNull(),
                isNew = row[Columns.IS_NEW.headerName]?.toBoolean() ?: false
            )
    }
}

@Entity(
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class ExpensePurpose(
    @PrimaryKey(autoGenerate = true) val purposeId: Long = AUTO_ID,
    var name: String? = null
) : DataModel() {
    override val id: Long
        get() = purposeId

    override fun toString(): String = name ?: ""

    companion object : CSVConvertible<ExpensePurpose> {
        override val saveFileName: String
            get() = "expense_purposes"

        private enum class Columns(
            val headerName: String,
            val getValue: KProperty1<ExpensePurpose, *>
        ) {
            ID("Purpose Id", ExpensePurpose::purposeId),
            NAME("Name", ExpensePurpose::name)
        }

        override fun fromCSV(row: Map<String, String>): ExpensePurpose =
            ExpensePurpose(
                purposeId = row[Columns.ID.headerName]?.toLong() ?: AUTO_ID,
                name = row[Columns.NAME.headerName]
            )

        override fun getColumns(): Array<Column<ExpensePurpose>> =
            Columns.values().map { Column(it.headerName, it.getValue) }.toTypedArray()
    }
}

enum class Purpose(val id: Long, val purposeName: String) {
    GAS(1, "Gas"),
    LOAN(2, "Car Payment"),
    INSURANCE(3, "Insurance"),
    OIL(4, "Oil Change")
}

@Entity
data class StandardMileageDeduction(
    @PrimaryKey val year: Long,
    var amount: Float
) : DataModel() {
    override val id: Long
        get() = year

    companion object {
        val STANDARD_DEDUCTIONS: Map<Long, Float> = mapOf(2021L to 0.56f, 2022L to 0.585f)
    }
}

data class FullExpense(
    @Embedded
    val expense: Expense,

    @Relation(parentColumn = "purpose", entityColumn = "purposeId")
    val purpose: ExpensePurpose
) : ListItemType {
    val id: Long
        get() = expense.expenseId

    val isEmpty: Boolean
        get() {
            val amountIsEmpty = expense.amount == null || expense.amount == 0f
            val isGasExpense = purpose.purposeId == Purpose.GAS.id
            val priceIsEmpty = expense.pricePerGal == null || expense.pricePerGal == 0f
            return amountIsEmpty && (!isGasExpense || priceIsEmpty) && !expense.isNew
        }
}

data class FullExpensePurpose(
    @Embedded
    val purpose: ExpensePurpose,

    @Relation(parentColumn = "purposeId", entityColumn = "purpose")
    val expenses: List<Expense>
)