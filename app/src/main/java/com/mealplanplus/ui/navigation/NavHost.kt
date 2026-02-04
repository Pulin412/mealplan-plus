package com.mealplanplus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mealplanplus.ui.screens.home.HomeScreen
import com.mealplanplus.ui.screens.foods.FoodsScreen
import com.mealplanplus.ui.screens.foods.AddFoodScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Foods : Screen("foods")
    object AddFood : Screen("add_food")
}

@Composable
fun MealPlanNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToFoods = { navController.navigate(Screen.Foods.route) }
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
    }
}
