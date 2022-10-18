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
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.OnboardingMileageActivity
import com.wtb.dashTracker.ui.activity_get_permissions.PageIndicator
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.FontFamilyFiraSans
import com.wtb.dashTracker.ui.theme.car
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun GetLocationPermissionsScreen(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null
) =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Location and Activity Permissions",
        subtitleText = "Required for automatic mileage tracking",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.LocationOn,
                contentDescription = "My Location",
                modifier = Modifier.size(96.dp),
                tint = car
            )
        },
        mainContent = {
            CustomOutlinedCard {
                val str = buildAnnotatedString {
                    append(stringResource(id = R.string.dialog_location_permission_1))

                    withStyle(style = styleBold) {
                        append(stringResource(id = R.string.dialog_location_permission_2_ital))
                    }

                    append(stringResource(id = R.string.dialog_location_permission_3))

                    withStyle(style = styleBold) {
                        append(stringResource(id = R.string.dialog_location_permission_4_ital))
                    }

                    append(stringResource(id = R.string.dialog_location_permission_5))
                }

                Text(
                    text = str,
                    fontSize = 18.sp,
                    fontFamily = FontFamilyFiraSans
                )
            }

            val uriHandler = LocalUriHandler.current

            CustomTextButton(
                onClick = { uriHandler.openUri("https://www.benica.dev/privacy") }
            ) {
                Text("Privacy Policy")
            }
        },
        navContent = {
            GetLocationPermissionsNav(activity = activity)
        }
    )

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun GetLocationPermissionsNav(
    activity: OnboardingMileageActivity? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        LocalContext.current
        FillSpacer()

        CustomTextButton(
            onClick = {
                activity?.setOptOutLocation(true)
            },
        ) {
            Text("No thanks")
        }

        DefaultSpacer()

        CustomTextButton(
            onClick = {
                activity?.setOptOutLocation(false)
                activity?.finish()
            },
        ) {
            Text("Maybe later")
        }

        DefaultSpacer()

        CustomOutlinedButton(
            onClick = {
                activity?.setOptOutLocation(false)
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
@Composable
fun GetLocationPermissionsPreview() {
    DashTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                GetLocationPermissionsScreen()
                PageIndicator(
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                    numPages = 4,
                    selectedPage = 1
                )
            }
        }
    }
}