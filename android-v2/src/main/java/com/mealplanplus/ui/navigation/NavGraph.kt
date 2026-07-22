package com.mealplanplus.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mealplanplus.ui.screens.auth.AuthScreen
import com.mealplanplus.ui.screens.auth.AuthViewModel
import com.mealplanplus.ui.screens.auth.ForgotPasswordScreen
import com.mealplanplus.ui.screens.diets.DietsScreen
import com.mealplanplus.ui.screens.exercises.ExercisesScreen
import com.mealplanplus.ui.screens.foods.FoodsScreen
import com.mealplanplus.ui.screens.health.HealthScreen
import com.mealplanplus.ui.screens.meals.MealsScreen
import com.mealplanplus.ui.screens.home.HomeScreen
import com.mealplanplus.ui.screens.plan.PlanScreen
import com.mealplanplus.ui.screens.profile.ProfileScreen

sealed class Screen(val route: String, val label: String) {
    object Today     : Screen("today",     "Today")
    object Plan      : Screen("plan",      "Plan")
    object Exercises : Screen("exercises", "Exercises")
    object Health    : Screen("health",    "Health")
    object Foods     : Screen("foods",     "Foods")
    object Meals     : Screen("meals",     "Meals")
    object Diets     : Screen("diets",     "Diets")
    object Profile   : Screen("profile",   "Profile")
}

private val bottomNavItems = listOf(
    Screen.Today     to Icons.Default.Home,
    Screen.Plan      to Icons.Default.CalendarMonth,
    Screen.Exercises to Icons.Default.FitnessCenter,
    Screen.Health    to Icons.Default.MonitorHeart,
)

/**
 * Top-level gate: unauthenticated users see the auth flow; signed-in users get the app.
 * The switch is automatic — [AuthViewModel.authState] flips on sign-in/out.
 */
@Composable
fun AppRoot() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val user by authViewModel.authState.collectAsState()
    if (user == null) AuthNavHost(authViewModel) else MealPlanNavHost()
}

@Composable
private fun AuthNavHost(vm: AuthViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "auth") {
        composable("auth")   { AuthScreen(vm, onForgotPassword = { nav.navigate("forgot") }) }
        composable("forgot") { ForgotPasswordScreen(vm, onBack = { nav.popBackStack() }) }
    }
}

@Composable
fun MealPlanNavHost() {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentDest   = navBackStack?.destination

    // Persistent bottom nav on every in-app screen (auth flow is a separate NavHost).
    val showBottomBar = true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { (screen, icon) ->
                        NavigationBarItem(
                            selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                            onClick  = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            // TEMP (no bottom nav yet): launch on Today; ☰/back cycle Today → Meals → Diets → Foods → Today.
            startDestination = Screen.Today.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route)     {
                HomeScreen(onMenu = { navController.navigate(Screen.Meals.route) },
                    onProfile = { navController.navigate(Screen.Profile.route) })
            }
            composable(Screen.Profile.route)   { ProfileScreen(onBack = { navController.navigate(Screen.Today.route) }) }
            composable(Screen.Plan.route)      { PlanScreen() }
            composable(Screen.Exercises.route) { ExercisesScreen() }
            composable(Screen.Health.route)    { HealthScreen() }
            composable(Screen.Meals.route)     {
                MealsScreen(onBack = { navController.navigate(Screen.Diets.route) })
            }
            composable(Screen.Diets.route)     {
                DietsScreen(onBack = { navController.navigate(Screen.Foods.route) })
            }
            composable(Screen.Foods.route)     {
                FoodsScreen(onBack = { navController.navigate(Screen.Today.route) })
            }
        }
    }
}
