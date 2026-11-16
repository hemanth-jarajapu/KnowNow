package com.passion.knownow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ReaderThemeMode { PAPER, SEPIA, NIGHT, EXPRESSIVE }

val LocalReaderPalette = staticCompositionLocalOf { PaperPalette }

@Composable
fun readerPalette(mode: ReaderThemeMode, systemDark: Boolean): ReaderPalette = when (mode) {
    ReaderThemeMode.PAPER -> PaperPalette
    ReaderThemeMode.SEPIA -> SepiaPalette
    ReaderThemeMode.NIGHT -> NightPalette
    ReaderThemeMode.EXPRESSIVE -> if (systemDark) NightPalette else ExpressiveReaderPalette
}

@Composable
fun KnowNowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KnowNowTypography,
        shapes = KnowNowShapes,
        content = content,
    )
}

@Composable
fun ReaderTheme(
    mode: ReaderThemeMode,
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val palette = readerPalette(mode, systemDark)
    val base = MaterialTheme.colorScheme
    val scheme = base.copy(
        background = palette.background,
        onBackground = palette.text,
        surface = palette.background,
        onSurface = palette.text,
        surfaceVariant = palette.surface,
        onSurfaceVariant = palette.muted,
        primary = palette.accent,
        outline = palette.muted.copy(alpha = 0.5f),
        outlineVariant = palette.muted.copy(alpha = 0.25f),
    )
    CompositionLocalProvider(LocalReaderPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = readerTypography(fontScale),
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
