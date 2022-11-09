package com.wtb.dashTracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.wtb.dashTracker.R

val primary: Color = Color(0xFFB0F0A0)
val primaryDark: Color = Color(0xFF70B850)
val primaryLight: Color = Color(0xFFD0F8C0)
val primaryFaded: Color = Color(0xFFF0FFE0)
val onPrimary: Color = Color(0xFF454556)
val onSecondary: Color = Color(0xFFF8F8FF)

@Composable
fun primaryLight(): Color = colorResource(id = R.color.light)

@Composable
fun secondary(): Color = colorResource(id = R.color.brick)

@Composable
fun secondaryLight(): Color = colorResource(id = R.color.brick_light)

@Composable
fun secondaryDark(): Color = colorResource(id = R.color.brick_dark)

@Composable
fun secondaryFaded(): Color = colorResource(id = R.color.brick_faded)


val up: Color = Color(0xFF80F090)
val down: Color = Color(0xFFF0A0B0)
val car: Color = Color(0xFF60E0FF)
//val down = Color(0xFFC85070)
//val car = Color(0xFF3090C8)

val darkPrimary: Color = Color(0xFF405040)
val darkPrimaryDark: Color = Color(0xFF304030)
val darkPrimaryLight: Color = Color(0xFF607060)
val darkPrimaryFaded: Color = Color(0xFF809080)
val darkOnPrimary: Color = Color(0xFFF7F7FF)
