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

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

val cardShape: RoundedCornerShape = RoundedCornerShape(24.dp)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = darkPrimary,
    onPrimary = darkOnPrimary,
    primaryContainer = darkPrimary,
    onPrimaryContainer = darkOnPrimary,
    inversePrimary = darkPrimaryLight,
    secondary = darkPrimaryFaded,
    onSecondary = darkOnSecondary,
    secondaryContainer = darkPrimaryLight,
    onSecondaryContainer = darkOnPrimary,
    tertiary = darkPrimaryLight,
    onTertiary = darkOnPrimary,
    tertiaryContainer = darkPrimaryDark,
    onTertiaryContainer = darkPrimaryFaded,
    surface = darkOnSecondary,
    onSurface = darkOnPrimary,
    background = Color.Black,
    onBackground = darkOnPrimary,
    outline = darkPrimaryDark,
)

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryLight,
    onPrimaryContainer = onPrimary,
    inversePrimary = primaryDark,
    secondary = primaryDark,
    onSecondary = onSecondary,
    secondaryContainer = primaryFaded,
    onSecondaryContainer = onSecondary,
    tertiary = primaryFaded,
    onTertiary = onPrimary,
    tertiaryContainer = primary,
    onTertiaryContainer = primaryDark,
    surface = onSecondary,
    onSurface = onPrimary,
    background = Color.White,
    onBackground = onPrimary,
    outline = primaryDark,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val theWindow = (view.context as Activity).window
            theWindow.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(theWindow, view).isAppearanceLightStatusBars =
                darkTheme
        }
    }

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