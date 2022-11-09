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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_welcome.ui.Card.*
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.car
import com.wtb.dashTracker.ui.theme.down
import com.wtb.dashTracker.ui.theme.up

interface WelcomeScreenCallback {
    fun nextScreen()
}

enum class Card {
    NONE, INCOME, EXPENSES, MILEAGE
}

@ExperimentalTextApi
@ExperimentalMaterial3Api
@Composable
internal fun WelcomeScreen(modifier: Modifier = Modifier, callback: WelcomeScreenCallback) =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Welcome to DashTracker",
        iconImage = { Logo() },
        mainContent = {
            Column {
                var expandedCard by remember { mutableStateOf(NONE) }

                SingleExpandableCard(
                    text = "Track your income",
                    icon = Icons.TwoTone.MonetizationOn,
                    iconTint = up,
                    iconDescription = "Drawing of a car",
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
                    text = "Track your expenses",
                    icon = Icons.TwoTone.Wallet,
                    iconTint = down,
                    iconDescription = "Drawing of a car",
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
                    text = "Track your mileage",
                    icon = Icons.TwoTone.DirectionsCar,
                    iconTint = car,
                    iconDescription = "Drawing of a car",
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
        }
    ) { WelcomeNav(callback) }

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
            Text("Setup")
            Icon(
                Icons.Rounded.NavigateNext,
                contentDescription = "Next screen"
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

