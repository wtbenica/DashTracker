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
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.wtb.dashTracker.ui.theme.DashTrackerTheme

@ExperimentalTextApi
@Composable
fun CustomButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        onClick = onClick,
        modifier = modifier,
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
    if (isSystemInDarkTheme()) {
        CustomButton(onClick = onClick, modifier = modifier, content = content)
    } else {
        CustomOutlinedButton(onClick = onClick, modifier = modifier, content = content)
    }
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
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.inversePrimary),
        onClick = onClick,
        modifier = modifier,
        content = content
    )
}

@Composable
fun BottomNavButtons(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = marginHalf())
    ) {
        FillSpacer()
        content()
    }
}

@ExperimentalTextApi
@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewButtons() {
    DashTrackerTheme {
        Surface {
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(top = marginHalf(), bottom = marginNarrow())
                ) {
                    BottomNavButtons {
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
                }
            }
        }
    }
}