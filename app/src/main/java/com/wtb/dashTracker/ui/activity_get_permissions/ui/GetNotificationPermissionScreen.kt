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
import android.os.Build
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.Notifications
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
import com.wtb.dashTracker.ui.activity_main.TAG
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.cardShape
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalTextApi
@Composable
fun GetNotificationPermissionScreen(modifier: Modifier = Modifier) =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Notifications Permission",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.Notifications,
                contentDescription = "Notifications Icon",
                modifier = Modifier.size(96.dp)
            )
        },
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
                    stringResource(id = R.string.dialog_location_permission),
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

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun GetNotificationsPermissionNav(activity: GetPermissionsActivity? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp, 0.dp, 8.dp, 8.dp)
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Log.d(TAG, "GetNotPerm: activity? ${activity == null}")
                    activity?.getNotificationPermission()
                } else {
                    Log.d(TAG, "GetNotPerm: End!")
                    activity?.finish()
                }
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

@ExperimentalMaterial3Api
@ExperimentalTextApi
@Preview
@Composable
fun PreviewGetNotificationPermissionScreen() {
    DashTrackerTheme {
        Surface {
            Column {
                GetNotificationPermissionScreen(modifier = Modifier.weight(1f, true))
                GetNotificationsPermissionNav()
            }
        }
    }
}