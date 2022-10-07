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

package com.wtb.dashTracker.ui.activity_welcome.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.cardShape

@ExperimentalTextApi
@Composable
fun ScreenTemplate(
    modifier: Modifier = Modifier,
    headerText: String,
    iconImage: @Composable ColumnScope.() -> Unit,
    mainContent: LazyListScope.() -> Unit
) {
    DashTrackerTheme {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = headerText,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .wrapContentHeight()
                        .weight(1f),
                    fontSize = 20.sp,
                )

                DefaultSpacer()

                OutlinedCard(
                    shape = cardShape,
                    modifier = Modifier
                        .height(96.dp)
                        .width(96.dp)
                        .align(Alignment.CenterVertically),
                ) {
                    iconImage()
                }
            }

            DefaultSpacer()

            LazyColumn(
                modifier = modifier
                    .fillMaxWidth(),
            ) {
                mainContent()
            }
        }
    }
}