package com.wtb.dashTracker.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.wtb.dashTracker.R
import com.wtb.dashTracker.util.PermissionsHelper

val primary: Color = Color(0xFFE0F8C0)
val primaryDark: Color = Color(0xFF90C860)
val primaryLight: Color = Color(0xFFE8FFD0)
val primaryFaded: Color = Color(0xFFF8FFF0)
val onPrimary: Color = Color(0xFF353539)
val onPrimaryVariant: Color = Color(0xFF686879)
val onSecondary: Color = Color(0xFFFEFEFE)
val onSecondaryVariant: Color = Color(0xFFEDEDED)

val secondaryFaded: Color = Color(0xFFF8FDFF)

val darkPrimary: Color = Color(0xFF283028)
val darkPrimaryDark: Color = Color(0xFF1b201b)
val darkPrimaryLight: Color = Color(0xFF384038)
val darkPrimaryFaded: Color = Color(0xFFa0a080)
val darkOnPrimary: Color = Color(0xFFE8E8E8)
val darkOnPrimaryVariant: Color = Color(0xFFCDCDCD)
val darkOnSecondary: Color = Color(0xFF101014)
val darkOnSecondaryVariant: Color = Color(0xFF202024)

val up: Color = Color(0xFF90C860)
val down: Color = Color(0xFFF0A0B0)
val car: Color = Color(0xFF99CCEE)

fun getDayNightColor(context: Context, nightColor: Color, dayColor: Color): Color {
    val mode = PermissionsHelper(context).uiModeIsDarkMode

    return if (mode) {
        nightColor
    } else {
        dayColor
    }
}

fun accent(context: Context): Color =
    getDayNightColor(context, Color(0xFF205060), Color(0xFFE0F0FF))

fun accentDark(context: Context): Color =
    getDayNightColor(context, Color(0xFF103038), Color(0xFF99CCEE))

fun accentFaded(context: Context): Color =
    getDayNightColor(context, Color(0xFF486878), Color(0xFFF8FDFF))

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