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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.wtb.dashTracker.ui.activity_get_permissions.ui.composables.PageIndicator
import com.wtb.dashTracker.ui.theme.DashTrackerTheme

@ExperimentalTextApi
@Composable
fun CustomButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        content = content
    )
}

@ExperimentalTextApi
@Composable
fun CustomTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.inversePrimary,
        ),
        onClick = onClick,
        modifier = modifier,
        content = content,
    )
}

@ExperimentalTextApi
@Composable
fun DefaultButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit
) {
    CustomOutlinedButton(onClick = onClick, modifier = modifier, content = content)
}


@ExperimentalTextApi
@Composable
fun CustomOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onTertiaryContainer),
        onClick = onClick,
        modifier = modifier,
        content = content
    )
}

@Composable
fun BottomNavButtons(content: @Composable RowScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onTertiary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = marginHalf(),
                    top = marginHalf(),
                    end = marginHalf(),
                    bottom = marginNarrow()
                )
        ) {
            FillSpacer()
            content()
        }
    }
}

@ExperimentalTextApi
@Preview(device = "spec:width=600dp,height=800dp,dpi=480")
@Preview(uiMode = UI_MODE_NIGHT_YES, device = "spec:width=600dp,height=800dp,dpi=480")
@Composable
fun PreviewButtons() {
    DashTrackerTheme {
        Surface {
            Column {
                BottomNavButtons {
                    DefaultButton(onClick = { }) {
                        Text(text = "Default Button")
                        Icon(Icons.Filled.FirstPage, contentDescription = "First Page")
                    }

                    HalfSpacer()

                    CustomButton(onClick = { }) {
                        Text(text = "Button")
                        Icon(Icons.Filled.FirstPage, contentDescription = "First Page")
                    }

                    HalfSpacer()

                    CustomTextButton(onClick = {}) {
                        Text(text = "Text")
                        Icon(Icons.Filled.TextSnippet, contentDescription = "Text Snippet")
                    }

                    HalfSpacer()

                    CustomOutlinedButton(onClick = {}) {
                        Text(text = "Outlined")
                        Icon(Icons.Outlined.Circle, contentDescription = "Circle")
                    }
                }

                PageIndicator(numPages = 4)
            }
        }
    }
}