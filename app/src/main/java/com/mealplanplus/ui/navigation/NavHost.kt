package com.mealplanplus.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import androidx.compose.material.icons.filled.Restaurant
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
import com.mealplanplus.ui.screens.auth.LoginScreen
import com.mealplanplus.ui.screens.auth.SignUpScreen
import com.mealplanplus.ui.screens.auth.ForgotPasswordScreen
import com.mealplanplus.ui.screens.profile.ProfileScreen
import com.mealplanplus.ui.screens.grocery.GroceryListsScreen
import com.mealplanplus.ui.screens.grocery.CreateGroceryListScreen
import com.mealplanplus.ui.screens.grocery.GroceryDetailScreen
import com.mealplanplus.util.AuthPreferences

private val PrimaryGreen = Color(0xFF2E7D52)

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Profile : Screen("profile")
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
    object ForgotPassword : Screen("forgot_password")
    object Settings : Screen("settings")
    object BarcodeScanner : Screen("barcode_scanner")
    object OnlineSearch : Screen("online_search")
    object GroceryLists : Screen("grocery_lists")
    object CreateGroceryList : Screen("create_grocery_list")
    object GroceryDetail : Screen("grocery_detail/{listId}") {
        fun createRoute(listId: Long) = "grocery_detail/$listId"
    }
}

// Bottom nav tab definitions
private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Screen.Home.route),
    BottomNavItem("Meal Plan", Icons.Filled.CalendarMonth, Screen.Calendar.route),
    BottomNavItem("Log", Icons.Filled.EditNote, Screen.DailyLog.route),
    BottomNavItem("Diets", Icons.Filled.Restaurant, Screen.Diets.route),
    BottomNavItem("Health", Icons.Filled.FavoriteBorder, Screen.Health.route),
    BottomNavItem("Grocery", Icons.Filled.ShoppingCart, Screen.GroceryLists.route)
)

// Routes where the bottom nav should be visible
private val bottomNavRoutes = setOf(
    Screen.Home.route,
    Screen.Calendar.route,
    Screen.DailyLog.route,
    Screen.Diets.route,
    Screen.Health.route,
    Screen.GroceryLists.route
)

@Composable
fun MealPlanNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val isLoggedIn by AuthPreferences.isLoggedIn(context).collectAsState(initial = null)

    if (isLoggedIn == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (isLoggedIn == true) Screen.Home.route else Screen.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onSignUpSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToLog = { navController.navigate(Screen.DailyLog.route) },
                    onNavigateToLogWithDate = { date ->
                        navController.navigate(Screen.DailyLogWithDate.createRoute(date))
                    },
                    onNavigateToHealth = { navController.navigate(Screen.Health.route) },
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                    onNavigateToGroceryLists = { navController.navigate(Screen.GroceryLists.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
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
                    onNavigateHome = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
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
                    onNavigateHome = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
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
            composable(Screen.GroceryLists.route) {
                GroceryListsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate(Screen.CreateGroceryList.route) },
                    onNavigateToDetail = { listId ->
                        navController.navigate(Screen.GroceryDetail.createRoute(listId))
                    }
                )
            }
            composable(Screen.CreateGroceryList.route) {
                CreateGroceryListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onListCreated = { listId ->
                        navController.navigate(Screen.GroceryDetail.createRoute(listId)) {
                            popUpTo(Screen.CreateGroceryList.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.GroceryDetail.route,
                arguments = listOf(navArgument("listId") { type = NavType.LongType })
            ) {
                GroceryDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavController, currentRoute: String?) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = PrimaryGreen
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            // Pop up to home to avoid deep back stack
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryGreen,
                    selectedTextColor = PrimaryGreen,
                    indicatorColor = Color(0xFFE8F5E9),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
