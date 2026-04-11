package com.mealplanplus.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import java.time.LocalDate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mealplanplus.ui.screens.settings.WidgetSettingsScreen
import com.mealplanplus.ui.screens.scanner.BarcodeScannerScreen
import com.mealplanplus.ui.screens.scanner.OnlineSearchScreen
import com.mealplanplus.ui.screens.meals.FoodPickerScreen
import com.mealplanplus.ui.screens.diets.DietMealSlotScreen
import com.mealplanplus.ui.screens.diets.DietMealPickerScreen
import com.mealplanplus.ui.screens.diets.MealDetailScreen
import com.mealplanplus.ui.screens.log.DietPickerScreen
import com.mealplanplus.ui.screens.auth.LoginScreen
import com.mealplanplus.ui.screens.auth.SignUpScreen
import com.mealplanplus.ui.screens.auth.ForgotPasswordScreen
import com.mealplanplus.ui.screens.profile.ProfileScreen
import com.mealplanplus.ui.screens.grocery.GroceryListsScreen
import com.mealplanplus.ui.screens.grocery.CreateGroceryListScreen
import com.mealplanplus.ui.screens.grocery.GroceryDetailScreen
import android.app.Activity
import android.content.Intent
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.widget.NAV_CALENDAR
import com.mealplanplus.widget.NAV_CALENDAR_FOR_DATE
import com.mealplanplus.widget.NAV_DIET_DETAIL
import com.mealplanplus.widget.NAV_HOME
import com.mealplanplus.widget.NAV_LOG_FOR_DATE
import com.mealplanplus.widget.WidgetDeepLink

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
    object DietDetail : Screen("diet_detail/{dietId}?autoEdit={autoEdit}") {
        fun createRoute(dietId: Long, autoEdit: Boolean = false) = "diet_detail/$dietId?autoEdit=$autoEdit"
    }
    object DietMealSlot : Screen("diet_meal_slot/{dietId}/{slotType}") {
        fun createRoute(dietId: Long, slotType: String) = "diet_meal_slot/$dietId/$slotType"
    }
    object MealDetail : Screen("meal_detail/{dietId}/{slotType}?readOnly={readOnly}") {
        fun createRoute(dietId: Long, slotType: String, readOnly: Boolean = false) =
            "meal_detail/$dietId/$slotType?readOnly=$readOnly"
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
    object CalendarWithDate : Screen("calendar_date/{initialDate}") {
        fun createRoute(date: String) = "calendar_date/$date"
    }
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
    object FoodPickerForCustomSlot : Screen("food_picker_custom_slot")
    object WidgetSettings : Screen("widget_settings")
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
    BottomNavItem("Diets", Icons.Filled.Restaurant, Screen.Diets.route),
    BottomNavItem("Health", Icons.Filled.FavoriteBorder, Screen.Health.route),
    BottomNavItem("Grocery", Icons.Filled.ShoppingCart, Screen.GroceryLists.route)
)

// Routes where the bottom nav should be visible
private val bottomNavRoutes = setOf(
    Screen.Home.route,
    Screen.Calendar.route,
    Screen.CalendarWithDate.route,
    Screen.Diets.route,
    Screen.Health.route,
    Screen.GroceryLists.route
)

