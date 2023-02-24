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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.PriceCheck
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.headerIconColor
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BATTERY_OPTIMIZER
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BG_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_NOTIFICATION
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@Composable
fun OnboardingIntroScreen(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null
): Unit =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Automatic Mileage Tracking",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.LocationOn,
                contentDescription = "",
                modifier = Modifier.size(96.dp),
                tint = headerIconColor()
            )
        },
        mainContent = {
            val icons = listOf(
                Icons.Outlined.AccessTime,
                Icons.Outlined.PriceCheck,
                Icons.Outlined.VisibilityOff
            )
            stringArrayResource(id = R.array.list_whats_new_mileage_tracking)
                .forEachIndexed { i, it ->
                    if (i != 0) {
                        WideSpacer()
                    }

                    ContentCard(
                        titleText = it,
                        icon = icons[i],
                        iconDescription = "Punch Clock"
                    )
                }

            FillSpacer()

            SecondaryCard {
                val str = buildAnnotatedString {
                    append("To grant permissions, select ")

                    withStyle(style = styleBold) {
                        append("I'm in.")
                    }
                }
                Text(str, modifier = Modifier.padding(24.dp))
            }
        },
        navContent = {
            OnboardingIntroNav(activity)
        }
    )

/**
 * Provides buttons for opting-in or -out to automatic mileage tracking: opt out, opt in, or
 * decide later. Sets the result for [activity] and calls [WelcomeActivity.finish]
 */
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
@Composable
fun OnboardingIntroNav(activity: OnboardingMileageActivity? = null) {
    DashTrackerTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
        ) {
            FillSpacer()

            CustomTextButton(
                onClick = {
                    activity?.setOptOutLocation(true)
                    activity?.setLocationEnabled(false)
                    activity?.setBooleanPref(activity.ASK_AGAIN_LOCATION, false)
                },
            ) {
                Text("No thanks")
            }

            DefaultSpacer()

            CustomTextButton(
                onClick = {
                    activity?.setOptOutLocation(false)
                    activity?.setLocationEnabled(false)
                    activity?.setBooleanPref(activity.ASK_AGAIN_LOCATION, true)
                    activity?.setBooleanPref(activity.ASK_AGAIN_BG_LOCATION, true)
                    activity?.setBooleanPref(activity.ASK_AGAIN_NOTIFICATION, true)
                    activity?.setBooleanPref(activity.ASK_AGAIN_BATTERY_OPTIMIZER, true)
                },
            ) {
                Text("Maybe later")
            }

            DefaultSpacer()

            CustomButton(
                onClick = {
                    activity?.setOptOutLocation(false)
                    activity?.setLocationEnabled(true)
                    activity?.setBooleanPref(activity.ASK_AGAIN_LOCATION, false)
                },
            ) {
                HalfSpacer()
                Text("I'm in!")
                Icon(
                    Icons.Rounded.NavigateNext,
                    contentDescription = "Next screen",
                    modifier = Modifier.padding(0.dp)
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun PreviewWhatsNew() {
    DashTrackerTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingIntroScreen(modifier = Modifier.weight(1f))
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewWhatsNewNight() {
    DashTrackerTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingIntroScreen(modifier = Modifier.weight(1f))
        }
    }
}