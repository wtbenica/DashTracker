package com.wtb.dashTracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.wtb.dashTracker.R

val primary: Color = Color(0xFFE0F8C0)
val primaryDark: Color = Color(0xFF90C860)
val primaryLight: Color = Color(0xFFE8FFD0)
val primaryFaded: Color = Color(0xFFF8FFF0)
val onPrimary: Color = Color(0xFF353539)
val onSecondary: Color = Color(0xFFFEFEFE)

val darkPrimary: Color = Color(0xFF283028)
val darkPrimaryDark: Color = Color(0xFF182018)
val darkPrimaryLight: Color = Color(0xFF384038)
val darkPrimaryFaded: Color = Color(0xFFa0a080)
val darkOnPrimary: Color = Color(0xFFE8E8E8)
val darkOnSecondary: Color = Color(0xFF17171a)

val up: Color = Color(0xFF90C860)
val down: Color = Color(0xFFF0A0B0)
val car: Color = Color(0xFF80D0E0)

@Composable
fun secondary(): Color = colorResource(id = R.color.brick)

@Composable
fun secondaryDark(): Color = colorResource(id = R.color.brick_dark)

@Composable
fun secondaryLight(): Color = colorResource(id = R.color.brick_light)

@Composable
fun secondaryFaded(): Color = colorResource(id = R.color.brick_faded)

@Composable
fun primaryDark(): Color = colorResource(id = R.color.dark)

@Composable
fun primaryFaded(): Color = colorResource(id = R.color.faded)

@Composable
fun onPrimaryVariant(): Color = colorResource(id = R.color.on_regular_light)


@Composable
fun accent(): Color = colorResource(id = R.color.accent)

@Composable
fun welcomeIconColor(): Color = if (isSystemInDarkTheme()) {
    accent()
} else {
    car
}