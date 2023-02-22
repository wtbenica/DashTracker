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
import androidx.compose.material.icons.twotone.PinDrop
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
import com.wtb.dashTracker.ui.activity_get_permissions.PageIndicator
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.headerIconColor
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.FontFamilyFiraSans
import com.wtb.dashTracker.ui.theme.accent
import com.wtb.dashTracker.util.PermissionsHelper.Companion.ASK_AGAIN_BG_LOCATION
import com.wtb.dashTracker.util.PermissionsHelper.Companion.OPT_OUT_LOCATION
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@Composable
fun GetBgLocationPermissionScreen(
    modifier: Modifier = Modifier,
    activity: OnboardingMileageActivity? = null
): Unit =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Background Location Permission",
        subtitleText = "Required for automatic mileage tracking",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.PinDrop,
                contentDescription = "Location symbol",
                modifier = Modifier
                    .size(96.dp),
                tint = headerIconColor()
            )
        },
        mainContent = {
            CustomOutlinedCard(context = activity) {
                Text(
                    text = stringResource(R.string.dialog_bg_location_text),
                    fontSize = fontSizeDimensionResource(id = R.dimen.text_size_med),
                    fontFamily = FontFamilyFiraSans
                )
            }

            val uriHandler = LocalUriHandler.current
            TextButton(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = accent(),
                ),
                onClick = {
                    uriHandler.openUri("https://www.benica.dev")
                }
            ) {
                Text("Privacy Policy")
            }

            FillSpacer()

            SecondaryCard {
                val str = buildAnnotatedString {
                    append("To grant background location permission, select ")

                    withStyle(style = styleBold) {
                        append("OK")
                    }

                    append(" then ")

                    withStyle(style = styleBold) {
                        append("Allow all the time")
                    }

                    append(", then return to DashTracker.")
                }
                Text(str, modifier = Modifier.padding(24.dp))
            }
        },
        navContent = {
            GetBgLocationPermissionNav(activity = activity)
        }
    )

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun GetBgLocationPermissionNav(
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
                activity?.setBooleanPref(activity.OPT_OUT_LOCATION, true)
                activity?.setLocationEnabled(false)
                activity?.setBooleanPref(activity.ASK_AGAIN_BG_LOCATION, false)
            },
        ) {
            Text("No thanks")
        }

        DefaultSpacer()

        CustomTextButton(
            onClick = {
                activity?.setBooleanPref(activity.OPT_OUT_LOCATION, false)
                activity?.setLocationEnabled(false)
                activity?.setBooleanPref(activity.ASK_AGAIN_BG_LOCATION, true)
            },
        ) {
            Text("Maybe later")
        }

        DefaultSpacer()

        CustomButton(
            onClick = {
                activity?.setBooleanPref(activity.OPT_OUT_LOCATION, false)
                activity?.setLocationEnabled(true)
                activity?.setBooleanPref(activity.ASK_AGAIN_BG_LOCATION, false)
                activity?.getBgPermission()
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
fun GetBgLocationPermissionPreview() {
    DashTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                GetBgLocationPermissionScreen()
                PageIndicator(
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                    numPages = 4,
                    selectedPage = 2
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
fun GetBgLocationPermissionPreviewNight() {
    DashTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column {
                GetBgLocationPermissionScreen()
                PageIndicator(
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                    numPages = 4,
                    selectedPage = 2
                )
            }
        }
    }
}