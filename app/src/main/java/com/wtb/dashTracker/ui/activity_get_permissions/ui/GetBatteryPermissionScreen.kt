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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.BatterySaver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_get_permissions.PageIndicator
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.headerIconColor
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.FontFamilyFiraSans
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.BG_BATTERY_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_BATTERY_OPTIMIZER
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@Composable
fun GetBatteryPermissionScreen(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null,
    finishWhenDone: Boolean = false
): Unit =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Battery Optimization",
        subtitleText = "Optional",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.BatterySaver,
                contentDescription = "Battery Saver Icon",
                modifier = Modifier.size(96.dp),
                tint = headerIconColor()
            )
        },
        mainContent = {
            CustomOutlinedCard(context = activity) {
                val str = buildAnnotatedString {
                    append(stringResource(id = R.string.dialog_battery_permission))
                }


                Text(
                    text = str,
                    fontSize = fontSizeDimensionResource(id = R.dimen.text_size_med),
                    fontFamily = FontFamilyFiraSans
                )

            }

            FillSpacer()

            SecondaryCard {
                val str = buildAnnotatedString {
                    append("To grant unrestricted background battery use, select ")

                    withStyle(style = styleBold) {
                        append("OK")
                    }

                    append(", then ")

                    withStyle(style = styleBold) {
                        append("Battery")
                    }

                    append(", then ")

                    withStyle(style = styleBold) {
                        append("Unrestricted")
                    }

                    append(" then return to DashTracker.")
                }
                Text(str, modifier = Modifier.padding(24.dp))
            }
        },
        navContent = {
            GetBatteryPermissionNav(activity = activity, finishWhenDone = finishWhenDone)
        }
    )


@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun GetBatteryPermissionNav(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null,
    finishWhenDone: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        LocalContext.current
        FillSpacer()

        CustomTextButton(
            onClick = {
                activity?.setBooleanPref(activity.OPT_OUT_BATTERY_OPTIMIZER, true)
                activity?.setBooleanPref(activity.BG_BATTERY_ENABLED, false)
                activity?.setBooleanPref(activity.ASK_AGAIN_BATTERY_OPTIMIZER, false)
                if (finishWhenDone) activity?.finish()
            },
        ) {
            Text("No thanks")
        }

        DefaultSpacer()

        CustomTextButton(
            onClick = {
                activity?.setBooleanPref(activity.OPT_OUT_BATTERY_OPTIMIZER, false)
                activity?.setBooleanPref(activity.BG_BATTERY_ENABLED, false)
                activity?.setBooleanPref(activity.ASK_AGAIN_BATTERY_OPTIMIZER, true)
                if (finishWhenDone) activity?.finish()
            },
        ) {
            Text("Maybe later")
        }

        DefaultSpacer()

        CustomButton(
            onClick = {
                activity?.setBooleanPref(activity.OPT_OUT_BATTERY_OPTIMIZER, false)
                activity?.setBooleanPref(activity.BG_BATTERY_ENABLED, true)
                activity?.setBooleanPref(activity.ASK_AGAIN_BATTERY_OPTIMIZER, false)
                activity?.getBatteryPermission()
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
                GetBatteryPermissionScreen()
                PageIndicator(
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                    numPages = 4,
                    selectedPage = 4
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun GetBatteryPermissionPreviewNight() {
    DashTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                GetBatteryPermissionScreen()
                PageIndicator(
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                    numPages = 4,
                    selectedPage = 4
                )
            }
        }
    }
}
