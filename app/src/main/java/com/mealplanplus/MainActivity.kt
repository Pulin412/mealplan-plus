package com.mealplanplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mealplanplus.ui.theme.MealPlanPlusTheme
import com.mealplanplus.ui.navigation.MealPlanNavHost
import com.mealplanplus.util.ThemePreferences
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val followSystem by ThemePreferences.isFollowSystem(this).collectAsState(initial = true)
            val darkModePref by ThemePreferences.isDarkMode(this).collectAsState(initial = false)
            val dynamicColor by ThemePreferences.isDynamicColor(this).collectAsState(initial = true)

            val darkTheme = if (followSystem) isSystemInDarkTheme() else darkModePref

            MealPlanPlusTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MealPlanNavHost()
                }
            }
        }
    }
}
