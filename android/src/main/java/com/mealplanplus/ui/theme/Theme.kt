package com.mealplanplus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

/** Provides the resolved dark-mode state (accounts for manual override via ThemePreferences). */
val LocalIsDarkTheme = compositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5EE),
    onPrimaryContainer = Color(0xFF1A3D2A),
    secondary = Color(0xFF555555),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F0F0),
    onSecondaryContainer = Color(0xFF111111),
    tertiary = Orange40,
    onTertiary = Color.White,
    tertiaryContainer = Orange90,
    onTertiaryContainer = Orange10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF888888),
    outline = Color(0xFFDEDEDE),
    outlineVariant = Color(0xFFCCCCCC),
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Green80,
    onSecondary = Green20,
    secondaryContainer = Green30,
    onSecondaryContainer = Green90,
    tertiary = Orange80,
    onTertiary = Orange20,
    tertiaryContainer = Orange30,
    onTertiaryContainer = Orange90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Grey10,
    onBackground = Grey90,
    surface = Grey10,
    onSurface = Grey90,
    surfaceVariant = GreyVariant30,
    onSurfaceVariant = GreyVariant80,
    outline = GreyVariant60,
    outlineVariant = GreyVariant30
)

@Composable
fun MealPlanPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalDensity provides Density(baseDensity.density, fontScale = baseDensity.fontScale * 1.1f)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
