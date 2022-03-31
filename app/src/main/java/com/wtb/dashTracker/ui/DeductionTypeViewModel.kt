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

import android.util.Log
import androidx.lifecycle.ViewModel
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.repository.DeductionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeductionTypeViewModel : ViewModel() {
    private val _deductionType: MutableStateFlow<DeductionType> =
        MutableStateFlow(DeductionType.NONE)

    val deductionType: StateFlow<DeductionType>
        get() = _deductionType

    fun setDeductionType(type: DeductionType) {
        _deductionType.value = type
        Log.d(
            APP + DeductionTypeViewModel::class.simpleName,
            "setDeductionType: ${type.name} ${deductionType.value.name}"
        )
    }
}