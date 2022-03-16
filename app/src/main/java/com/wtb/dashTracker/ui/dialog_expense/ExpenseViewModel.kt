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

import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.GasExpense
import com.wtb.dashTracker.database.models.MaintenanceExpense
import com.wtb.dashTracker.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class ExpenseViewModel : BaseViewModel<GasExpense>() {
    override fun getItemFlowById(id: Int): Flow<GasExpense?> =
        repository.getGasExpenseFlowById(id)

    fun getMaintExpenseFlowById(id: Int): Flow<MaintenanceExpense?> =
        repository.getMaintenanceExpenseFlowById(id)

    private val _maintId = MutableStateFlow(AUTO_ID)
    protected val maintId: StateFlow<Int>
        get() = _maintId

    internal val maintItem: StateFlow<MaintenanceExpense?> = id.flatMapLatest { id ->
        val itemFlow = getMaintExpenseFlowById(id)
        itemFlow
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun loadMaintExpense(id: Int?) {
        _maintId.value = id ?: AUTO_ID
    }
}

