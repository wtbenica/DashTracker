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

package com.wtb.dashTracker.ui.activity_welcome.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.util.PermissionsHelper.Companion.AUTHENTICATION_ENABLED
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun InitialSettings(context: WelcomeActivity? = null) {
    ScreenTemplate(
        headerText = "Settings",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        mainContent = {
            CustomOutlinedCard {
                Column {
                    Text("Weekly Pay Adjustment/Incentive", fontWeight = FontWeight.Bold)

                    Text(text = stringResource(R.string.lorem_ipsum))

                    HalfSpacer()

                    Row(
                        modifier = Modifier.padding(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show weekly adjustment field",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        FillSpacer()

                        val showBPAs = remember { mutableStateOf(true) }
                        Switch(checked = showBPAs.value, onCheckedChange = { newValue ->
                            showBPAs.value = newValue
                            context?.let {
                                PreferenceManager.getDefaultSharedPreferences(context).edit()
                                    .putBoolean(context.PREF_SHOW_BASE_PAY_ADJUSTS, newValue)
                                    .apply()
                            }
                        })
                    }
                }
            }

            HalfSpacer()

            CustomOutlinedCard {
                Column {
                    Text("Require fingerprint/PIN to unlock", fontWeight = FontWeight.Bold)

                    Text(text = stringResource(R.string.lorem_ipsum))

                    HalfSpacer()

                    Row(
                        modifier = Modifier.padding(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Require unlock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        FillSpacer()

                        val authEnabled = remember { mutableStateOf(true) }

                        Switch(checked = authEnabled.value, onCheckedChange = { newValue ->
                            authEnabled.value = newValue
                            context?.let {
                                PreferenceManager.getDefaultSharedPreferences(context).edit()
                                    .putBoolean(context.AUTHENTICATION_ENABLED, newValue)
                                    .apply()
                            }
                        })
                    }
                }
            }
        },
        navContent = {
            InitialSettingsNav(callback = context as InitialScreenCallback)
        }
    )
}

interface InitialScreenCallback {
    fun nextScreen2()
}

@ExperimentalTextApi
@Composable
fun InitialSettingsNav(callback: InitialScreenCallback) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(1f))

        CustomOutlinedButton(
            onClick = {
                callback.nextScreen2()
            },
        ) {
            HalfSpacer()
            Text("What's New")
            Icon(
                Icons.Rounded.NavigateNext,
                contentDescription = "Next screen"
            )
        }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@Composable
@Preview(showBackground = true)
fun ThisPreview() {
    InitialSettings()
}