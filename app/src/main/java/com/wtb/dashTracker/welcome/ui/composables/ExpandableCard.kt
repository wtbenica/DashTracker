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

package com.wtb.dashTracker.welcome.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.welcome.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.welcome.ui.theme.primaryDark
import com.wtb.dashTracker.welcome.ui.theme.up

@ExperimentalTextApi
@Composable
fun ExpandableCard(
    text: String, icon: ImageVector, iconTint: Color, content: @Composable (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val rippleColor = MaterialTheme.colorScheme.secondary
    val cardShape = RoundedCornerShape(24.dp)

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
                        fontSize = 16.sp,
                        textAlign = TextAlign.Start,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        icon,
                        contentDescription = "Drawing of a car",
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

@OptIn(ExperimentalTextApi::class)
@Composable
@Preview
fun PreviewExpandableCard() {
    ExpandableCard(
        text = "Track your income",
        icon = Icons.TwoTone.AttachMoney,
        iconTint = up,
    )
}

@ExperimentalTextApi
@Composable
fun ListRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.TwoTone.Circle,
            contentDescription = "Bulleted item",
            modifier = Modifier.size(10.dp),
            tint = primaryDark
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text,
            fontSize = 15.sp,
        )
    }
}