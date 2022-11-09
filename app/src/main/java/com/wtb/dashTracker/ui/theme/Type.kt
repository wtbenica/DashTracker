package com.wtb.dashTracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.R

@ExperimentalTextApi
val provider: GoogleFont.Provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
//
//@ExperimentalTextApi
//val FontLalezar = GoogleFont(name = "Lalezar")
//
//@ExperimentalTextApi
//val FontFamilyLalezar = FontFamily(
//    Font(FontLalezar, provider)
//)

@ExperimentalTextApi
val FontOswald: GoogleFont = GoogleFont(name = "Oswald")

@ExperimentalTextApi
val FontFamilyOswald: FontFamily = FontFamily(
    Font(FontOswald, provider)
)

@ExperimentalTextApi
val FontNotoSans: GoogleFont = GoogleFont(name = "Fira Sans")

@ExperimentalTextApi
val FontFamilyFiraSans: FontFamily = FontFamily(
    Font(FontNotoSans, provider)
)

// Set of Material typography styles to start with
@ExperimentalTextApi
val Typography: Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamilyOswald,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        baselineShift = BaselineShift(.15f)
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamilyOswald,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)