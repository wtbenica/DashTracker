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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.ui.theme.DashTrackerTheme

@ExperimentalTextApi
@Composable
fun CustomButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    DashTrackerTheme {
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            onClick = onClick,
            modifier = modifier,
            content = content
        )
    }
}

@ExperimentalTextApi
@Composable
fun CustomTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    DashTrackerTheme {
        TextButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.secondary,
            ),
            onClick = onClick,
            modifier = modifier,
            content = content,
        )
    }
}


@ExperimentalTextApi
@Composable
fun CustomOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    DashTrackerTheme {
        OutlinedButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.secondary),
            onClick = onClick,
            modifier = modifier,
            content = content
        )
    }
}

@ExperimentalTextApi
@Preview
@Composable
fun PreviewButtons() {
    DashTrackerTheme {
        Surface {
            Row(modifier = Modifier.padding(4.dp)) {
                CustomButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text(text = "Button")
                    Icon(Icons.Filled.FirstPage, contentDescription = "First Page")
                }

                Spacer(Modifier.width(8.dp))

                CustomTextButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text(text = "Text")
                    Icon(Icons.Filled.TextSnippet, contentDescription = "Text Snippet")
                }

                Spacer(Modifier.width(8.dp))

                CustomOutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text(text = "Outlined")
                    Icon(Icons.Outlined.Circle, contentDescription = "Circle")
                }
            }
        }
    }
}