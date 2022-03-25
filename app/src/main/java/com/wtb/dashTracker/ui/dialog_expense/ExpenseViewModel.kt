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

package com.wtb.dashTracker.ui.dialog_expense

import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.database.models.ExpensePurpose
import com.wtb.dashTracker.database.models.FullExpensePurpose
import com.wtb.dashTracker.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class ExpenseViewModel : BaseViewModel<Expense>() {
    override fun getItemFlowById(id: Int): Flow<Expense?> =
        repository.getExpenseFlowById(id)

    suspend fun getPurposeIdByName(name: String) = repository.getPurposeIdByName(name)

    val expensePurposes: Flow<List<ExpensePurpose>> = repository.allExpensePurposes

    val fullPurposes: Flow<List<FullExpensePurpose>> = repository.allFullPurposes
}

