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

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalTextApi
@ExperimentalMaterial3Api
@Composable
fun WhatsNewScreen(modifier: Modifier = Modifier) =
    ScreenTemplate(
        modifier = modifier,
        headerText = "What's New",
        iconImage = {
            Logo()
        }
    ) {
        item {
            ContentCard(
                text = "Automatic Mileage Tracking",
                icon = Icons.TwoTone.LocationOn,
                iconTint = MaterialTheme.colorScheme.secondary,
                iconDescription = "Location On"
            ) {
                Text(
                    text = stringResource(R.string.whats_new_mileage_tracking),
                    modifier = Modifier.padding(32.dp, 16.dp, 32.dp, 32.dp),
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        item {
            DefaultSpacer()
        }
    }

@ExperimentalMaterial3Api
@ExperimentalTextApi
@ExperimentalCoroutinesApi
@Composable
fun WhatsNewNav(activity: WelcomeActivity? = null) {
    DashTrackerTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 0.dp, 8.dp, 8.dp)
        ) {
            val context = LocalContext.current
            FillSpacer()

            CustomTextButton(
                onClick = {
                    activity?.setResult(
                        RESULT_OK,
                        Intent().putExtra(
                            WelcomeActivity.ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN,
                            WelcomeActivity.Companion.MileageTrackingOptIn.OPT_OUT
                        )
                    )
                    activity?.finish()
                },
            ) {
                Text("No thanks")
            }

            HalfSpacer()

            CustomTextButton(
                onClick = {
                    activity?.setResult(
                        RESULT_OK,
                        Intent().putExtra(
                            WelcomeActivity.ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN,
                            WelcomeActivity.Companion.MileageTrackingOptIn.DECIDE_LATER
                        )
                    )
                    activity?.finish()
                },
            ) {
                Text("Maybe later")
            }

            HalfSpacer()

            CustomOutlinedButton(
                onClick = {
                    activity?.setResult(
                        RESULT_OK,
                        Intent().putExtra(
                            WelcomeActivity.ACTIVITY_RESULT_MILEAGE_TRACKING_OPT_IN,
                            WelcomeActivity.Companion.MileageTrackingOptIn.OPT_IN
                        )
                    )
                    activity?.finish()
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

@ExperimentalTextApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun PreviewWhatsNew() {
    DashTrackerTheme {
        Column {
            WhatsNewScreen(modifier = Modifier.weight(1f))
            WhatsNewNav()
        }
    }
}