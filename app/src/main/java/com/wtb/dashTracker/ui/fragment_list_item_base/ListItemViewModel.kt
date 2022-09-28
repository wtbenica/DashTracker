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

package com.wtb.dashTracker.ui.fragment_list_item_base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DataModel
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
abstract class ListItemViewModel<T : DataModel> : ViewModel() {
    protected val repository = Repository.get()

    private val _id = MutableStateFlow(AUTO_ID)
    protected val id: StateFlow<Long>
        get() = _id

    internal open val item: StateFlow<T?> = id.flatMapLatest { id ->
        val itemFlow = getItemFlowById(id)
        itemFlow
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    abstract fun getItemFlowById(id: Long): Flow<T?>

    fun loadDataModel(id: Long?) {
        _id.value = id ?: AUTO_ID
    }

    fun insert(dataModel: DataModel) = repository.saveModel(dataModel)

    suspend fun insertSus(dataModel: DataModel): Long = repository.saveModelSus(dataModel)

    fun upsert(dataModel: DataModel) {
        CoroutineScope(Dispatchers.Default).launch {
            repository.upsertModel(dataModel)
        }
    }

    fun upsert(dataModel: T, loadItem: Boolean = true) {
        CoroutineScope(Dispatchers.Default).launch {
            val id = repository.upsertModel(dataModel)
            if (id != -1L && loadItem) {
                _id.value = id
            }
        }
    }

    suspend fun upsertAsync(dataModel: DataModel, loadItem: Boolean = true): Long =
        withContext(Dispatchers.Default) {
            val id = repository.upsertModel(dataModel)
            if (id != -1L && loadItem) {
                _id.value = id
            }
            id
        }

    fun delete(dataModel: DataModel) = repository.deleteModel(dataModel)

    fun clearEntry() {
        _id.value = AUTO_ID
    }

    companion object
}