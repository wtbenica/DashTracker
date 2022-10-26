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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.util.PermissionsHelper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.LOCATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_SUMMARY_SCREEN
import com.wtb.dashTracker.util.REQUIRED_PERMISSIONS
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
fun SummaryScreen(modifier: Modifier = Modifier, activity: OnboardingMileageActivity) {
    val permissionsHelper = PermissionsHelper(activity)

    val locOff: @Composable (ColumnScope.() -> Unit) = {
        Icon(
            imageVector = Icons.Outlined.LocationOff,
            contentDescription = "Location Off",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
    }

    val locOn: @Composable (ColumnScope.() -> Unit) = {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = "Location On",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
    }

    val iconImage: @Composable (ColumnScope.() -> Unit) = if (
        permissionsHelper.hasPermissions(activity, *REQUIRED_PERMISSIONS)
        && permissionsHelper.sharedPrefs.getBoolean(activity.LOCATION_ENABLED, false)
    ) {
        locOn
    } else {
        locOff
    }
//        permissionsHelper.whenHasDecided(
//            optOutLocation = locOff,
//            hasAllPermissions = locOn,
//            hasNotification = locOn,
//            hasBgLocation = locOn,
//            hasLocation = locOff,
//            noPermissions = locOff
//        ) ?: locOn

    ScreenTemplate(
        modifier = modifier,
        headerText = "Mileage Tracking",
        iconImage = iconImage,
        mainContent = {
            CustomOutlinedCard {
                // TODO: whenHasDecided is not the appropriate function here. need to come up
                //  with one that is only whenHasPermissionsAndEnabled
                Text(
                    text = permissionsHelper.whenHasDecided(
                        optOutLocation = "You've opted out of mileage tracking. To turn it on, go" +
                                " to Settings.",
                        hasAllPermissions = "Mileage tracking is set up. You can make adjustments" +
                                " by going to Settings.",
                        hasNotification = "Mileage tracking is all set up. If you are getting " +
                                "inaccurate mileage, it's recommended that you turn off " +
                                "battery optimization for DashTracker, which you can do by going " +
                                "to Settings.",
                        hasBgLocation = "Mileage tracking is all set up. If you are getting " +
                                "inaccurate mileage, it's recommended that you turn off " +
                                "battery optimization for DashTracker, which you can do by going " +
                                "to Settings.",
                        hasLocation = "Mileage tracking is missing required permissions. Next " +
                                "time you press the Start Dash button, you will be given another " +
                                "chance to opt out or grant the required permissions.",
                        noPermissions = "Mileage tracking is missing required permissions. Next " +
                                "time you press the Start Dash button, you will be given another " +
                                "chance to opt out or grant the required permissions."
                    ) ?: throw IllegalStateException(
                        "Expected non-null result from whenPermissions"
                    )
                )
            }
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
