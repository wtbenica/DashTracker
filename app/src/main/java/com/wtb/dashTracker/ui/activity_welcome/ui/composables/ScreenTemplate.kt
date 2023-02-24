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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AccessAlarm
import androidx.compose.material.icons.twotone.Dangerous
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.ui.OnboardingIntroNav
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.headerIconColor
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.cardShape
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalTextApi
@Composable
fun ScreenTemplate(
    modifier: Modifier = Modifier,
    headerText: String,
    subtitleText: String? = null,
    iconImage: @Composable (ColumnScope.() -> Unit),
    mainContent: @Composable (ColumnScope.() -> Unit),
    navContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp)
    ) {
        DefaultSpacer()

        OutlinedCard(
            shape = cardShape,
            colors = cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            border = BorderStroke(width = borderStrokeWidth, color = MaterialTheme.colorScheme.primaryContainer)
        ) {
            WideSpacer()

            Row(verticalAlignment = Alignment.CenterVertically) {
                FillSpacer()

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = headerText,
                        modifier = Modifier.padding(0.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = subtitleText?.let { 18.sp } ?: 20.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    subtitleText?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(0.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp,
                        )
                    }
                }

                FillSpacer()

                Card(
                    modifier = Modifier
                        .width(128.dp)
                        .height(96.dp)
                        .align(Alignment.CenterVertically)
                        .padding(end = dimensionResource(R.dimen.margin_half)),
                    shape = cardShape,
                    colors = cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.tertiary
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        iconImage()
                    }
                }
            }

            WideSpacer()
        }

        Spacer(modifier = Modifier.size(64.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            mainContent()
        }

        navContent?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 16.dp)
            ) {
                it()
            }
        }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
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
                        tint = headerIconColor()
                    )
                },
                mainContent = {
                    ContentCard(
                        titleText = "ContentCard",
                        icon = Icons.TwoTone.AccessAlarm,
                        iconDescription = "Access Alarm"
                    )
                },
                navContent = {
                    OnboardingIntroNav()
                }
            )
        }
    }
}


@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewScreenTemplateNight() {
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
                        tint = headerIconColor()
                    )
                },
                mainContent = {
                    ContentCard(
                        titleText = "ContentCard",
                        icon = Icons.TwoTone.AccessAlarm,
                        iconDescription = "Access Alarm"
                    )
                },
                navContent = {
                    OnboardingIntroNav()
                }
            )
        }
    }
}

val styleBold: SpanStyle =
    SpanStyle(fontWeight = FontWeight.Bold)
