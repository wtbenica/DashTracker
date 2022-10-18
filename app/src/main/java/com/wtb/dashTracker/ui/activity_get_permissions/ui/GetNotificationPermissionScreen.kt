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

import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_get_permissions.PageIndicator
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.FontFamilyFiraSans
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREFS_ASK_AGAIN_NOTIFICATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.SHOW_NOTIFICATION
import kotlinx.coroutines.ExperimentalCoroutinesApi

@RequiresApi(TIRAMISU)
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@Composable
fun GetNotificationPermissionScreen(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null
) =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Notifications Permission",
        subtitleText = "Optional",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.Notifications,
                contentDescription = "Notifications Icon",
                modifier = Modifier.size(96.dp)
            )
        },
        mainContent = {
            CustomOutlinedCard {
                Text(
                    text = stringResource(id = R.string.dialog_notification_permission),
                    fontSize = 18.sp,
                    fontFamily = FontFamilyFiraSans
                )
            }
        },
        navContent = {
            GetNotificationsPermissionNav(activity = activity)
        }
    )

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
@RequiresApi(TIRAMISU)
fun GetNotificationsPermissionNav(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        FillSpacer()

        CustomTextButton(
            onClick = {
                activity?.let { it.setOptOutPref(activity.SHOW_NOTIFICATION, false) }
//                activity?.setOptOutPref(PREFS_OPT_OUT_NOTIFICATION, true)
                activity?.setOptOutPref(PREFS_ASK_AGAIN_NOTIFICATION, false)
            },
        ) {
            Text("No thanks")
        }

        DefaultSpacer()

        CustomTextButton(
            onClick = {
                activity?.let { it.setOptOutPref(activity.SHOW_NOTIFICATION, true) }
//                activity?.setOptOutPref(PREFS_OPT_OUT_NOTIFICATION, false)
                activity?.setOptOutPref(PREFS_ASK_AGAIN_NOTIFICATION, true)
            },
        ) {
            Text("Maybe later")
        }

        DefaultSpacer()

        CustomOutlinedButton(
            onClick = {
                activity?.let { it.setOptOutPref(activity.SHOW_NOTIFICATION, true) }
//                activity?.setOptOutPref(PREFS_OPT_OUT_NOTIFICATION, false)
                activity?.setOptOutPref(PREFS_ASK_AGAIN_NOTIFICATION, false)
                activity?.getNotificationPermission()
            },
        ) {
            HalfSpacer()
            Text("OK")
            Icon(
                Icons.Rounded.NavigateNext,
                contentDescription = "Next screen",
                modifier = Modifier.padding(0.dp)
            )
        }
    }
}

@ExperimentalCoroutinesApi
@RequiresApi(TIRAMISU)
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Preview
@Composable
fun PreviewGetNotificationPermissionScreen() {
    DashTrackerTheme {
        Surface {
            Column {
                GetNotificationPermissionScreen()
                PageIndicator(
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                    numPages = 4,
                    selectedPage = 3
                )
            }
        }
    }
}