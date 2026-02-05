package com.mealplanplus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mealplanplus.ui.screens.home.HomeScreen
import com.mealplanplus.ui.screens.foods.FoodsScreen
import com.mealplanplus.ui.screens.foods.AddFoodScreen
import com.mealplanplus.ui.screens.meals.MealsScreen
import com.mealplanplus.ui.screens.meals.AddMealScreen
import com.mealplanplus.ui.screens.diets.DietsScreen
import com.mealplanplus.ui.screens.diets.AddDietScreen
import com.mealplanplus.ui.screens.log.DailyLogScreen
import com.mealplanplus.ui.screens.calendar.CalendarScreen
import com.mealplanplus.ui.screens.health.HealthScreen
import com.mealplanplus.ui.screens.charts.ChartsScreen
import com.mealplanplus.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Foods : Screen("foods")
    object AddFood : Screen("add_food")
    object Meals : Screen("meals")
    object AddMeal : Screen("add_meal")
    object Diets : Screen("diets")
    object AddDiet : Screen("add_diet")
    object DailyLog : Screen("daily_log")
    object DailyLogWithDate : Screen("daily_log/{date}") {
        fun createRoute(date: String) = "daily_log/$date"
    }
    object Calendar : Screen("calendar")
    object Health : Screen("health")
    object Charts : Screen("charts")
    object Settings : Screen("settings")
}

@Composable
fun MealPlanNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToFoods = { navController.navigate(Screen.Foods.route) },
                onNavigateToMeals = { navController.navigate(Screen.Meals.route) },
                onNavigateToDiets = { navController.navigate(Screen.Diets.route) },
                onNavigateToLog = { navController.navigate(Screen.DailyLog.route) },
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onNavigateToHealth = { navController.navigate(Screen.Health.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Foods.route) {
            FoodsScreen(
                onNavigateToAddFood = { navController.navigate(Screen.AddFood.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddFood.route) {
            AddFoodScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Meals.route) {
            MealsScreen(
                onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                onNavigateToMealDetail = { /* TODO */ },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddMeal.route) {
            AddMealScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Diets.route) {
            DietsScreen(
                onNavigateToAddDiet = { navController.navigate(Screen.AddDiet.route) },
                onNavigateToDietDetail = { /* TODO */ },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddDiet.route) {
            AddDietScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DailyLog.route) {
            DailyLogScreen(
                date = null,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFoods = { navController.navigate(Screen.Foods.route) }
            )
        }
        composable(
            route = Screen.DailyLogWithDate.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            DailyLogScreen(
                date = date,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFoods = { navController.navigate(Screen.Foods.route) }
            )
        }
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLog = { date ->
                    navController.navigate(Screen.DailyLogWithDate.createRoute(date))
                }
            )
        }
        composable(Screen.Health.route) {
            HealthScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCharts = { navController.navigate(Screen.Charts.route) }
            )
        }
        composable(Screen.Charts.route) {
            ChartsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
