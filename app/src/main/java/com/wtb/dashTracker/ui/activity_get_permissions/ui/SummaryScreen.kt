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
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.welcomeIconColor
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.util.*
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BG_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_NOTIFICATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.LOCATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_SUMMARY_SCREEN
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
fun SummaryScreen(modifier: Modifier = Modifier, activity: OnboardingMileageActivity) {
    val permHelp = PermissionsHelper(activity)

    fun getIconImage(): @Composable (ColumnScope.() -> Unit) {
        val locOff: @Composable (ColumnScope.() -> Unit) = {
            Icon(
                imageVector = Icons.Outlined.LocationOff,
                contentDescription = "Location Off",
                modifier = Modifier.size(96.dp),
                tint = welcomeIconColor()
            )
        }

        val locOn: @Composable (ColumnScope.() -> Unit) = {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = "Location On",
                modifier = Modifier.size(96.dp),
                tint = welcomeIconColor()
            )
        }

        val iconImage: @Composable (ColumnScope.() -> Unit) = if (
            activity.hasPermissions(*REQUIRED_PERMISSIONS)
            && permHelp.sharedPrefs.getBoolean(activity.LOCATION_ENABLED, false)
        ) {
            locOn
        } else {
            locOff
        }
        return iconImage
    }

    val iconImage: @Composable (ColumnScope.() -> Unit) = getIconImage()

    ScreenTemplate(
        modifier = modifier,
        headerText = "Mileage Tracking",
        iconImage = iconImage,
        mainContent = {
            CustomOutlinedCard {
                Text(
                    text = when {
                        permHelp.locationEnabled -> "Automatic mileage tracking is enabled." +
                                if (!activity.hasPermissions(*OPTIONAL_PERMISSIONS)
                                    && permHelp.sharedPrefs.getBoolean(
                                        activity.ASK_AGAIN_NOTIFICATION,
                                        false
                                    )
                                ) {
                                    " Next time you hit Start Dash, you will be asked again if you would like to enable notifications."
                                } else {
                                    ""
                                } +
                                if (!activity.hasBatteryPermission()
                                    && permHelp.sharedPrefs.getBoolean(
                                        activity.ASK_AGAIN_BATTERY_OPTIMIZER,
                                        false
                                    )
                                ) {
                                    " Next time you hit Start Dash, you will be asked again if you would like to allow unrestricted background battery use."
                                } else {
                                    ""
                                }
                        permHelp.sharedPrefs.getBoolean(activity.OPT_OUT_LOCATION, true) -> {
                            "You've opted out of automatic mileage tracking. If you change your " +
                                    "mind, you can turn it on by going to Settings."
                        }
                        permHelp.sharedPrefs.getBoolean(activity.ASK_AGAIN_LOCATION, false) ||
                                permHelp.sharedPrefs.getBoolean(
                                    activity.ASK_AGAIN_BG_LOCATION,
                                    false
                                ) -> "Next time you hit Start Dash, you will be asked whether you " +
                                "want to enable automatic mileage tracking. You can also enable " +
                                "automatic tracking from Settings."
                        else -> "Automatic mileage tracking is disabled. You can enable it from " +
                                "Settings."
                    }
                )
            }

            HalfSpacer()

            PermissionsSummaryCard(
                locationEnabled = permHelp.fgLocationEnabled,
                bgLocationEnabled = permHelp.bgLocationEnabled,
                notificationsEnabled = permHelp.notificationsEnabled,
                batteryOptimizationDisabled = permHelp.batteryOptimizationDisabled
            )
        },
        navContent = {
            SummaryScreenNav(activity = activity)
        }
    )
}


@ExperimentalCoroutinesApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
fun SummaryScreenNav(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        FillSpacer()

        CustomOutlinedButton(
            onClick = {
                activity?.setBooleanPref(activity.PREF_SHOW_SUMMARY_SCREEN, false)
                activity?.finish()
            },
        ) {
            HalfSpacer()
            Text("Done")
            Icon(
                Icons.Rounded.NavigateNext,
                contentDescription = "Next screen",
                modifier = Modifier.padding(0.dp)
            )
        }
    }
}

@ExperimentalTextApi
@Composable
fun PermRow(
    permDesc: String,
    permIcon: ImageVector,
    permIconDescription: String,
    isEnabled: Boolean,
    isRequired: Boolean = true
) {
    Row {
        Icon(permIcon, permIconDescription, tint = MaterialTheme.colorScheme.secondary)

        HalfSpacer()

        Column(modifier = Modifier.weight(1f)) {
            @Suppress("DEPRECATION")
            Text(
                text = permDesc,
                style = LocalTextStyle.current.merge(
                    TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Top,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
            )

            Text(text = if (isRequired) "Required" else "Optional", fontSize = 10.sp)
        }

        HalfSpacer()

        if (isEnabled)
            Icon(
                imageVector = Icons.TwoTone.Check,
                contentDescription = "enabled",
                tint = MaterialTheme.colorScheme.secondary
            )
        else
            Icon(
                imageVector = Icons.TwoTone.DoNotDisturb,
                contentDescription = "disabled",
                tint = MaterialTheme.colorScheme.error
            )
    }
}

@ExperimentalCoroutinesApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Preview(showBackground = true)
@Composable
fun SummaryScreenPreview() {
    ScreenTemplate(
        headerText = "Mileage Tracking",
        iconImage = {
            Icon(
                imageVector = Icons.Outlined.LocationOff,
                contentDescription = "Location Off",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        mainContent = {
            CustomOutlinedCard {
                Text(text = "Automatic mileage tracking is enabled")
            }

            HalfSpacer()

            PermissionsSummaryCard(
                locationEnabled = true,
                bgLocationEnabled = true,
                notificationsEnabled = true,
                batteryOptimizationDisabled = false
            )
        },
        navContent = {
            SummaryScreenNav()
        }
    )
}

@ExperimentalTextApi
@Composable
fun PermissionsSummaryCard(
    locationEnabled: Boolean,
    bgLocationEnabled: Boolean,
    notificationsEnabled: Boolean,
    batteryOptimizationDisabled: Boolean
) {
    CustomOutlinedCard {
        Text(text = "Permissions")

        DefaultSpacer()

        PermRow(
            permDesc = "Location",
            permIcon = Icons.TwoTone.LocationOn,
            permIconDescription = "location icon",
            isEnabled = locationEnabled
        )

        DefaultSpacer()

        PermRow(
            permDesc = "Background Location",
            permIcon = Icons.TwoTone.LocationSearching,
            permIconDescription = "location icon",
            isEnabled = bgLocationEnabled
        )

        DefaultSpacer()

        PermRow(
            permDesc = "Notifications",
            permIcon = Icons.TwoTone.Notifications,
            permIconDescription = "notifications icon",
            isEnabled = notificationsEnabled,
            isRequired = false
        )

        DefaultSpacer()

        PermRow(
            permDesc = "Battery Optimization Disabled",
            permIcon = Icons.TwoTone.BatterySaver,
            permIconDescription = "battery icon",
            isEnabled = batteryOptimizationDisabled,
            isRequired = false
        )
    }
}
