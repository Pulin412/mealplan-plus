package com.mealplanplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.mealplanplus.data.notifications.NotificationScheduler
import com.mealplanplus.ui.navigation.AppRoot
import com.mealplanplus.ui.theme.MealPlanTheme
import com.mealplanplus.ui.theme.ThemeStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themeStore: ThemeStore
    @Inject lateinit var notificationScheduler: NotificationScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        // Branded cold-start splash (shows the logo instead of the default launcher icon).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Re-arm reminder alarms on launch (survives app upgrades / cleared alarms).
        notificationScheduler.rescheduleAll()
        setContent {
            val mode by themeStore.mode.collectAsState()
            val dark = when (mode) {
                com.mealplanplus.ui.theme.ThemeMode.SYSTEM -> isSystemInDarkTheme()
                com.mealplanplus.ui.theme.ThemeMode.LIGHT -> false
                com.mealplanplus.ui.theme.ThemeMode.DARK -> true
            }
            // Edge-to-edge draws behind the status/nav bars, so their icons (clock, battery) must
            // match the APP theme, not the system's — otherwise light mode = white icons on white.
            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            MealPlanTheme(darkTheme = dark) {
                AppRoot()
            }
        }
    }
}
