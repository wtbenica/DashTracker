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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AccessAlarm
import androidx.compose.material.icons.twotone.Dangerous
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.cardShape

@ExperimentalTextApi
@Composable
fun ScreenTemplate(
    modifier: Modifier = Modifier,
    headerText: String,
    subtitleText: String? = null,
    iconImage: @Composable() (ColumnScope.() -> Unit),
    mainContent: @Composable() (ColumnScope.() -> Unit),
    navContent: @Composable() (ColumnScope.() -> Unit)? = null
) {
    DashTrackerTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp, 16.dp, 16.dp, 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = headerText,
                        modifier = Modifier.padding(0.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    subtitleText?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 16.sp,
                        )
                    }
                }

                FillSpacer()

                OutlinedCard(
                    shape = cardShape,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.CenterVertically),
                ) {
                    iconImage()
                }
            }

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f, true),
                content = {
                    mainContent()
                }
            )

            navContent?.let {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    content = {
                        it()
                    }
                )
            }
        }
    }
}

@ExperimentalTextApi
@Preview(showBackground = true)
@Composable
fun PreviewScreenTemplate() {
    DashTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ScreenTemplate(
                headerText = "ScreenTemplate",
                subtitleText = "Subtitle",
                iconImage = {
                    Icon(
                        imageVector = Icons.TwoTone.Dangerous,
                        contentDescription = "Dangerous",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                mainContent = {
                    ContentCard(
                        text = "ContentCard",
                        icon = Icons.TwoTone.AccessAlarm,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconDescription = "Access Alarm"
                    )
                }
            )
        }
    }
}

val styleBold =
    SpanStyle(fontWeight = FontWeight.Bold)
