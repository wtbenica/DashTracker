/*
 * Copyright 2023 Wesley T. Benica
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

package com.wtb.dashTracker.ui.activity_welcome.ui.composables

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.wtb.dashTracker.ui.activity_get_permissions.ui.GetLocationPermissionsScreen
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Composable
fun ActivityScreen(content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            content()
        }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@Preview(showSystemUi = false)
@Composable
fun ActivityScreenPreview() {
    DashTrackerTheme {
        ActivityScreen {
            GetLocationPermissionsScreen()
        }
    }
}