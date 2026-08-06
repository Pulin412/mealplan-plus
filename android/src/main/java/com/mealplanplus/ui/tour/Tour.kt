package com.mealplanplus.ui.tour

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * One coach-mark: highlights the on-screen element registered under [key] with a tooltip.
 * [route] (when set) is the nav destination the tour switches to before showing this step, so
 * targets that live on another screen (e.g. the More list) are composed and can report bounds.
 */
data class TourStep(
    val key: String,
    val title: String,
    val body: String,
    val route: String? = null,
)

/**
 * The guided first-run tour: walk the bottom-nav map, then drill into the More screen where
 * Foods/Meals/Diets/Groceries live. Keys are matched by [Modifier.tourTarget] call sites.
 */
val TOUR_STEPS: List<TourStep> = listOf(
    TourStep("nav_today", "Today", "Your daily hub — log meals by slot and watch your calorie ring.", route = "today"),
    TourStep("nav_plan", "Plan", "Plan diets across your week and auto-build a grocery list."),
    TourStep("nav_exercises", "Exercises", "Workouts, sessions and your exercise library."),
    TourStep("nav_health", "Health", "Track glucose, weight & blood pressure with trends."),
    TourStep("nav_misc", "More", "Foods, Meals, Diets & Groceries all live under here."),
    TourStep("misc_foods", "Foods", "Your ingredient library — add foods here.", route = "misc"),
    TourStep("misc_meals", "Meals", "Combine foods into reusable meals."),
    TourStep("misc_diets", "Diets", "Build day-plan diets from your meals, then schedule them in Plan."),
    TourStep("misc_groceries", "Groceries", "Auto-generate a shopping list from your planned diets."),
    // Closing step has no target → a centered card over the dimmed screen.
    TourStep("done", "You're all set!", "Replay this tour anytime from Settings.", route = "today"),
)

/**
 * Drives the spotlight tour: which step is active, and the live bounds of every registered target.
 * Bounds are in root coordinates so the full-screen overlay (drawn above the Scaffold, covering the
 * bottom nav) shares the same space.
 */
@Stable
class TourController {
    var running by mutableStateOf(false)
        private set
    var index by mutableStateOf(0)
        private set

    /** key → element bounds in root px, kept fresh by [Modifier.tourTarget]. */
    val targets = mutableStateMapOf<String, Rect>()

    val step: TourStep? get() = if (running) TOUR_STEPS.getOrNull(index) else null
    val isLast: Boolean get() = index >= TOUR_STEPS.lastIndex

    fun start() {
        index = 0
        running = true
    }

    fun next(onFinish: () -> Unit) {
        if (isLast) finish(onFinish) else index++
    }

    fun finish(onFinish: () -> Unit) {
        running = false
        onFinish()
    }

    fun report(key: String, bounds: Rect) {
        targets[key] = bounds
    }
}

@Composable
fun rememberTourController(): TourController = remember { TourController() }

/** Available to any screen under the nav host so it can register targets without prop-drilling. */
val LocalTourController = compositionLocalOf<TourController?> { null }

/** Registers this element as the tour target [key], reporting its position to [controller]. */
fun Modifier.tourTarget(key: String, controller: TourController?): Modifier =
    if (controller == null) this
    else this.onGloballyPositioned { controller.report(key, it.boundsInRoot()) }

/** Convenience overload that reads the controller from [LocalTourController]. */
@Composable
fun Modifier.tourTarget(key: String): Modifier = tourTarget(key, LocalTourController.current)
