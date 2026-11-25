package com.mayank.questlist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mayank.questlist.R

val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron, FontWeight.Bold),
    Font(R.font.orbitron, FontWeight.Normal)
)

val RajdhaniFontFamily = FontFamily(
    Font(R.font.rajdhani, FontWeight.Normal),
    Font(R.font.rajdhani, FontWeight.Medium),
    Font(R.font.rajdhani, FontWeight.Bold)
)

val TextGlowShadow = Shadow(
    color = MainGlow,
    blurRadius = 12f
)

val SystemTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 3.sp,
        color = TextBlueWhite,
        shadow = TextGlowShadow
    ),
    titleLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 2.5.sp,
        color = TextBlueWhite,
        shadow = TextGlowShadow
    ),
    titleMedium = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 2.sp,
        color = TextBlueWhite,
        shadow = TextGlowShadow
    ),
    bodyLarge = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = TextBlueWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = TextBlueWhite
    ),
    labelLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.5.sp,
        color = BrightAccent
    )
)