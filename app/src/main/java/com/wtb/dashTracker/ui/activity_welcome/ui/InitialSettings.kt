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
import androidx.preference.PreferenceManager
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.FontFamilyFiraSans
import com.wtb.dashTracker.util.PermissionsHelper.Companion.AUTHENTICATION_ENABLED
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Composable
fun customSwitchColors(): SwitchColors {
    return SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.surface,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        checkedBorderColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
        uncheckedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedBorderColor = MaterialTheme.colorScheme.primary
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun InitialSettings(context: WelcomeActivity? = null) {
    ScreenTemplate(
        headerText = "Initial Setup",
        iconImage = {
            Icon(
                imageVector = Icons.TwoTone.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        mainContent = {
            CustomOutlinedCard {
                Column {
                    Row(
                        modifier = Modifier.padding(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.pref_title_show_weekly_adjustment_field),
                            fontWeight = FontWeight.Bold,
                        )
                        FillSpacer()

                        val showBPAs = remember { mutableStateOf(true) }
                        Switch(
                            checked = showBPAs.value, onCheckedChange = { newValue ->
                                showBPAs.value = newValue
                                context?.let {
                                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                                        .putBoolean(context.PREF_SHOW_BASE_PAY_ADJUSTS, newValue)
                                        .apply()
                                }
                            },
                            colors = customSwitchColors()
                        )
                    }

                    HalfSpacer()

                    Text(
                        text = stringResource(R.string.show_weekly_bpa_desc),
                        fontSize = fontSizeDimensionResource(id = R.dimen.text_size_med),
                        fontFamily = FontFamilyFiraSans
                    )
                }
            }

            HalfSpacer()

            CustomOutlinedCard {
                Column {
                    Row(
                        modifier = Modifier.padding(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.pref_title_require_authentication),
                            fontWeight = FontWeight.Bold
                        )
                        FillSpacer()

                        val authEnabled = remember { mutableStateOf(true) }

                        Switch(
                            checked = authEnabled.value, onCheckedChange = { newValue ->
                                authEnabled.value = newValue
                                context?.let {
                                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                                        .putBoolean(context.AUTHENTICATION_ENABLED, newValue)
                                        .apply()
                                }
                            },
                            colors = customSwitchColors()
                        )
                    }

                    HalfSpacer()

                    Text(
                        text = stringResource(R.string.require_pin_desc),
                        fontSize = fontSizeDimensionResource(id = R.dimen.text_size_med),
                        fontFamily = FontFamilyFiraSans
                    )
                }
            }
        },
        navContent = {
            InitialSettingsNav(callback = context as InitialScreenCallback?)
        }
    )
}

interface InitialScreenCallback {
    fun nextScreen2()
}

@ExperimentalTextApi
@Composable
fun InitialSettingsNav(callback: InitialScreenCallback? = null) {
    val cb = callback ?: object : InitialScreenCallback {
        override fun nextScreen2() {}
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(1f))

        CustomOutlinedButton(
            onClick = {
                cb.nextScreen2()
            },
        ) {
            HalfSpacer()
            Text("Mileage Tracking")
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