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

package com.wtb.dashTracker.ui.activity_get_permissions

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.PinDrop
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
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.FontFamilyFiraSans
import com.wtb.dashTracker.ui.theme.car
import com.wtb.dashTracker.util.PermissionsHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@Composable
fun GetBgLocationPermissionScreen(
    modifier: Modifier = Modifier,
    activity: GetPermissionsActivity? = null
) =
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
                tint = car
            )
        },
        mainContent = {
            val str = buildAnnotatedString {
                append(stringResource(R.string.dialog_bg_location_text_1))

                withStyle(style = styleBold) {
                    append(stringResource(R.string.dialog_bg_location_text_2_ital))
                }

                append(stringResource(R.string.dialog_bg_location_text_3))
            }

            CustomOutlinedCard() {
                Text(
                    text = str,
                    fontSize = 18.sp,
                    fontFamily = FontFamilyFiraSans
                )
            }

            val uriHandler = LocalUriHandler.current

            CustomTextButton(onClick = {
                uriHandler.openUri("https://www.benica.dev")
            }) {
                Text("Privacy Policy")
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
    activity: GetPermissionsActivity? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        LocalContext.current
        FillSpacer()

        CustomTextButton(
            onClick = {
                activity?.setOptOutPref(PermissionsHelper.PREFS_OPT_OUT_LOCATION, true)
            },
        ) {
            Text("No thanks")
        }

        DefaultSpacer()

        CustomTextButton(
            onClick = {
                activity?.setOptOutPref(PermissionsHelper.PREFS_OPT_OUT_LOCATION, false)
                activity?.finish()
            },
        ) {
            Text("Maybe later")
        }

        DefaultSpacer()

        CustomOutlinedButton(
            onClick = {
                activity?.setOptOutPref(PermissionsHelper.PREFS_OPT_OUT_LOCATION, false)
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