@Composable
fun MealPlanNavHost(
    widgetDeepLink: WidgetDeepLink? = null
) {
    val widgetNavTarget = widgetDeepLink?.target
    val widgetDate      = widgetDeepLink?.date
    val widgetDietId    = widgetDeepLink?.dietId
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

    val startDestination = remember { if (isLoggedIn == true) Screen.Home.route else Screen.Login.route }

    // On logout: restart the activity cleanly so NavHost, ViewModels, and back
    // stack all reset to a fresh state. Avoids Compose Navigation back-stack bugs.
    //
    // We track the previous value so we only restart on a true→false transition.
    // Checking startDestination was wrong: if the user launched the app while
    // already logged out (startDestination="login"), logged into a new account,
    // then logged out again, startDestination never equalled "home" and the
    // restart never fired — leaving them stuck on the profile screen.
    var previousLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isLoggedIn) {
        if (previousLoggedIn == true && isLoggedIn == false) {
            val activity = context as? Activity ?: return@LaunchedEffect
            val intent = Intent(activity, activity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            activity.startActivity(intent)
            activity.finish()
        }
        previousLoggedIn = isLoggedIn
    }

    // Handle widget deep-links.
    // Key on widgetDeepLink?.id so that:
    //   • every new tap (even to the same destination type) triggers navigation, and
    //   • warm-launch taps (onNewIntent) re-run the effect because MainActivity updates
    //     widgetDeepLink with a fresh id each time.
    LaunchedEffect(widgetDeepLink?.id) {
        if (widgetNavTarget == null || isLoggedIn != true) return@LaunchedEffect
        when (widgetNavTarget) {
            NAV_HOME -> navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
                launchSingleTop = true
            }
            NAV_CALENDAR -> navController.navigate(Screen.Calendar.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
                launchSingleTop = true
            }
            NAV_DIET_DETAIL -> {
                val dietId = widgetDietId ?: return@LaunchedEffect
                navController.navigate(Screen.DietDetail.createRoute(dietId, autoEdit = false)) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            }
            NAV_CALENDAR_FOR_DATE -> {
                val date = widgetDate ?: return@LaunchedEffect
                navController.navigate(Screen.CalendarWithDate.createRoute(date)) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            }
            NAV_LOG_FOR_DATE -> {
                val date = widgetDate ?: return@LaunchedEffect
                navController.navigate(Screen.DailyLogWithDate.createRoute(date)) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            }
        }
    }

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
                    onLogout = { /* LaunchedEffect(isLoggedIn) handles nav after clearAuth emits */ },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) { backStackEntry ->
                HomeScreen(
                    onNavigateToLog = { navController.navigate(Screen.DailyLog.route) },
                    onNavigateToLogWithDate = { date ->
                        navController.navigate(Screen.CalendarWithDate.createRoute(date))
                    },
                    onNavigateToHealth = { navController.navigate(Screen.Health.route) },
                    onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                    onNavigateToGroceryLists = { navController.navigate(Screen.GroceryLists.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToDietPickerForToday = {
                        navController.navigate(
                            Screen.DietPicker.createRoute(LocalDate.now().toString())
                        )
                    },
                    onNavigateToMealDetail = { dietId, slotType ->
                        navController.navigate(Screen.MealDetail.createRoute(dietId, slotType, readOnly = true))
                    },
                    onNavigateToFoods = { navController.navigate(Screen.Foods.route) },
                    onNavigateToMeals = { navController.navigate(Screen.Meals.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToDiets = { navController.navigate(Screen.Diets.route) },
                    savedStateHandle = backStackEntry.savedStateHandle
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
                    onFoodSelected = { food, quantity, unit ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("selected_food_id", food.id)
                            set("selected_quantity", quantity)
                            set("selected_unit", unit.name)
                        }
                        navController.popBackStack()
                    },
                    onUsdaFoodSelected = { usdaFood, quantity, unit ->
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
                            set("selected_unit", unit.name)
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
                    onFoodSelected = { food, quantity, unit ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("selected_food_id", food.id)
                            set("selected_quantity", quantity)
                            set("selected_unit", unit.name)
                        }
                        navController.popBackStack()
                    },
                    onUsdaFoodSelected = { usdaFood, quantity, unit ->
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
                            set("selected_unit", unit.name)
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Diets.route) {
                DietsScreen(
                    onNavigateToAddDiet = { navController.navigate(Screen.AddDiet.route) },
                    onNavigateToDietDetail = { dietId -> navController.navigate(Screen.DietDetail.createRoute(dietId, autoEdit = true)) },
                    onNavigateToDietDetailView = { dietId -> navController.navigate(Screen.DietDetail.createRoute(dietId, autoEdit = false)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AddDiet.route) {
                AddDietScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onDietSaved = { dietId ->
                        navController.navigate(Screen.DietDetail.createRoute(dietId, autoEdit = true)) {
                            popUpTo(Screen.AddDiet.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.DietDetail.route,
                arguments = listOf(
                    navArgument("dietId") { type = NavType.LongType },
                    navArgument("autoEdit") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val savedStateHandle = backStackEntry.savedStateHandle
                val autoEdit = backStackEntry.arguments?.getBoolean("autoEdit") ?: false
                DietDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFoodPicker = { navController.navigate(Screen.FoodPickerForDietSlot.route) },
                    onNavigateToMealDetail = { dietId, slotType ->
                        navController.navigate(Screen.MealDetail.createRoute(dietId, slotType))
                    },
                    savedStateHandle = savedStateHandle,
                    autoEdit = autoEdit
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
            composable(
                route = Screen.MealDetail.route,
                arguments = listOf(
                    navArgument("dietId") { type = NavType.LongType },
                    navArgument("slotType") { type = NavType.StringType },
                    navArgument("readOnly") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val readOnly = backStackEntry.arguments?.getBoolean("readOnly") ?: false
                MealDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    readOnly = readOnly
                )
            }
            composable(Screen.FoodPickerForDietSlot.route) {
                FoodPickerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onFoodSelected = { food, quantity, unit ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("selected_food_id", food.id)
                            set("selected_quantity", quantity)
                            set("selected_unit", unit.name)
                        }
                        navController.popBackStack()
                    },
                    onUsdaFoodSelected = { usdaFood, quantity, unit ->
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
                            set("selected_unit", unit.name)
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
                    onNavigateToFoodPickerForCustomSlot = {
                        navController.navigate(Screen.FoodPickerForCustomSlot.route)
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
                    onNavigateToFoodPickerForCustomSlot = {
                        navController.navigate(Screen.FoodPickerForCustomSlot.route)
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
            composable(Screen.Calendar.route) { backStackEntry ->
                CalendarScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLog = { date ->
                        navController.navigate(Screen.DailyLogWithDate.createRoute(date))
                    },
                    onNavigateToDietPicker = { date ->
                        navController.navigate(Screen.DietPicker.createRoute(date))
                    },
                    onNavigateToMealDetail = { dietId, slotType ->
                        navController.navigate(Screen.MealDetail.createRoute(dietId, slotType, readOnly = true))
                    },
                    savedStateHandle = backStackEntry.savedStateHandle
                )
            }
            // Widget deep-link: same screen but with an initialDate nav argument so the
            // CalendarViewModel pre-selects the tapped date instead of defaulting to today.
            composable(
                route = Screen.CalendarWithDate.route,
                arguments = listOf(navArgument("initialDate") { type = NavType.StringType })
            ) { backStackEntry ->
                CalendarScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLog = { date ->
                        navController.navigate(Screen.DailyLogWithDate.createRoute(date))
                    },
                    onNavigateToDietPicker = { date ->
                        navController.navigate(Screen.DietPicker.createRoute(date))
                    },
                    onNavigateToMealDetail = { dietId, slotType ->
                        navController.navigate(Screen.MealDetail.createRoute(dietId, slotType, readOnly = true))
                    },
                    savedStateHandle = backStackEntry.savedStateHandle
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
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWidgetSettings = { navController.navigate(Screen.WidgetSettings.route) }
                )
            }
            composable(Screen.WidgetSettings.route) {
                WidgetSettingsScreen(
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
            composable(Screen.FoodPickerForCustomSlot.route) {
                com.mealplanplus.ui.screens.foods.FoodsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    pickerMode = true,
                    onFoodSelected = { food, qty ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("custom_food_id", food.id)
                            set("custom_food_qty", qty)
                        }
                        navController.popBackStack()
                    }
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
            val isHome = item.route == Screen.Home.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    // Home tab always navigates (clears back stack to home root).
                    // Other tabs skip if already selected.
                    if (!selected || isHome) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = !isHome
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = !isHome
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
