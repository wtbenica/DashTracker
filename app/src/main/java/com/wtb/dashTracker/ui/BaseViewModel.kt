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

package com.wtb.dashTracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.DataModel
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
abstract class BaseViewModel<T : DataModel> : ViewModel() {
    protected val repository = Repository.get()

    private val _id = MutableStateFlow(AUTO_ID)
    protected val id: StateFlow<Int>
        get() = _id

    internal val item: StateFlow<T?> = id.flatMapLatest { id ->
        val itemFlow = getItemFlowById(id)
        itemFlow
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    abstract fun getItemFlowById(id: Int): Flow<T?>

    fun loadDataModel(id: Int?) {
        _id.value = id ?: AUTO_ID
    }

    fun insert(dataModel: DataModel) = repository.saveModel(dataModel)

    fun upsert(dataModel: DataModel) {
        CoroutineScope(Dispatchers.Default).launch {
            val id = repository.upsertModel(dataModel)
            if (id != -1L) {
                _id.value = id.toInt()
            }
        }
    }

    suspend fun upsertAsync(dataModel: DataModel): Long = CoroutineScope(Dispatchers.Default).async {
        val id = repository.upsertModel(dataModel)
        if (id != -1L) {
            _id.value = id.toInt()
        }
        return@async id
    }.await()

    fun delete(dataModel: DataModel) = repository.deleteModel(dataModel)

    fun clearEntry() {
        _id.value = AUTO_ID
    }

    companion object {
        private const val TAG = APP + "BaseViewModel"
    }
}