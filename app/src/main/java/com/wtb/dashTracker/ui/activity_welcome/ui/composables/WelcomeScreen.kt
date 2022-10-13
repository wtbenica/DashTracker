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

import androidx.annotation.StringRes
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.car
import com.wtb.dashTracker.ui.theme.down
import com.wtb.dashTracker.ui.theme.up

sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object WelcomeScreen : Screen("route_welcome", R.string.route_welcome)
    object WhatsNewScreen : Screen("route_whats_new", R.string.route_whats_new)
}

@ExperimentalTextApi
@ExperimentalMaterial3Api
@Composable
fun WelcomeScreen(modifier: Modifier = Modifier, navHostController: NavHostController) =
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
                "Drawing of a car",
            ) {
                Column {
                    stringArrayResource(id = R.array.track_income).forEach {
                        ListRow(
                            it,
                            icon = Icons.TwoTone.Circle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(rowSpacing))

            ExpandableCard(
                text = "Track your expenses",
                icon = Icons.TwoTone.Wallet,
                iconTint = down,
                "Drawing of a car",
            ) {
                Column {
                    stringArrayResource(id = R.array.track_expense).forEach {
                        ListRow(
                            it,
                            icon = Icons.TwoTone.Circle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(rowSpacing))

            ExpandableCard(
                text = "Track your mileage",
                icon = Icons.TwoTone.DirectionsCar,
                iconTint = car,
                "Drawing of a car",
            ) {
                Column {
                    stringArrayResource(id = R.array.track_mileage).forEach {
                        ListRow(
                            it,
                            icon = Icons.TwoTone.Circle
                        )
                    }
                }
            }

            DefaultSpacer()
        }
    ) { WelcomeNav(navHostController = navHostController) }

@ExperimentalTextApi
@Composable
fun WelcomeNav(navHostController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(1f))

        CustomOutlinedButton(
            onClick = {
                navHostController.navigate(Screen.WhatsNewScreen.route) {
                    launchSingleTop = true
                }
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
    val navController = rememberNavController()
    DashTrackerTheme {
        Surface {
            Column {
                WelcomeScreen(modifier = Modifier.weight(1f), navController)
            }
        }
    }
}

