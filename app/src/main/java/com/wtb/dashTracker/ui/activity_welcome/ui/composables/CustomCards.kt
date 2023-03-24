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
import androidx.annotation.DimenRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.twotone.AttachMoney
import androidx.compose.material.icons.twotone.Circle
import androidx.compose.material.icons.twotone.SquareFoot
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.cardShape

@Composable
fun fontSizeDimensionResource(@DimenRes id: Int): TextUnit =
    dimensionResource(id = id).value.sp

@Composable
fun customCardColors(): CardColors {
    return cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

//        MaterialTheme.colorScheme.secondary

val borderStrokeWidth: Dp
    @Composable get() = dimensionResource(id = R.dimen.margin_skinny)


@ExperimentalTextApi
@Composable
fun SingleExpandableCard(
    text: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconDescription: String,
    isExpanded: Boolean,
    callback: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = true,
                    radius = Dp.Unspecified,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                enabled = content != null
            ) { callback() },
        shape = cardShape,
        colors = customCardColors(),
        border = BorderStroke(
            width = borderStrokeWidth,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column {
            Row(
                modifier = if (content == null) {
                    Modifier
                        .padding(all = marginDefault())
                } else {
                    Modifier
                        .padding(start = marginDefault(), top = marginDefault(), end = marginDefault())
                }
            ) {
                Text(
                    text = text,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Start,
                )

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
                            .padding(marginDefault())
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
                            .height(marginDefault()),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    false -> Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Show more",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(marginDefault()),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@ExperimentalTextApi
@Composable
fun CustomOutlinedCard(
    modifier: Modifier = Modifier,
    padding: Dp? = null,
    outlineColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable (ColumnScope.() -> Unit)
): Unit = OutlinedCard(
    modifier = modifier
        .fillMaxWidth()
        .clip(cardShape),
    shape = cardShape,
    colors = customCardColors(),
    border = BorderStroke(
        width = borderStrokeWidth,
        color = outlineColor
    ),
) {
    Column(
        modifier = Modifier.padding(
            padding ?: marginDefault()
        )
    ) {
        content()
    }
}


/**
 * Outlined card with title, icon, and content areas
 *
 * @param titleText
 * @param icon
 * @param iconTint
 * @param iconDescription
 * @param content
 */
@ExperimentalTextApi
@Composable
fun ContentCard(
    titleText: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconDescription: String,
    content: @Composable (() -> Unit)? = null
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape),
        shape = cardShape,
        colors = customCardColors(),
        border = BorderStroke(
            width = borderStrokeWidth,
            color = MaterialTheme.colorScheme.secondary
        )
    ) {
        Row(
            modifier = if (content == null) {
                Modifier.padding(all = marginDefault())
            } else {
                Modifier.padding(start = marginDefault(), top = marginDefault(), end = marginDefault())
            }
        ) {
            Text(
                text = titleText,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f),
                fontSize = 18.sp,
                textAlign = TextAlign.Start,
            )

            HalfSpacer()

            Icon(
                icon,
                contentDescription = iconDescription,
                modifier = Modifier
                    .size(48.dp),
                tint = iconTint
            )
        }

        if (content != null) {
            Row(
                modifier = Modifier
                    .padding(marginDefault())
            ) {
                content()
            }
        }
    }
}

@ExperimentalTextApi
@Composable
fun SecondaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
    ) {
        content()
    }
}


@ExperimentalTextApi
@Composable
fun ListRow(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    iconSize: Dp = 18.dp,
    fontFamily: FontFamily? = MaterialTheme.typography.bodyLarge.fontFamily,
    icon: ImageVector = Icons.TwoTone.Circle
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = "Bulleted item",
            modifier = Modifier
                .size(iconSize + 6.dp)
                .align(Alignment.CenterVertically)
                .padding(6.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@ExperimentalTextApi
@Composable
@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
fun PreviewSingleExpandableCard() {
    DashTrackerTheme {
        SingleExpandableCard(
            text = "Track Your Income",
            icon = Icons.TwoTone.SquareFoot,
            iconDescription = "Stinky Fish",
            isExpanded = true,
            callback = { },
        ) {
            ListRow("Eat your potato salad, dearie")
        }
    }
}

@ExperimentalTextApi
@Composable
@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
fun PreviewContentCard() {
    DashTrackerTheme {
        ContentCard(
            titleText = "Track your income as if your life depended on it i'm trying to see what " +
                    "happens when the text is long and I want to see if it is limited to nope",
            icon = Icons.TwoTone.AttachMoney,
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
    }
}


@ExperimentalTextApi
@Composable
@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
fun PreviewSecondaryCard() {
    DashTrackerTheme {
        SecondaryCard {
            val str = buildAnnotatedString {
                append("To grant location permissions, select ")

                withStyle(style = styleBold) {
                    append("OK")
                }

                append(" then allow location access ")

                withStyle(style = styleBold) {
                    append("While using the app")
                }

                append(", then ")

                withStyle(style = styleBold) {
                    append("Allow")
                }

                append(" physical activity access.")
            }
            Text(str, modifier = Modifier.padding(marginDefault()))
        }
    }
}