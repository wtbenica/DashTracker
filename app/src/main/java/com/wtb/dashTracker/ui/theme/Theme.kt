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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController

val cardShape: RoundedCornerShape = RoundedCornerShape(24.dp)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    // template header
    primaryContainer = darkPrimaryLight,
    // template header stroke
    primary = darkPrimary,
    // header icon color
    inversePrimary = darkPrimaryFaded,
    // secondary container
    tertiaryContainer = darkPrimaryDark,
    // card outline
    secondary = darkSecondaryLight,
    // content card icon
    secondaryContainer = darkSecondaryFaded,
    // accent
    tertiary = darkAccent,

    onPrimary = darkOnPrimary,
    background = darkOnSecondary,
    onTertiary = darkOnPrimary,
    onPrimaryContainer = darkOnPrimaryVariant,
    onSecondaryContainer = darkOnSecondaryVariant,
    onTertiaryContainer = darkSecondary,
    surface = darkOnSecondary,
    onSurface = darkOnPrimary,
    surfaceVariant = darkOnSecondaryVariant,

    // switch thumb
    onSecondary = darkSecondaryDark,
    //switch track
    onBackground = darkSecondaryFaded,
    // switch thumb disabled
    outline = darkSecondary,

)

private val LightColorScheme: ColorScheme = lightColorScheme(
    // template header
    primaryContainer = primaryLight,
    // template header stroke
    primary = primary,
    // header icon color
    inversePrimary = primaryDark,
    // secondary container
    tertiaryContainer = primaryFaded,
    // card outline
    secondary = secondaryLight,
    // content card icon
    secondaryContainer = secondaryDark,
    // accent
    tertiary = accent,

    onPrimary = onPrimary,
    background = onSecondary,
    onTertiary = onPrimary,
    onPrimaryContainer = onPrimaryVariant,
    onSecondaryContainer = onSecondaryVariant,
    onTertiaryContainer = secondaryFaded,
    surface = onSecondary,
    onSurface = onPrimary,
    surfaceVariant = onSecondaryVariant,

    // switch thumb
    onSecondary = secondaryDark,
    // switch track
    onBackground = secondaryFaded,
    // switch thumb disabled
    outline = secondary,
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
        systemUiController.setStatusBarColor(
            color = colorScheme.primary,
            darkIcons = !darkTheme
        )
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}