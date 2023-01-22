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

package com.wtb.dashTracker.ui.activity_welcome.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.Circle
import androidx.compose.material.icons.twotone.DirectionsCar
import androidx.compose.material.icons.twotone.MonetizationOn
import androidx.compose.material.icons.twotone.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_welcome.ui.SelectedCard.*
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.welcomeIconColor

interface WelcomeScreenCallback {
    fun nextScreen()
}

enum class SelectedCard {
    NONE, INCOME, EXPENSES, MILEAGE
}

@ExperimentalTextApi
@ExperimentalMaterial3Api
@Composable
internal fun WelcomeScreen(modifier: Modifier = Modifier, callback: WelcomeScreenCallback) =
    ScreenTemplate(
        modifier = modifier,
        headerText = stringResource(R.string.welcome_header_welcome_screen),
        iconImage = { Logo() },
        mainContent = {
            Column {
                var expandedCard by remember { mutableStateOf(NONE) }

                SingleExpandableCard(
                    text = stringResource(R.string.welcome_screen_item_track_income),
                    icon = Icons.TwoTone.MonetizationOn,
                    iconTint = welcomeIconColor(),
                    iconDescription = stringResource(R.string.content_desc_welcome_screen_wallet_icon),
                    isExpanded = expandedCard == INCOME,
                    callback = {
                        expandedCard =
                            if (expandedCard == INCOME) {
                                NONE
                            } else {
                                INCOME
                            }
                    },
                ) {
                    Column {
                        stringArrayResource(id = R.array.track_income).forEach {
                            ListRow(text = it, icon = Icons.TwoTone.Circle)
                        }
                    }
                }

                WideSpacer()

                SingleExpandableCard(
                    text = stringResource(R.string.welcome_screen_item_track_expenses),
                    icon = Icons.TwoTone.Wallet,
                    iconTint = welcomeIconColor(),
                    iconDescription = stringResource(R.string.content_desc_welcome_screen_wallet_icon),
                    isExpanded = expandedCard == EXPENSES,
                    callback = {
                        expandedCard =
                            if (expandedCard == EXPENSES) {
                                NONE
                            } else {
                                EXPENSES
                            }
                    },
                ) {
                    Column {
                        stringArrayResource(id = R.array.track_expense).forEach {
                            ListRow(text = it, icon = Icons.TwoTone.Circle)
                        }
                    }
                }

                WideSpacer()

                SingleExpandableCard(
                    text = stringResource(R.string.welcome_screen_item_track_mileage),
                    icon = Icons.TwoTone.DirectionsCar,
                    iconTint = welcomeIconColor(),
                    iconDescription = stringResource(R.string.content_desc_welcome_screen_car_icon),
                    isExpanded = expandedCard == MILEAGE,
                    callback = {
                        expandedCard =
                            if (expandedCard == MILEAGE) {
                                NONE
                            } else {
                                MILEAGE
                            }
                    },
                ) {
                    Column {
                        stringArrayResource(id = R.array.track_mileage).forEach {
                            ListRow(text = it, icon = Icons.TwoTone.Circle)
                        }
                    }

                    DefaultSpacer()
                }
            }
        },
        navContent = {
            WelcomeNav(callback)
        }
    )

@ExperimentalTextApi
@Composable
fun WelcomeNav(callback: WelcomeScreenCallback) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(1f))

        CustomOutlinedButton(
            onClick = {
                callback.nextScreen()
            },
        ) {
            HalfSpacer()
            Text(stringResource(R.string.lbl_setup))
            Icon(
                Icons.Rounded.NavigateNext,
                contentDescription = stringResource(R.string.content_desc_icon_next_screen)
            )
        }
    }
}

@ExperimentalTextApi
@ExperimentalMaterial3Api
@Preview
@Composable
fun PreviewWelcome() {
    val callback = object : WelcomeScreenCallback {
        override fun nextScreen() {

        }
    }

    DashTrackerTheme {
        Surface {
            Column {
                WelcomeScreen(modifier = Modifier.weight(1f), callback)
            }
        }
    }
}

@ExperimentalTextApi
@ExperimentalMaterial3Api
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewWelcomeNight() {
    val callback = object : WelcomeScreenCallback {
        override fun nextScreen() {

        }
    }

    DashTrackerTheme {
        Surface {
            Column {
                WelcomeScreen(modifier = Modifier.weight(1f), callback)
            }
        }
    }
}

