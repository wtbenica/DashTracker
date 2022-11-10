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

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.BatterySaver
import androidx.compose.material3.*
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
fun ReenableBatteryOptimizationScreen(
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
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        mainContent = {
            CustomOutlinedCard {
                val str = buildAnnotatedString {
                    append(stringResource(id = R.string.reenable_battery_permission_1))

                    withStyle(style = styleBold) {
                        append(stringResource(id = R.string.reenable_battery_permission_2_ital))
                    }

                    append(stringResource(id = R.string.reenable_battery_permission_3))

                    withStyle(style = styleBold) {
                        append(stringResource(id = R.string.reenable_battery_permission_4_ital))
                    }

                    append(stringResource(id = R.string.reenable_battery_permission_5))

                    withStyle(style = styleBold) {
                        append(stringResource(id = R.string.reenable_battery_permission_6_ital))
                    }

                    append(stringResource(id = R.string.reenable_battery_permission_7))
                }


                Text(
                    text = str,
                    fontSize = fontSizeDimensionResource(id = R.dimen.text_size_med),
                    fontFamily = FontFamilyFiraSans
                )
            }

            FillSpacer()

            SecondaryOutlinedCard {
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
            ReenableBatteryOptimizationNav(activity = activity)
        }
    )


@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun ReenableBatteryOptimizationNav(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        LocalContext.current
        FillSpacer()

        CustomTextButton(
            onClick = {
                activity?.setBooleanPref(activity.BG_BATTERY_ENABLED, true)
                activity?.finish()
            },
        ) {
            Text("No thanks")
        }

        DefaultSpacer()

        CustomOutlinedButton(
            onClick = {
                activity?.setBooleanPref(activity.OPT_OUT_BATTERY_OPTIMIZER, true)
                activity?.setBooleanPref(activity.BG_BATTERY_ENABLED, false)
                activity?.setBooleanPref(activity.ASK_AGAIN_BATTERY_OPTIMIZER, false)
                activity?.getBatteryPermission(false)
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
fun ReenableBatteryOptimizationPreview() {
    DashTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                ReenableBatteryOptimizationScreen()
                PageIndicator(
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                    numPages = 4,
                    selectedPage = 4
                )
            }
        }
    }
}