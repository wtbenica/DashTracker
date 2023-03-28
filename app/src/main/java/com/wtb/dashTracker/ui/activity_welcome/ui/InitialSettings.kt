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

import android.content.SharedPreferences
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_welcome.WelcomeActivity
import com.wtb.dashTracker.ui.activity_welcome.ui.composables.*
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.FontFamilyFiraSans
import com.wtb.dashTracker.ui.theme.cardShape
import com.wtb.dashTracker.ui.theme.headerIconColor
import com.wtb.dashTracker.util.PermissionsHelper
import com.wtb.dashTracker.util.PermissionsHelper.Companion.AUTHENTICATION_ENABLED
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SHOW_BASE_PAY_ADJUSTS
import com.wtb.dashTracker.util.PermissionsHelper.Companion.PREF_SKIP_WELCOME_SCREEN
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalTextApi
@Composable
fun ColumnScope.InitialSettings(activity: WelcomeActivity? = null) {
    val textSizeBody = R.dimen.text_size_sm

    val permissionsHelper = PermissionsHelper(LocalContext.current)

    val sharedPrefs: SharedPreferences? = permissionsHelper.sharedPrefs

    @Composable
    fun themePrefHeader() {
        Text(
            text = stringResource(id = R.string.pref_title_light_dark_theme),
            fontWeight = FontWeight.Bold
        )

        WideSpacer()

        var expanded by remember { mutableStateOf(false) }

        val mode: String = stringResource(permissionsHelper.uiMode.displayName)

        var selectedTheme by remember { mutableStateOf(mode) }

        val focusManager = LocalFocusManager.current

        val regularOutline = MaterialTheme.colorScheme.outlineVariant
        val focusedOutline = MaterialTheme.colorScheme.onError

        DefaultOutlinedCard(
            outlineColor = if (expanded) {
                focusedOutline
            } else {
                regularOutline
            }
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = it
                },
                modifier = Modifier.wrapContentWidth()
            ) {
                TextField(
                    value = selectedTheme,
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor()
                        .width(IntrinsicSize.Min),
                    readOnly = true,
                    textStyle = TextStyle(
                        fontSize = fontSizeDimensionResource(id = textSizeBody),
                        fontFamily = FontFamilyFiraSans,
                        baselineShift = BaselineShift(-.2f)
                    ),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    maxLines = 1,
                    shape = cardShape,
                )

                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(
                        extraSmall = RoundedCornerShape(marginHalf())
                    )
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                            focusManager.clearFocus(true)
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        PermissionsHelper.UiMode.values().forEach {
                            val uiModeText = stringResource(it.displayName)

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = uiModeText,
                                        fontSize = fontSizeDimensionResource(id = textSizeBody),
                                        fontFamily = FontFamilyFiraSans,
                                    )
                                },
                                onClick = {
                                    selectedTheme = uiModeText
                                    expanded = false
                                    focusManager.clearFocus()

                                    permissionsHelper.apply {
                                        val prev = uiModeIsDarkMode
                                        updateUiMode(selectedTheme) {
                                            if (prev != uiModeIsDarkMode) {
                                                setBooleanPref(
                                                    context.PREF_SKIP_WELCOME_SCREEN,
                                                    true
                                                )
                                                // I think this effectively restarts the activity bc
                                                // WelcomeActivity finishes, and in MainActivity's
                                                // onResume/Create, it restarts WelcomeActivity bc
                                                // it never actually finished. If not, I'm not sure
                                                // why this works. I could check, but if it works,
                                                // it works. For now.
                                                activity?.finish()
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun bpaPrefHeader() {
        Text(
            text = stringResource(R.string.pref_title_show_weekly_adjustment_field),
            fontWeight = FontWeight.Bold,
        )

        FillSpacer()
        val prefShowBpas = permissionsHelper.sharedPrefs?.getBoolean(
            permissionsHelper.context.PREF_SHOW_BASE_PAY_ADJUSTS,
            true
        ) ?: true
        val showBPAs = remember { mutableStateOf(prefShowBpas) }

        Switch(
            checked = showBPAs.value,
            onCheckedChange = { newValue ->
                showBPAs.value = newValue
                permissionsHelper.setBooleanPref(
                    permissionsHelper.context.PREF_SHOW_BASE_PAY_ADJUSTS,
                    newValue
                )
            },
            colors = switchColors()
        )
    }

    @Composable
    fun authPrefHeader() {
        Text(
            text = stringResource(id = R.string.pref_title_require_authentication),
            fontWeight = FontWeight.Bold
        )
        FillSpacer()

        val prefAuthEnabled =
            sharedPrefs?.getBoolean(permissionsHelper.context.AUTHENTICATION_ENABLED, true) ?: true
        val authEnabled = remember { mutableStateOf(prefAuthEnabled) }

        Switch(
            checked = authEnabled.value,
            onCheckedChange = { newValue ->
                authEnabled.value = newValue
                permissionsHelper.setBooleanPref(
                    permissionsHelper.context.AUTHENTICATION_ENABLED,
                    newValue
                )
            },
            colors = switchColors()
        )
    }

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
            SettingsCard(
                headerContent = { themePrefHeader() }
            )

            WideSpacer()

            SettingsCard(
                headerContent = { bpaPrefHeader() }
            ) {
                Text(
                    text = stringResource(R.string.show_weekly_bpa_desc),
                    fontSize = fontSizeDimensionResource(id = textSizeBody),
                    fontFamily = FontFamilyFiraSans
                )
            }

            WideSpacer()

            SettingsCard(
                headerContent = { authPrefHeader() }
            ) {
                Text(
                    text = stringResource(R.string.require_pin_desc),
                    fontSize = fontSizeDimensionResource(id = textSizeBody),
                    fontFamily = FontFamilyFiraSans
                )
            }
        },
        navContent = {
            InitialSettingsNav(callback = activity as InitialScreenCallback?)
        }
    )
}

@Composable
fun switchColors(): SwitchColors {
    return SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.secondary,
        checkedTrackColor = MaterialTheme.colorScheme.surface,
        checkedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        uncheckedThumbColor = MaterialTheme.colorScheme.errorContainer,
        uncheckedTrackColor = MaterialTheme.colorScheme.onError,
        uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )
}

@ExperimentalTextApi
@Composable
fun SettingsCard(
    headerContent: @Composable (RowScope.() -> Unit),
    body: @Composable (ColumnScope.() -> Unit)? = null
) {
    DefaultOutlinedCard {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.onError,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = dimensionResource(id = R.dimen.min_touch_target))
                        .padding(
                            horizontal = marginDefault(),
                            vertical = body?.let { 0.dp }
                                ?: marginHalf()
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    content = headerContent
                )
            }

            body?.let {
                Column(
                    modifier = Modifier
                        .padding(
                            vertical = marginDefault(),
                            horizontal = marginDefault()
                        )
                ) {
                    body()
                }
            }
        }
    }
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

    BottomNavButtons {
        DefaultButton(
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
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
fun ThisPreview() {
    DashTrackerTheme {
        ActivityScreen {
            InitialSettings()
        }
    }
}