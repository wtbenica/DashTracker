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

package com.wtb.dashTracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController

val cardShape: RoundedCornerShape = RoundedCornerShape(24.dp)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = darkPrimary, // header,
    onPrimary = darkOnPrimary,
    primaryContainer = darkPrimaryLight, // buttons,
    onPrimaryContainer = darkOnPrimaryVariant,
    inversePrimary = darkPrimaryFaded, // header icon,

    secondary = darkSecondaryFaded, // card outline
    onSecondary = darkSecondary, // DropDownMenu background
    secondaryContainer = darkAccent, // content card icon
    onSecondaryContainer = darkOnSecondaryVariant,

    tertiary = darkAccent, // accent
    onTertiary = darkOnPrimary,
    tertiaryContainer = darkPrimaryDark, // secondary container, bottom content
    onTertiaryContainer = darkSecondary, // expandable card ripple,

    background = darkOnSecondary,
    onBackground = darkSecondaryFaded, //switch track checked / unchecked

    surface = darkOnSecondary,
    onSurface = darkOnPrimary,
    surfaceVariant = darkOnSecondaryVariant, // DropDownMenuBox background
    onSurfaceVariant = Color.Magenta,
    surfaceTint = Color.Yellow,
    inverseSurface = darkPrimary, // Secondary Card bg
    inverseOnSurface = darkPrimaryLight, // Secondary Card stroke
    error = Color.Cyan,
    onError = darkSecondaryLight, // settingsCard top panel background
    errorContainer = darkSecondary, // switch thumb unchecked
    onErrorContainer = Color.Red,
    outline = darkSecondaryDark, // switch thumb checked, custom outline card outline, switch border checked/unchecked
    outlineVariant = Color.Green,
    scrim = Color.Blue
)


private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = primary, // header
    onPrimary = onPrimary,
    primaryContainer = primaryLight, // buttons
    onPrimaryContainer = onPrimaryVariant, // header subtitle, disabled text,
    inversePrimary = primaryDark, // header icon, text buttons,

    secondary = secondary, // switch thumb checked
    onSecondary = onSecondary, //
    secondaryContainer = secondaryDark, // content card icon, permission granted icon
    onSecondaryContainer = onSecondaryVariant,

    tertiary = accent, // accent, list row icon, privacy policy link, permissions row icon,
    onTertiary = onPrimary,
    tertiaryContainer = primaryFaded, // secondary container
    onTertiaryContainer = secondaryFaded,

    background = Color.Cyan,
    onBackground = secondaryFaded, //switch track unchecked

    surface = onSecondary, // header icon bg, main bg, card bg, switch track checked
    onSurface = onPrimary,
    surfaceVariant = onSecondary, // DropDownMenuBox background (can't change)
    onSurfaceVariant = onPrimary, // DropDown arrow (?can't change?)
    surfaceTint = secondaryFaded, // expandable card ripple
    inverseSurface = primaryFaded, // Secondary Card bg
    inverseOnSurface = primary, // Secondary Card stroke, outline button outline

//    error = DEFAULT,

    // settingsCard top panel background, switch track unchecked, dropdown menu open indicator
    onError = secondaryFaded,
    errorContainer = secondary, // switch thumb unchecked
    onErrorContainer = Color.Red,

    // custom outline card outline, switch border checked/unchecked
    outline = secondary,
    outlineVariant = secondaryLight, // card outline, switch border
    scrim = Color.Blue
)

@ExperimentalTextApi
@Composable
fun DashTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // sets status bar text to light/dark
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.apply {
            setStatusBarColor(
                color = colorScheme.primary,
                darkIcons = !darkTheme
            )
            setNavigationBarColor(
                color = colorScheme.tertiaryContainer,
                darkIcons = !darkTheme
            )
        }
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}