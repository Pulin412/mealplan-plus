package com.mealplanplus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Build an M3 colour scheme from a semantic [AppColors] palette. */
private fun AppColors.toMaterialScheme() =
    if (isDark)
        darkColorScheme(
            primary = accent, onPrimary = onAccent,
            background = appBg, onBackground = ink,
            surface = surface, onSurface = ink,
            error = danger,
        )
    else
        lightColorScheme(
            primary = accent, onPrimary = onAccent,
            background = appBg, onBackground = ink,
            surface = surface, onSurface = ink,
            error = danger,
        )

@Composable
fun MealPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    // Scale every sp in the app up 10% (dp spacing unchanged) so text reads larger everywhere,
    // on top of whatever accessibility font scale the user has set.
    val d = LocalDensity.current
    val scaledDensity = Density(d.density, d.fontScale * 1.10f)
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography  = Typography,
            content     = content,
        )
    }
}
