package com.wtb.dashTracker.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.wtb.dashTracker.R
import com.wtb.dashTracker.util.PermissionsHelper

val primaryDark: Color = Color(0xFF90C060)
val primary: Color = Color(0xFFD4FB99)
val primaryLight: Color = Color(0xFFE4FFB9)
val primaryFaded: Color = Color(0xFFF0FFE8)
val onPrimary: Color = Color(0xFF353539)

val onSecondary: Color = Color(0xFFFEFEFE)


val secondaryFaded: Color = Color(0xFFF8FDFF)

val darkPrimaryDark: Color = Color(0xFF1B201B)
val darkPrimary: Color = Color(0xFF283028)
val darkPrimaryLight: Color = Color(0xFF384038)
val darkPrimaryFaded: Color = Color(0xFFA0A080)
val darkOnPrimary: Color = Color(0xFFE8E8E8)
val darkOnPrimaryVariant: Color = Color(0xFFCDCDCD)
val darkOnSecondary: Color = Color(0xFF101014)
val darkOnSecondaryVariant: Color = Color(0xFF202024)

val up: Color = Color(0xFF90C860)
val down: Color = Color(0xFFF0A0B0)
val car: Color = Color(0xFF99CCEE)

@Composable
fun iconColor(context: Context?): Color =
    context?.let {
        getDayNightColor(context, accent(), secondaryDark())
    } ?: secondary()

@Composable
fun rippleColor(context: Context?): Color =
    context?.let {
        getDayNightColor(context, accent(), secondaryFaded())
    } ?: secondaryLight()

fun getDayNightColor(context: Context, nightColor: Color, dayColor: Color): Color {
    val mode = PermissionsHelper(context).uiModeIsDarkMode

    return if (mode) {
        nightColor
    } else {
        dayColor
    }
}

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
fun onPrimary(): Color = colorResource(id = R.color.on_regular)

@Composable
fun onPrimaryVariant(): Color = colorResource(id = R.color.on_regular_light)

@Composable
fun onSecondaryVariant(): Color = colorResource(id = R.color.on_brick_light)

@Composable
fun accent(): Color = colorResource(id = R.color.accent)

@Composable
fun welcomeIconColor(): Color = car