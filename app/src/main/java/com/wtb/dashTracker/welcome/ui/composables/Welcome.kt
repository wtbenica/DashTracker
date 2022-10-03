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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.twotone.AttachMoney
import androidx.compose.material.icons.twotone.DirectionsCar
import androidx.compose.material.icons.twotone.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.wtb.dashTracker.R
import com.wtb.dashTracker.welcome.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.welcome.ui.theme.car
import com.wtb.dashTracker.welcome.ui.theme.down
import com.wtb.dashTracker.welcome.ui.theme.up

sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object WelcomeScreen : Screen("welcome", R.string.route_welcome)
    object WhatsNewScreen : Screen("whats_new", R.string.route_whats_new)
}

@ExperimentalTextApi
@ExperimentalMaterial3Api
@Composable
fun Welcome(modifier: Modifier = Modifier) {
    DashTrackerTheme {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    verticalAlignment = CenterVertically,
                ) {
                    Text(
                        text = "Welcome to DashTracker",
                        modifier = Modifier
                            .padding(16.dp)
                            .wrapContentHeight()
                            .weight(1f),
                        fontSize = 20.sp,
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedCard(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(96.dp)
                            .width(96.dp)
                            .align(CenterVertically),
                    ) {
                        Logo()
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            val rowSpacing = 8.dp

            item {
                ExpandableCard(
                    text = "Track your income",
                    icon = Icons.TwoTone.AttachMoney,
                    iconTint = up,
                ) {
                    Column {
                        stringArrayResource(id = R.array.track_income).forEach { ListRow(it) }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(rowSpacing))
            }

            item {
                ExpandableCard(
                    text = "Track your expenses",
                    icon = Icons.TwoTone.Wallet,
                    iconTint = down,
                ) {
                    Column {
                        stringArrayResource(id = R.array.track_expense).forEach { ListRow(it) }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(rowSpacing))
            }

            item {
                ExpandableCard(
                    text = "Track your mileage",
                    icon = Icons.TwoTone.DirectionsCar,
                    iconTint = car,
                ) {
                    Column {
                        stringArrayResource(id = R.array.track_mileage).forEach { ListRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeNav(navHostController: NavHostController) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(1f))

        CustomButton(
            onClick = {
                navHostController.navigate(Screen.WhatsNewScreen.route) {
                    launchSingleTop = true
                }
            },
        ) {
            Text("What's New")
            Icon(
                Icons.Rounded.ArrowForward,
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
        Column {
            Welcome(modifier = Modifier.weight(1f))
            WelcomeNav(navHostController = navController)
        }
    }
}

