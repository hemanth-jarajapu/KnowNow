package com.passion.knownow.ui.theme

import androidx.compose.ui.graphics.Color

// Expressive app palette: violet, coral, mint.
val Violet = Color(0xFF6C4DFF)
val VioletDark = Color(0xFFC9BFFF)
val Coral = Color(0xFFFF6B5A)
val Mint = Color(0xFF00C2A8)

val LightColors = androidx.compose.material3.lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DDFF),
    onPrimaryContainer = Color(0xFF1D0060),
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = Color(0xFF410001),
    tertiary = Mint,
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFFB8F5E9),
    onTertiaryContainer = Color(0xFF00201A),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

val DarkColors = androidx.compose.material3.darkColorScheme(
    primary = VioletDark,
    onPrimary = Color(0xFF32009A),
    primaryContainer = Color(0xFF4B31C9),
    onPrimaryContainer = Color(0xFFE4DDFF),
    secondary = Color(0xFFFFB4A8),
    onSecondary = Color(0xFF690100),
    secondaryContainer = Color(0xFF930100),
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = Color(0xFF5FE8D2),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF005046),
    onTertiaryContainer = Color(0xFFB8F5E9),
    background = Color(0xFF131218),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF131218),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

// Reader palettes — deliberately separate from the expressive shell.
data class ReaderPalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val isDark: Boolean,
)

val PaperPalette = ReaderPalette(
    background = Color(0xFFF7F2E7),
    surface = Color(0xFFFFFCF5),
    text = Color(0xFF221F1A),
    muted = Color(0xFF6E675C),
    accent = Color(0xFF6C4DFF),
    isDark = false,
)

val SepiaPalette = ReaderPalette(
    background = Color(0xFFF2E6CF),
    surface = Color(0xFFFBF0DC),
    text = Color(0xFF3A2E1F),
    muted = Color(0xFF7A6A52),
    accent = Color(0xFF8A5A2B),
    isDark = false,
)

val NightPalette = ReaderPalette(
    background = Color(0xFF121212),
    surface = Color(0xFF1B1B1B),
    text = Color(0xFFE6E1D7),
    muted = Color(0xFFA39C8F),
    accent = Color(0xFFC9BFFF),
    isDark = true,
)

val ExpressiveReaderPalette = ReaderPalette(
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF1B1B1F),
    muted = Color(0xFF49454F),
    accent = Violet,
    isDark = false,
)
