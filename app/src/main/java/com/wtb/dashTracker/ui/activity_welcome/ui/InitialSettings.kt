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

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_settings.SettingsActivity.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity.Companion.headerIconColor
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.*
import com.wtb.dashTracker.util.PermissionsHelper.Companion.AUTHENTICATION_ENABLED
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Composable
fun customSwitchColors(context: Context?): SwitchColors {
    return if (context != null) {
        SwitchDefaults.colors(
            checkedThumbColor = accentDark(context),
            checkedTrackColor = accentFaded(context),
            checkedBorderColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = accent(context),
            uncheckedTrackColor = accentFaded(context),
            uncheckedBorderColor = MaterialTheme.colorScheme.primary
        )
    } else {
        SwitchDefaults.colors(
            checkedThumbColor = secondaryDark(),
            checkedTrackColor = secondaryFaded(),
            checkedBorderColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = secondary(),
            uncheckedTrackColor = secondaryFaded(),
            uncheckedBorderColor = MaterialTheme.colorScheme.primary
        )
    }
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun InitialSettings(context: Context? = null) {
    ScreenTemplate(
        headerText = stringResource(R.string.welcome_header_initial_setup),
        iconImage = {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = headerIconColor()
            )
        },
        mainContent = {
            CustomOutlinedCard(padding = 0) {
                Column {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(
                                    vertical = dimensionResource(R.dimen.margin_half),
                                    horizontal = dimensionResource(id = R.dimen.margin_wide)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.pref_title_show_weekly_adjustment_field),
                                fontWeight = FontWeight.Bold,
                            )

                            FillSpacer()

                            val showBPAs = remember { mutableStateOf(true) }
                            Switch(
                                checked = showBPAs.value,
                                onCheckedChange = { newValue ->
                                    showBPAs.value = newValue
                                    context?.let {
                                        PreferenceManager.getDefaultSharedPreferences(context)
                                            .edit()
                                            .putBoolean(
                                                context.PREF_SHOW_BASE_PAY_ADJUSTS,
                                                newValue
                                            )
                                            .apply()
                                    }
                                },
                                colors = customSwitchColors(context)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.show_weekly_bpa_desc),
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.margin_wide)),
                        fontSize = fontSizeDimensionResource(id = R.dimen.text_size_med),
                        fontFamily = FontFamilyFiraSans
                    )
                }
            }

            HalfSpacer()

            CustomOutlinedCard(padding = 0) {
                Column {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(
                                    vertical = dimensionResource(R.dimen.margin_half),
                                    horizontal = dimensionResource(id = R.dimen.margin_wide)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.pref_title_require_authentication),
                                fontWeight = FontWeight.Bold
                            )
                            FillSpacer()

                            val authEnabled = remember { mutableStateOf(true) }

                            Switch(
                                checked = authEnabled.value,
                                onCheckedChange = { newValue ->
                                    authEnabled.value = newValue
                                    context?.let {
                                        PreferenceManager.getDefaultSharedPreferences(context)
                                            .edit()
                                            .putBoolean(
                                                context.AUTHENTICATION_ENABLED,
                                                newValue
                                            )
                                            .apply()
                                    }
                                },
                                colors = customSwitchColors(context)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.require_pin_desc),
                        modifier = Modifier.padding(dimensionResource(R.dimen.margin_wide)),
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
            Text(stringResource(R.string.lbl_mileage_tracking))
            Icon(
                Icons.Rounded.NavigateNext,
                contentDescription = stringResource(R.string.content_desc_icon_next_screen)
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
    DashTrackerTheme {
        InitialSettings()
    }
}

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalTextApi
@Composable
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
fun ThisPreviewNight() {
    DashTrackerTheme {
        InitialSettings()
    }
}