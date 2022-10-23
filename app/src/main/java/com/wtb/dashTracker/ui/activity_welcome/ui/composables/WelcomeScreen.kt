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
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.AttachMoney
import androidx.compose.material.icons.twotone.Circle
import androidx.compose.material.icons.twotone.DirectionsCar
import androidx.compose.material.icons.twotone.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.car
import com.wtb.dashTracker.ui.theme.down
import com.wtb.dashTracker.ui.theme.up

interface WelcomeScreenCallback {
    fun nextScreen()
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
            val rowSpacing = 8.dp

            ExpandableCard(
                text = "Track your income",
                icon = Icons.TwoTone.AttachMoney,
                iconTint = up,
                iconDescription = "Drawing of a car",
            ) {
                Column {
                    stringArrayResource(id = R.array.track_income).forEach {
                        ListRow(text = it, icon = Icons.TwoTone.Circle)
                    }
                }
            }

            Spacer(modifier = Modifier.height(rowSpacing))

            ExpandableCard(
                text = "Track your expenses",
                icon = Icons.TwoTone.Wallet,
                iconTint = down,
                iconDescription = "Drawing of a car",
            ) {
                Column {
                    stringArrayResource(id = R.array.track_expense).forEach {
                        ListRow(text = it, icon = Icons.TwoTone.Circle)
                    }
                }
            }

            Spacer(modifier = Modifier.height(rowSpacing))

            ExpandableCard(
                text = "Track your mileage",
                icon = Icons.TwoTone.DirectionsCar,
                iconTint = car,
                iconDescription = "Drawing of a car",
            ) {
                Column {
                    stringArrayResource(id = R.array.track_mileage).forEach {
                        ListRow(text = it, icon = Icons.TwoTone.Circle)
                    }
                }
            }

            DefaultSpacer()
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
            Text("What's New")
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

