package com.mealplanplus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography  = Typography,
            content     = content,
        )
    }
}
