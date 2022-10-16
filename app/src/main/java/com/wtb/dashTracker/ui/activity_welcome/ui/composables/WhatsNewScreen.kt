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

package com.wtb.dashTracker.ui.activity_welcome.ui.composables

import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_get_permissions.GetPermissionsActivity
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.util.PermissionsHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalTextApi
@ExperimentalMaterial3Api
@Composable
fun WhatsNewScreen(modifier: Modifier = Modifier, activity: WelcomeActivity? = null) =
    ScreenTemplate(
        modifier = modifier,
        headerText = "Automatic Mileage Tracking",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.LocationOn,
                contentDescription = "",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.secondary
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
                        HalfSpacer()
                    }

                    ContentCard(
                        text = it,
                        icon = icons[i],
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconDescription = "Punch Clock",
                    )
                }
        }
    ) { WhatsNewNav(activity) }

/**
 * Provides buttons for opting-in or -out to automatic mileage tracking: opt out, opt in, or
 * decide later. Sets the result for [activity] and calls [WelcomeActivity.finish]
 */
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
@Composable
fun WhatsNewNav(activity: WelcomeActivity? = null) {
    DashTrackerTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
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
                },
            ) {
                Text("Maybe later")
            }

            DefaultSpacer()

            CustomOutlinedButton(
                onClick = {
                    activity?.setOptOutPref(PermissionsHelper.PREFS_OPT_OUT_LOCATION, false) {
                        activity.startActivity(Intent(activity, GetPermissionsActivity::class.java))
                    }
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
        Column {
            WhatsNewScreen(modifier = Modifier.weight(1f))
        }
    }
}