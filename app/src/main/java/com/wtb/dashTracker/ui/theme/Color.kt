package com.wtb.dashTracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val primaryDark: Color = Color(0xFF78A040)
val primary: Color = Color(0xFFD4FB99)
val primaryLight: Color = Color(0xFFE4FFB9)
val primaryFaded: Color = Color(0xFFF0FFE0)

val onPrimary: Color = Color(0xFF353539)
val onPrimaryVariant: Color = Color(0xFF686879)

val secondaryDark: Color = Color(0xFF79ABCA)
val secondary: Color = Color(0xFF8FCFF0)
val secondaryLight: Color = Color(0xFFC0E8F5)
val secondaryFaded: Color = Color(0xFFDFF8FF)
val accent: Color = Color(0xFF196F90)

val onSecondary: Color = Color(0xFFFEFEFE)
val onSecondaryVariant: Color = Color(0xFFF7F7F7)

val darkPrimaryDark: Color = Color(0xFF1B201B)
val darkPrimary: Color = Color(0xFF283828)
val darkPrimaryLight: Color = Color(0xFF384838)
val darkPrimaryFaded: Color = Color(0xFF788868)

val darkOnPrimary: Color = Color(0xFFE8E8E8)
val darkOnPrimaryVariant: Color = Color(0xFFCDCDCD)


val darkSecondaryDark: Color = Color(0xFF07202b)
val darkSecondary: Color = Color(0xFF133443)
val darkSecondaryLight: Color = Color(0xFF234756)
val darkSecondaryFaded: Color = Color(0xFF375B6B)
val darkAccent: Color = Color(0xFFA0BBBB)

val darkOnSecondary: Color = Color(0xFF101014)
val darkOnSecondaryVariant: Color = Color(0xFF202024)

@Composable
fun headerIconColor(): Color = MaterialTheme.colorScheme.inversePrimary