package com.passion.knownow.ui.theme

import androidx.compose.ui.graphics.Color

val BrandPrimary = Color(0xFF3F51B5)
val BrandPrimaryContainer = Color(0xFFDCE0FF)
val BrandOnPrimary = Color(0xFFFFFFFF)
val BrandOnPrimaryContainer = Color(0xFF00105C)
val BrandSecondary = Color(0xFF00BCD4)
val BrandSecondaryContainer = Color(0xFFB2EBF2)
val BrandOnSecondary = Color(0xFFFFFFFF)
val BrandOnSecondaryContainer = Color(0xFF00363D)

val LightBackground = Color(0xFFFDFBFF)
val LightOnBackground = Color(0xFF1B1B1F)
val LightSurfaceVariant = Color(0xFFE3E1EC)
val LightOnSurfaceVariant = Color(0xFF46464F)
val LightOutline = Color(0xFF777680)
val LightOutlineVariant = Color(0xFFC7C5D0)
val LightError = Color(0xFFBA1A1A)

val DarkBackground = Color(0xFF121318)
val DarkOnBackground = Color(0xFFE4E1E9)
val DarkSurfaceVariant = Color(0xFF46464F)
val DarkOnSurfaceVariant = Color(0xFFC7C5D0)
val DarkOutline = Color(0xFF918F9A)
val DarkOutlineVariant = Color(0xFF46464F)
val DarkError = Color(0xFFFFB4AB)

val LightColors = androidx.compose.material3.lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = BrandOnSecondary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
)

val DarkColors = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF00208E),
    primaryContainer = Color(0xFF24389C),
    onPrimaryContainer = Color(0xFFDCE0FF),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFFB2EBF2),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
)