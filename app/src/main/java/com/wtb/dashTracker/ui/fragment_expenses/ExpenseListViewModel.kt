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

package com.wtb.dashTracker.ui.fragment_expenses

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.wtb.dashTracker.database.models.Expense
import com.wtb.dashTracker.database.models.FullExpense
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
class ExpenseListViewModel : ViewModel() {
    private val repository: Repository = Repository.get()

    val expenseList: Flow<PagingData<FullExpense>> = repository.allExpensesPaged

    fun delete(expense: Expense) = repository.deleteModel(expense)

    fun deleteExpenseById(id: Long) = repository.deleteExpenseById(id)
}