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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.twotone.AttachMoney
import androidx.compose.material.icons.twotone.Circle
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.cardShape
import com.wtb.dashTracker.ui.theme.primaryDark
import com.wtb.dashTracker.ui.theme.up

@ExperimentalTextApi
@Composable
fun ExpandableCard(
    text: String,
    icon: ImageVector,
    iconTint: Color,
    iconDescription: String,
    content: @Composable() (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    DashTrackerTheme {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .clickable(content != null) { isExpanded = !isExpanded },
            shape = cardShape,
            colors = cardColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary)
        ) {
            Column {
                Row(
                    modifier = if (content == null)
                        Modifier.padding(all = 16.dp)
                    else
                        Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        icon,
                        contentDescription = iconDescription,
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp),
                        tint = iconTint
                    )
                }

                if (content != null) {
                    AnimatedVisibility(visible = isExpanded) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                        ) {
                            content()
                        }
                    }
                }
            }

            if (content != null) {
                Crossfade(
                    targetState = isExpanded,
                ) {
                    when (it) {
                        true -> Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Show less",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                        )
                        false -> Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Show more",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalTextApi
@Composable
fun CustomOutlinedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = DashTrackerTheme {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape),
        shape = cardShape,
        colors = cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            content()
        }
    }
}

@ExperimentalTextApi
@Composable
fun ContentCard(
    text: String,
    icon: ImageVector,
    iconTint: Color,
    iconDescription: String,
    content: @Composable() (() -> Unit)? = null
) {
    DashTrackerTheme {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape),
            shape = cardShape,
            colors = cardColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary)
        ) {
            Column {
                Row(
                    modifier = if (content == null)
                        Modifier.padding(all = 16.dp)
                    else
                        Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        icon,
                        contentDescription = iconDescription,
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp),
                        tint = iconTint
                    )
                }

                if (content != null) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
@Preview
fun PreviewExpandableCard() {
    Column {
        ContentCard(
            text = "Track your income",
            icon = Icons.TwoTone.AttachMoney,
            iconTint = up,
            iconDescription = "Drawing of a car",
        ) {
            Column {
                ListRow("Is today Monday?")
                ListRow(
                    "\"Eat your potato salad, dearie,\" said the wolf to the crow, as the night " +
                            "melody played of whistles and skeetleburrs."
                )
            }
        }

        DefaultSpacer()

        ExpandableCard(
            text = "Track your income",
            icon = Icons.TwoTone.AttachMoney,
            iconTint = up,
            iconDescription = "Drawing of a car",
        ) {
            ListRow("Eat your potato salad, dearie")
        }

        DefaultSpacer()

        CustomOutlinedCard {
            Text("This is just an outlined card")
        }
    }
}

@ExperimentalTextApi
@Composable
fun ListRow(
    text: String,
    rowModifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    iconSize: Dp = 18.dp,
    fontFamily: FontFamily? = MaterialTheme.typography.bodyLarge.fontFamily,
    icon: ImageVector = Icons.TwoTone.Circle
) {
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = "Bulleted item",
            modifier = Modifier.size(iconSize + 6.dp)
                .padding(start = 0.dp, top = 6.dp, end = 0.dp, bottom = 0.dp),
            tint = primaryDark
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text,
            fontSize = fontSize,
            fontFamily = fontFamily
        )
    }
}

@ExperimentalTextApi
@Composable
fun HeaderRow(text: String, rowModifier: Modifier = Modifier, icon: ImageVector) =
    ListRow(
        text = text,
        rowModifier = rowModifier,
        fontSize = 18.sp,
        iconSize = 18.dp,
        icon = icon
    )
