package com.mealplanplus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary   = Teal,
    background = AppBg,
    surface    = AppBg,
    onPrimary  = androidx.compose.ui.graphics.Color.White,
    onBackground = Ink,
    onSurface    = Ink,
    error = Danger
)

@Composable
fun MealPlanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography  = Typography,
        content     = content
    )
}
