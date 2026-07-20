package com.mealplanplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mealplanplus.ui.navigation.AppRoot
import com.mealplanplus.ui.theme.MealPlanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Branded cold-start splash (shows the logo instead of the default launcher icon).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MealPlanTheme {
                AppRoot()
            }
        }
    }
}
