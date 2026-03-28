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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mealplanplus.ui.theme.MealPlanPlusTheme
import com.mealplanplus.ui.navigation.MealPlanNavHost
import com.mealplanplus.util.ThemePreferences
import com.mealplanplus.widget.EXTRA_DATE
import com.mealplanplus.widget.EXTRA_DIET_ID
import com.mealplanplus.widget.EXTRA_NAVIGATE_TO
import com.mealplanplus.widget.WidgetDeepLink
import com.mealplanplus.widget.WidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Reactive deep-link state.  Using [mutableStateOf] instead of a plain val means
     * that when [onNewIntent] fires (app already in background), updating this field
     * triggers recomposition and the [LaunchedEffect] in NavHost re-runs for the new tap.
     *
     * Each new tap produces a fresh [WidgetDeepLink] with a new [WidgetDeepLink.id], so
     * even two consecutive taps to the same destination type both trigger navigation.
     */
    private var widgetDeepLink by mutableStateOf<WidgetDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WidgetUpdateWorker.enqueue(this)
        parseWidgetIntent(intent)           // cold launch

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
                    MealPlanNavHost(widgetDeepLink = widgetDeepLink)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseWidgetIntent(intent)           // warm launch — triggers recomposition
    }

    /** Converts raw intent extras into a [WidgetDeepLink] and stores it reactively. */
    private fun parseWidgetIntent(intent: android.content.Intent?) {
        val target = intent?.getStringExtra(EXTRA_NAVIGATE_TO) ?: return
        widgetDeepLink = WidgetDeepLink(
            target = target,
            date   = intent.getStringExtra(EXTRA_DATE),
            dietId = intent.getLongExtra(EXTRA_DIET_ID, -1L).takeIf { it != -1L }
        )
    }
}
