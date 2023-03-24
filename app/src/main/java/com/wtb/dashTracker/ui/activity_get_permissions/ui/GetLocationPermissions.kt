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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_get_permissions.ui.composables.PageIndicator
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.headerIconColor
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.FontFamilyFiraSans
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_LOCATION
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun ColumnScope.GetLocationPermissionsScreen(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null
): Unit =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Location and Activity Permissions",
        subtitleText = "Required for automatic mileage tracking",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.LocationOn,
                contentDescription = "My Location",
                modifier = Modifier.size(96.dp),
                tint = headerIconColor()
            )
        },
        mainContent = {
            CustomOutlinedCard {
                Text(
                    text = stringResource(id = R.string.dialog_location_permission),
                    fontSize = fontSizeDimensionResource(id = R.dimen.text_size_med),
                    fontFamily = FontFamilyFiraSans
                )
            }

            PrivacyPolicyLink()

            FillSpacer()

            SecondaryCard {
                val str = buildAnnotatedString {
                    append("To grant location permissions, select ")

                    withStyle(style = styleBold) {
                        append("OK")
                    }

                    append(" then allow location access ")

                    withStyle(style = styleBold) {
                        append("While using the app")
                    }

                    append(", then ")

                    withStyle(style = styleBold) {
                        append("Allow")
                    }

                    append(" physical activity access.")
                }
                Text(str, modifier = Modifier.padding(marginDefault()))
            }
        },
        navContent = {
            GetLocationPermissionsNav(activity = activity)
        }
    )

@Composable
fun PrivacyPolicyLink() {
    val uriHandler = LocalUriHandler.current
    TextButton(
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.tertiary,
        ),
        onClick = {
            uriHandler.openUri("https://www.benica.dev/privacy")
        }
    ) {
        Text("Privacy Policy")
    }
}

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun GetLocationPermissionsNav(
    activity: OnboardingMileageActivity? = null
) {
    BottomNavButtons {
        CustomTextButton(
            onClick = {
                activity?.setOptOutLocation(true)
                activity?.setLocationEnabled(false)
                activity?.setBooleanPref(activity.ASK_AGAIN_LOCATION, false)
            },
        ) {
            Text("No thanks")
        }

        HalfSpacer()

        CustomTextButton(
            onClick = {
                activity?.setOptOutLocation(false)
                activity?.setLocationEnabled(false)
                activity?.setBooleanPref(activity.ASK_AGAIN_LOCATION, true)
                activity?.finish()
            },
        ) {
            Text("Maybe later")
        }

        HalfSpacer()

        CustomButton(
            onClick = {
                activity?.setOptOutLocation(false)
                activity?.setLocationEnabled(true)
                activity?.setBooleanPref(activity.ASK_AGAIN_LOCATION, false)
                activity?.getLocationPermissions()
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
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun GetLocationPermissionsPreview() {
    DashTrackerTheme {
        ActivityScreen {
            GetLocationPermissionsScreen()

            PageIndicator(
                numPages = 5,
                selectedPage = 1
            )
        }
    }
}