package com.passion.knownow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.passion.knownow.R

private fun variableFont(resId: Int, weight: Int): Font = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(
    variableFont(R.font.manrope, 300),
    variableFont(R.font.manrope, 400),
    variableFont(R.font.manrope, 500),
    variableFont(R.font.manrope, 600),
    variableFont(R.font.manrope, 700),
    variableFont(R.font.manrope, 800),
)

val Newsreader = FontFamily(
    variableFont(R.font.newsreader, 400),
    variableFont(R.font.newsreader, 500),
    variableFont(R.font.newsreader, 600),
    variableFont(R.font.newsreader, 700),
)

val JetBrainsMono = FontFamily(
    variableFont(R.font.jetbrainsmono, 400),
    variableFont(R.font.jetbrainsmono, 500),
    variableFont(R.font.jetbrainsmono, 700),
)

val KnowNowTypography = Typography(
    displayLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.8).sp),
    displaySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.4).sp),
    headlineLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

fun readerTypography(scale: Float): Typography = Typography(
    displayLarge = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Bold, fontSize = (46 * scale).sp, lineHeight = (54 * scale).sp),
    headlineLarge = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Bold, fontSize = (30 * scale).sp, lineHeight = (38 * scale).sp),
    headlineMedium = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.SemiBold, fontSize = (25 * scale).sp, lineHeight = (32 * scale).sp),
    headlineSmall = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.SemiBold, fontSize = (21 * scale).sp, lineHeight = (28 * scale).sp),
    titleLarge = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.SemiBold, fontSize = (19 * scale).sp, lineHeight = (26 * scale).sp),
    titleMedium = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = (17 * scale).sp, lineHeight = (24 * scale).sp),
    bodyLarge = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Normal, fontSize = (19 * scale).sp, lineHeight = (31 * scale).sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Normal, fontSize = (17 * scale).sp, lineHeight = (27 * scale).sp),
    bodySmall = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Normal, fontSize = (15 * scale).sp, lineHeight = (22 * scale).sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)
