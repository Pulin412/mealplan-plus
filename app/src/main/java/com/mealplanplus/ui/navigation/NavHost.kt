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
import com.mealplanplus.ui.screens.meals.EditMealScreen
import com.mealplanplus.ui.screens.diets.DietsScreen
import com.mealplanplus.ui.screens.diets.AddDietScreen
import com.mealplanplus.ui.screens.diets.DietDetailScreen
import com.mealplanplus.ui.screens.log.DailyLogScreen
import com.mealplanplus.ui.screens.log.LogMealPickerScreen
import com.mealplanplus.ui.screens.calendar.CalendarScreen
import com.mealplanplus.ui.screens.health.HealthScreen
import com.mealplanplus.ui.screens.charts.ChartsScreen
import com.mealplanplus.ui.screens.settings.SettingsScreen
import com.mealplanplus.ui.screens.scanner.BarcodeScannerScreen
import com.mealplanplus.ui.screens.scanner.OnlineSearchScreen
import com.mealplanplus.ui.screens.meals.FoodPickerScreen
import com.mealplanplus.ui.screens.diets.DietMealSlotScreen
import com.mealplanplus.ui.screens.diets.DietMealPickerScreen
import com.mealplanplus.ui.screens.log.DietPickerScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Foods : Screen("foods")
    object AddFood : Screen("add_food")
    object Meals : Screen("meals")
    object AddMeal : Screen("add_meal")
    object EditMeal : Screen("edit_meal/{mealId}") {
        fun createRoute(mealId: Long) = "edit_meal/$mealId"
    }
    object FoodPicker : Screen("food_picker")
    object FoodPickerForEdit : Screen("food_picker_edit")
    object Diets : Screen("diets")
    object AddDiet : Screen("add_diet")
    object DietDetail : Screen("diet_detail/{dietId}") {
        fun createRoute(dietId: Long) = "diet_detail/$dietId"
    }
    object DietMealSlot : Screen("diet_meal_slot/{dietId}/{slotType}") {
        fun createRoute(dietId: Long, slotType: String) = "diet_meal_slot/$dietId/$slotType"
    }
    object FoodPickerForDietSlot : Screen("food_picker_diet_slot")
    object DietMealPicker : Screen("diet_meal_picker/{slotType}") {
        fun createRoute(slotType: String) = "diet_meal_picker/$slotType"
    }
    object DailyLog : Screen("daily_log")
    object DailyLogWithDate : Screen("daily_log/{date}") {
        fun createRoute(date: String) = "daily_log/$date"
    }
    object LogMealPicker : Screen("log_meal_picker/{date}/{slotType}") {
        fun createRoute(date: String, slotType: String) = "log_meal_picker/$date/$slotType"
    }
    object DietPicker : Screen("diet_picker/{date}") {
        fun createRoute(date: String) = "diet_picker/$date"
    }
    object Calendar : Screen("calendar")
    object Health : Screen("health")
    object Charts : Screen("charts")
    object Settings : Screen("settings")
    object BarcodeScanner : Screen("barcode_scanner")
    object OnlineSearch : Screen("online_search")
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
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLogWithDate = { date -> navController.navigate(Screen.DailyLogWithDate.createRoute(date)) }
            )
        }
        composable(Screen.Foods.route) {
            FoodsScreen(
                onNavigateToAddFood = { navController.navigate(Screen.AddFood.route) },
                onNavigateToScanner = { navController.navigate(Screen.BarcodeScanner.route) },
                onNavigateToOnlineSearch = { navController.navigate(Screen.OnlineSearch.route) },
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
                onNavigateToMealDetail = { mealId -> navController.navigate(Screen.EditMeal.createRoute(mealId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddMeal.route) { backStackEntry ->
            // Observe food selection results from FoodPickerScreen
            val savedStateHandle = backStackEntry.savedStateHandle

            AddMealScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFoodPicker = { navController.navigate(Screen.FoodPicker.route) },
                savedStateHandle = savedStateHandle
            )
        }
        composable(Screen.FoodPicker.route) {
            FoodPickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onFoodSelected = { food, quantity ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("selected_food_id", food.id)
                        set("selected_quantity", quantity)
                    }
                    navController.popBackStack()
                },
                onUsdaFoodSelected = { usdaFood, quantity ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("usda_food_name", usdaFood.name)
                        set("usda_food_brand", usdaFood.brand)
                        set("usda_food_calories", usdaFood.calories)
                        set("usda_food_protein", usdaFood.protein)
                        set("usda_food_carbs", usdaFood.carbs)
                        set("usda_food_fat", usdaFood.fat)
                        set("usda_food_serving_size", usdaFood.servingSize)
                        set("usda_food_serving_unit", usdaFood.servingUnit)
                        set("selected_quantity", quantity)
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Screen.EditMeal.route,
            arguments = listOf(navArgument("mealId") { type = NavType.LongType })
        ) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            EditMealScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFoodPicker = { navController.navigate(Screen.FoodPickerForEdit.route) },
                savedStateHandle = savedStateHandle
            )
        }
        composable(Screen.FoodPickerForEdit.route) {
            FoodPickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onFoodSelected = { food, quantity ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("selected_food_id", food.id)
                        set("selected_quantity", quantity)
                    }
                    navController.popBackStack()
                },
                onUsdaFoodSelected = { usdaFood, quantity ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("usda_food_name", usdaFood.name)
                        set("usda_food_brand", usdaFood.brand)
                        set("usda_food_calories", usdaFood.calories)
                        set("usda_food_protein", usdaFood.protein)
                        set("usda_food_carbs", usdaFood.carbs)
                        set("usda_food_fat", usdaFood.fat)
                        set("usda_food_serving_size", usdaFood.servingSize)
                        set("usda_food_serving_unit", usdaFood.servingUnit)
                        set("selected_quantity", quantity)
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Diets.route) {
            DietsScreen(
                onNavigateToAddDiet = { navController.navigate(Screen.AddDiet.route) },
                onNavigateToDietDetail = { dietId -> navController.navigate(Screen.DietDetail.createRoute(dietId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddDiet.route) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            AddDietScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMealPicker = { slotType ->
                    navController.navigate(Screen.DietMealPicker.createRoute(slotType))
                },
                savedStateHandle = savedStateHandle
            )
        }
        composable(
            route = Screen.DietDetail.route,
            arguments = listOf(navArgument("dietId") { type = NavType.LongType })
        ) { backStackEntry ->
            val dietId = backStackEntry.arguments?.getLong("dietId") ?: 0L
            DietDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMealSlot = { slotType ->
                    navController.navigate(Screen.DietMealSlot.createRoute(dietId, slotType))
                }
            )
        }
        composable(
            route = Screen.DietMealSlot.route,
            arguments = listOf(
                navArgument("dietId") { type = NavType.LongType },
                navArgument("slotType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            DietMealSlotScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFoodPicker = { navController.navigate(Screen.FoodPickerForDietSlot.route) },
                onNavigateToMealPicker = { slotType ->
                    navController.navigate(Screen.DietMealPicker.createRoute(slotType))
                },
                savedStateHandle = savedStateHandle
            )
        }
        composable(Screen.FoodPickerForDietSlot.route) {
            FoodPickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onFoodSelected = { food, quantity ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("selected_food_id", food.id)
                        set("selected_quantity", quantity)
                    }
                    navController.popBackStack()
                },
                onUsdaFoodSelected = { usdaFood, quantity ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("usda_food_name", usdaFood.name)
                        set("usda_food_brand", usdaFood.brand)
                        set("usda_food_calories", usdaFood.calories)
                        set("usda_food_protein", usdaFood.protein)
                        set("usda_food_carbs", usdaFood.carbs)
                        set("usda_food_fat", usdaFood.fat)
                        set("usda_food_serving_size", usdaFood.servingSize)
                        set("usda_food_serving_unit", usdaFood.servingUnit)
                        set("selected_quantity", quantity)
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Screen.DietMealPicker.route,
            arguments = listOf(navArgument("slotType") { type = NavType.StringType })
        ) { backStackEntry ->
            val slotType = backStackEntry.arguments?.getString("slotType") ?: ""
            DietMealPickerScreen(
                slotType = slotType,
                onNavigateBack = { navController.popBackStack() },
                onMealSelected = { mealId ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("selected_meal_id", mealId)
                        set("selected_slot_type", slotType)
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.DailyLog.route) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            DailyLogScreen(
                date = null,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMealPicker = { date, slotType ->
                    navController.navigate(Screen.LogMealPicker.createRoute(date, slotType))
                },
                onNavigateToDietPicker = { date ->
                    navController.navigate(Screen.DietPicker.createRoute(date))
                },
                savedStateHandle = savedStateHandle
            )
        }
        composable(
            route = Screen.DailyLogWithDate.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val savedStateHandle = backStackEntry.savedStateHandle
            DailyLogScreen(
                date = date,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMealPicker = { dateStr, slotType ->
                    navController.navigate(Screen.LogMealPicker.createRoute(dateStr, slotType))
                },
                onNavigateToDietPicker = { dateStr ->
                    navController.navigate(Screen.DietPicker.createRoute(dateStr))
                },
                savedStateHandle = savedStateHandle
            )
        }
        composable(
            route = Screen.DietPicker.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            DietPickerScreen(
                date = date,
                onNavigateBack = { navController.popBackStack() },
                onDietSelected = { dietId, selectedDate ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("selected_diet_id", dietId)
                        set("selected_date", selectedDate)
                    }
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
        composable(
            route = Screen.LogMealPicker.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("slotType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val slotType = backStackEntry.arguments?.getString("slotType") ?: ""
            LogMealPickerScreen(
                slotType = slotType,
                onNavigateBack = { navController.popBackStack() },
                onMealSelected = { mealId, quantity ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("selected_meal_id", mealId)
                        set("selected_meal_quantity", quantity)
                        set("selected_slot_type", slotType)
                    }
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
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
        composable(Screen.BarcodeScanner.route) {
            BarcodeScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onFoodSaved = { navController.popBackStack() }
            )
        }
        composable(Screen.OnlineSearch.route) {
            OnlineSearchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
