package com.mealplanplus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Minimalist light scheme — near-black primary, #F7F7F7 background, white cards
private val LightColorScheme = lightColorScheme(
    primary              = AppOnSurface,          // #111
    onPrimary            = Color.White,
    primaryContainer     = TagGreyBg,             // #F0F0F0
    onPrimaryContainer   = AppOnSurface,
    secondary            = StatusSuccess,          // #2E7D52
    onSecondary          = Color.White,
    secondaryContainer   = TagGreenBg,            // #E8F5EE
    onSecondaryContainer = TagGreenText,
    tertiary             = AIAccent,              // #7C3AED
    onTertiary           = Color.White,
    tertiaryContainer    = TagPurpleBg,           // #F3EEFF
    onTertiaryContainer  = TagPurpleText,
    error                = StatusError,
    onError              = Color.White,
    errorContainer       = Color(0xFFFFE8E8),
    onErrorContainer     = StatusError,
    background           = AppBackground,         // #F7F7F7
    onBackground         = AppOnSurface,
    surface              = AppSurface,            // #FFFFFF
    onSurface            = AppOnSurface,
    surfaceVariant       = Color(0xFFF5F5F5),
    onSurfaceVariant     = AppMuted,              // #888
    outline              = AppBorder,             // #DEDEDE
    outlineVariant       = AppDivider             // #EBEBEB
)

private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFFE8E8E8),
    onPrimary            = Color(0xFF1A1A1A),
    primaryContainer     = Color(0xFF2A2A2A),
    onPrimaryContainer   = Color(0xFFE8E8E8),
    secondary            = Color(0xFF4CAF80),
    onSecondary          = Color(0xFF003320),
    secondaryContainer   = Color(0xFF1A3D2A),
    onSecondaryContainer = Color(0xFF4CAF80),
    tertiary             = Color(0xFF9B72CF),
    onTertiary           = Color(0xFF1A0A2A),
    tertiaryContainer    = Color(0xFF2A1A3A),
    onTertiaryContainer  = Color(0xFF9B72CF),
    error                = Red80,
    onError              = Red20,
    errorContainer       = Red30,
    onErrorContainer     = Red90,
    background           = Color(0xFF121212),
    onBackground         = Color(0xFFE8E8E8),
    surface              = Color(0xFF1E1E1E),
    onSurface            = Color(0xFFE8E8E8),
    surfaceVariant       = Color(0xFF2A2A2A),
    onSurfaceVariant     = Color(0xFFAAAAAA),
    outline              = Color(0xFF444444),
    outlineVariant       = Color(0xFF333333)
)

@Composable
fun MealPlanPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — it would override the minimalist design palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent status bar — content draws behind it
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}
