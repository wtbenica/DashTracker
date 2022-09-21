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

package com.wtb.dashTracker.ui.dialog_confirm.mileage_breakdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wtb.dashTracker.database.models.AUTO_ID
import com.wtb.dashTracker.database.models.FullEntry
import com.wtb.dashTracker.repository.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class ConfirmationDialogMileageStuffViewModel : ViewModel() {
    protected val repository = Repository.get()

    private val _entryId = MutableStateFlow(AUTO_ID)
    val entryId: StateFlow<Long>
        get() = _entryId

    val fullEntry: StateFlow<FullEntry?> = _entryId.flatMapLatest {
        repository.getFullEntryFlowById(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun loadEntry(id: Long?) {
        _entryId.value = id ?: AUTO_ID
    }
}