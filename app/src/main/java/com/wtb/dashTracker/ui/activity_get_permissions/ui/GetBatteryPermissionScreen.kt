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

package com.wtb.dashTracker.ui.activity_get_permissions.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.BatterySaver
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.GetPermissionsActivity
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.cardShape
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalTextApi
@Composable
fun GetBatteryPermission(modifier: Modifier = Modifier) =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Battery Optimization",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.BatterySaver,
                contentDescription = "Battery Saver Icon",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    ) {
        item {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    stringResource(id = R.string.dialog_battery_permission),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item {
            val uriHandler = LocalUriHandler.current

            CustomTextButton(onClick = {
                uriHandler.openUri("https://www.benica.dev")
            }) {
                Text("Privacy Policy")
            }
        }
    }

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun GetBatteryPermissionNav(activity: GetPermissionsActivity? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        LocalContext.current
        FillSpacer()

        CustomTextButton(
            onClick = {
                activity?.setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(
                        WelcomeActivity.ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN,
                        WelcomeActivity.Companion.MileageTrackingOptIn.OPT_OUT
                    )
                )
                activity?.finish()
            },
        ) {
            Text("No thanks")
        }

        HalfSpacer()

        CustomTextButton(
            onClick = {
                activity?.setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(
                        WelcomeActivity.ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN,
                        WelcomeActivity.Companion.MileageTrackingOptIn.DECIDE_LATER
                    )
                )
                activity?.finish()
            },
        ) {
            Text("Maybe later")
        }

        HalfSpacer()

        CustomOutlinedButton(
            onClick = {
                activity?.getBatteryPermission()
            },
        ) {
            HalfSpacer()
            Text("Allow")
            Icon(
                Icons.Rounded.NavigateNext,
                contentDescription = "Next screen",
                modifier = Modifier.padding(0.dp)
            )
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Preview(showBackground = true)
@Composable
fun GetBatteryPermissionPreview() {
    DashTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                GetBatteryPermission(modifier = Modifier.weight(1f))
                GetBatteryPermissionNav()
            }
        }
    }
}
