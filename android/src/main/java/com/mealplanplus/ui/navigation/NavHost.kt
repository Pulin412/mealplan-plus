package com.mealplanplus.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import java.time.LocalDate
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
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
import com.mealplanplus.ui.screens.calendar.CalendarDayDetailScreen
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
import com.mealplanplus.ui.screens.auth.LandingScreen
import com.mealplanplus.ui.screens.auth.LoginScreen
import com.mealplanplus.ui.screens.auth.SignUpScreen
import com.mealplanplus.ui.screens.auth.ForgotPasswordScreen
import com.mealplanplus.ui.screens.profile.ProfileScreen
import com.mealplanplus.ui.screens.grocery.GroceryListsScreen
import com.mealplanplus.ui.screens.grocery.CreateGroceryListScreen
import com.mealplanplus.ui.screens.grocery.GroceryDetailScreen
import com.mealplanplus.ui.screens.workout.WorkoutHistoryScreen
import com.mealplanplus.ui.screens.workout.WorkoutLogScreen
import com.mealplanplus.ui.screens.workout.WorkoutTemplatesScreen
import com.mealplanplus.ui.screens.workout.AddEditWorkoutTemplateScreen
import com.mealplanplus.ui.screens.workout.AddEditExerciseScreen
import com.mealplanplus.ui.screens.workout.ExerciseCatalogueScreen
import android.app.Activity
import android.content.Intent
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.widget.NAV_CALENDAR
import com.mealplanplus.widget.NAV_CALENDAR_FOR_DATE
import com.mealplanplus.widget.NAV_DIET_DETAIL
import com.mealplanplus.widget.NAV_HOME
import com.mealplanplus.widget.NAV_LOG_FOR_DATE
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.alpha
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.DividerColor
import com.mealplanplus.ui.theme.LocalIsDarkTheme
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.widget.WidgetDeepLink
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
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
    object PlanDayDetail : Screen("plan_day/{initialDate}") {
        fun createRoute(date: String) = "plan_day/$date"
    }
    object Health : Screen("health")
    object Charts : Screen("charts")
    object ForgotPassword : Screen("forgot_password")
    object Settings : Screen("settings")
    object BarcodeScanner : Screen("barcode_scanner")
    object BarcodeScannerForAddFood : Screen("barcode_scanner_for_add_food")
    object OnlineSearch : Screen("online_search")
    object GroceryLists : Screen("grocery_lists")
    object CreateGroceryList : Screen("create_grocery_list")
    object GroceryDetail : Screen("grocery_detail/{listId}") {
        fun createRoute(listId: Long) = "grocery_detail/$listId"
    }
    object FoodPickerForCustomSlot : Screen("food_picker_custom_slot")
    object WidgetSettings : Screen("widget_settings")
    object WorkoutHistory : Screen("workout_history")
    object WorkoutLog : Screen("workout_log?templateId={templateId}") {
        fun create(templateId: Long? = null) = if (templateId != null) "workout_log?templateId=$templateId" else "workout_log?templateId=-1"
    }
    object WorkoutTemplates : Screen("workout_templates")
    object AddWorkoutTemplate : Screen("add_workout_template/{templateId}") {
        fun create(templateId: Long? = null) = "add_workout_template/${templateId ?: -1}"
    }
    object AddExercise : Screen("add_exercise/{exerciseId}") {
        fun create(exerciseId: Long? = null) = "add_exercise/${exerciseId ?: -1}"
    }
    object ExerciseCatalogue : Screen("exercise_catalogue")
}

// Bottom nav tab definitions (4 visible + 1 FAB)
private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    // secondary routes that also make this tab appear selected
    val ownedRoutePrefix: String? = null
)

// Only hide the bar on unauthenticated screens
private val authOnlyRoutes = setOf(
    Screen.Landing.route,
    Screen.Login.route,
    Screen.SignUp.route,
    Screen.ForgotPassword.route
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

    val startDestination = remember { if (isLoggedIn == true) Screen.Home.route else Screen.Landing.route }

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
    val showBottomBar = isLoggedIn == true && currentRoute != null && currentRoute !in authOnlyRoutes
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showMiscSheet    by remember { mutableStateOf(false) }

    // Tabs in swipe order (ignoring + and More)
    val swipeTabs = listOf(Screen.Home.route, Screen.DailyLog.route, Screen.Calendar.route)
    val currentTabIndex = when {
        currentRoute == Screen.Home.route -> 0
        currentRoute?.startsWith("daily_log") == true -> 1
        currentRoute?.startsWith("calendar") == true -> 2
        else -> -1
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(Screen.Home.route) { saveState = route != Screen.Home.route; inclusive = false }
            launchSingleTop = true
            restoreState = route != Screen.Home.route
        }
    }

    var swipeAccum by remember { mutableFloatStateOf(0f) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    onQuickAdd = { showQuickAddSheet = true },
                    onMisc     = { showMiscSheet = true }
                )
            }
        }
    ) { innerPadding ->
        val swipeMod = if (showBottomBar && currentTabIndex >= 0) {
            Modifier.pointerInput(currentTabIndex) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeAccum = 0f },
                    onDragEnd = { swipeAccum = 0f },
                    onDragCancel = { swipeAccum = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeAccum += dragAmount
                        val threshold = 120.dp.toPx()
                        when {
                            swipeAccum < -threshold && currentTabIndex < swipeTabs.size - 1 -> {
                                navigateToTab(swipeTabs[currentTabIndex + 1])
                                swipeAccum = 0f
                            }
                            swipeAccum > threshold && currentTabIndex > 0 -> {
                                navigateToTab(swipeTabs[currentTabIndex - 1])
                                swipeAccum = 0f
                            }
                        }
                    }
                )
            }
        } else Modifier

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .then(swipeMod)
        ) {
            composable(Screen.Landing.route) {
                LandingScreen(
                    onSignInWithEmail = { navController.navigate(Screen.Login.route) },
                    onCreateAccount = { navController.navigate(Screen.SignUp.route) }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    onNavigateBack = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Landing.route) { inclusive = true }
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
                            popUpTo(Screen.Landing.route) { inclusive = true }
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
            composable(Screen.AddFood.route) { backStackEntry ->
                val savedStateHandle = backStackEntry.savedStateHandle
                AddFoodScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanner = { navController.navigate(Screen.BarcodeScannerForAddFood.route) },
                    savedStateHandle = savedStateHandle
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
                        navController.navigate(Screen.DietDetail.createRoute(dietId, autoEdit = false)) {
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
                val dietId = backStackEntry.arguments?.getLong("dietId") ?: 0L
                DietDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFoodPicker = { navController.navigate(Screen.FoodPickerForDietSlot.route) },
                    onNavigateToMealDetail = { dId, slotType ->
                        navController.navigate(Screen.MealDetail.createRoute(dId, slotType))
                    },
                    onNavigateToEditSlot = { slotType ->
                        navController.navigate(Screen.DietMealSlot.createRoute(dietId, slotType))
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
                    onNavigateToDayDetail = { date ->
                        navController.navigate(Screen.PlanDayDetail.createRoute(date.toString()))
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
                    onNavigateToDayDetail = { date ->
                        navController.navigate(Screen.PlanDayDetail.createRoute(date.toString()))
                    },
                    savedStateHandle = backStackEntry.savedStateHandle
                )
            }
            composable(
                route = Screen.PlanDayDetail.route,
                arguments = listOf(navArgument("initialDate") { type = NavType.StringType })
            ) { backStackEntry ->
                CalendarDayDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDietPicker = { date ->
                        navController.navigate(Screen.DietPicker.createRoute(date))
                    },
                    onNavigateToLog = { date ->
                        navController.navigate(Screen.DailyLogWithDate.createRoute(date))
                    },
                    onNavigateToStartWorkout = { templateId ->
                        navController.navigate(Screen.WorkoutLog.create(templateId))
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
                    onNavigateToWidgetSettings = { navController.navigate(Screen.WidgetSettings.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
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
            // Scanner in "fill form" mode — passes scanned food data back to AddFoodScreen
            composable(Screen.BarcodeScannerForAddFood.route) {
                BarcodeScannerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onFoodSaved = { navController.popBackStack() },
                    onFoodFound = { food ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("scanned_food_name", food.name)
                            set("scanned_food_brand", food.brand)
                            set("scanned_food_calories", food.caloriesPer100)
                            set("scanned_food_protein", food.proteinPer100)
                            set("scanned_food_carbs", food.carbsPer100)
                            set("scanned_food_fat", food.fatPer100)
                        }
                        navController.popBackStack()
                    }
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
            composable(Screen.WorkoutHistory.route) {
                WorkoutHistoryScreen(
                    onNavigateToLog = { navController.navigate(Screen.WorkoutLog.create()) },
                    onNavigateToExercises = { navController.navigate(Screen.ExerciseCatalogue.route) },
                    onNavigateToTemplates = { navController.navigate(Screen.WorkoutTemplates.route) }
                )
            }
            composable(
                route = Screen.WorkoutLog.route,
                arguments = listOf(navArgument("templateId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { backStack ->
                val templateId = backStack.arguments?.getLong("templateId")?.takeIf { it > 0 }
                WorkoutLogScreen(
                    preselectedTemplateId = templateId,
                    onBack = { navController.popBackStack() },
                    onFinished = { navController.popBackStack() }
                )
            }
            composable(Screen.WorkoutTemplates.route) {
                WorkoutTemplatesScreen(
                    onBack = { navController.popBackStack() },
                    onCreateTemplate = { navController.navigate(Screen.AddWorkoutTemplate.create()) },
                    onEditTemplate = { id -> navController.navigate(Screen.AddWorkoutTemplate.create(id)) },
                    onStartFromTemplate = { id ->
                        navController.navigate(Screen.WorkoutLog.create(id)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = Screen.AddWorkoutTemplate.route,
                arguments = listOf(navArgument("templateId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) { backStack ->
                val existingId = backStack.arguments?.getLong("templateId")?.takeIf { it > 0 }
                AddEditWorkoutTemplateScreen(
                    existingTemplateId = existingId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.AddExercise.route,
                arguments = listOf(navArgument("exerciseId") {
                    type = NavType.LongType; defaultValue = -1L
                })
            ) {
                AddEditExerciseScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ExerciseCatalogue.route) {
                ExerciseCatalogueScreen(
                    onBack = { navController.popBackStack() },
                    onAddExercise = { navController.navigate(Screen.AddExercise.create()) }
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

    if (showQuickAddSheet) {
        QuickAddSheet(
            onDismiss = { showQuickAddSheet = false },
            onNavigate = { route ->
                showQuickAddSheet = false
                navController.navigate(route)
            }
        )
    }

    if (showMiscSheet) {
        MiscSheet(
            onDismiss = { showMiscSheet = false },
            onNavigate = { route ->
                showMiscSheet = false
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) { saveState = true; inclusive = false }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

// ── New minimalist bottom nav (5 slots: Home | Log | + | Plan | Misc) ──────────

private val SelectedNavColor: Color
    @Composable get() = TextPrimary
private val UnselectedNavColor: Color
    @Composable get() = TextMuted
private val NavIndicator: Color
    @Composable get() = TagGrayBg

@Composable
private fun BottomNavBar(
    navController: NavController,
    currentRoute: String?,
    onQuickAdd: () -> Unit,
    onMisc: () -> Unit
) {
    // Determine which tab is active based on current route
    val isHomeActive = currentRoute == Screen.Home.route
    val isLogActive  = currentRoute?.startsWith("daily_log") == true
    val isPlanActive = currentRoute?.startsWith("calendar") == true

    fun navTo(route: String, isHome: Boolean = false) {
        navController.navigate(route) {
            popUpTo(Screen.Home.route) {
                saveState = !isHome
                inclusive = false
            }
            launchSingleTop = true
            restoreState = !isHome
        }
    }

    NavigationBar(
        containerColor = CardBg,
        contentColor = SelectedNavColor,
        tonalElevation = 0.dp,
        modifier = Modifier.height(64.dp)
    ) {
        // Home
        NavTab(
            icon = Icons.Filled.Home,
            label = "Home",
            selected = isHomeActive,
            onClick = { navTo(Screen.Home.route, isHome = true) }
        )

        // Log → today's food log
        NavTab(
            icon = Icons.Default.Description,
            label = "Log",
            selected = isLogActive,
            onClick = { navTo(Screen.DailyLog.route) }
        )

        // Centre + FAB
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TextPrimary)
                    .clickable { onQuickAdd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick add",
                    tint = CardBg,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Plan → Calendar
        NavTab(
            icon = Icons.Filled.CalendarMonth,
            label = "Plan",
            selected = isPlanActive,
            onClick = { navTo(Screen.Calendar.route) }
        )

        // Misc → opens bottom sheet
        NavTab(
            icon = Icons.Default.Settings,
            label = "More",
            selected = false,
            onClick = onMisc
        )
    }
}

@Composable
private fun RowScope.NavTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor   = SelectedNavColor,
            selectedTextColor   = SelectedNavColor,
            indicatorColor      = NavIndicator,
            unselectedIconColor = UnselectedNavColor,
            unselectedTextColor = UnselectedNavColor
        )
    )
}

// ── Quick-add action model ────────────────────────────────────────────────────

private data class QuickAction(
    val icon: ImageVector,
    val tint: Color,
    val label: String,
    val subtitle: String,
    val route: String
)

// ── Animated swipe-up quick-add sheet ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var revealed by remember { mutableStateOf(false) }

    val actions = listOf(
        QuickAction(Icons.Default.Edit,      Color(0xFF2E7D52), "Log Today's Meals", "Open today's food diary",           Screen.DailyLog.route),
        QuickAction(Icons.Default.Add,        Color(0xFFF59E0B), "Add a Food",         "Search or scan a food item",        Screen.AddFood.route),
        QuickAction(Icons.Default.Restaurant, Color(0xFFC05200), "Create a Meal",      "Bundle foods into a reusable meal", Screen.AddMeal.route),
        QuickAction(Icons.Default.List,       Color(0xFF1E4FBF), "Build a Diet",       "Create a structured diet plan",     Screen.AddDiet.route),
    )

    // Alpha for each row — staggered fade-in via animateFloatAsState + tween delayMillis
    val alphas = actions.mapIndexed { i, _ ->
        animateFloatAsState(
            targetValue = if (revealed) 1f else 0f,
            animationSpec = tween(durationMillis = 220, delayMillis = i * 65),
            label = "alpha_$i"
        ).value
    }

    // Offset for each row — staggered spring slide-up via Animatable
    val offsets = remember { actions.map { Animatable(24f) } }
    LaunchedEffect(Unit) {
        delay(60L)      // give the sheet its open animation first
        revealed = true
        actions.indices.forEach { i ->
            launch {
                delay(i * 65L)
                offsets[i].animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    fun go(route: String) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) { onDismiss(); onNavigate(route) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CardBg,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(TextMuted.copy(alpha = 0.3f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            actions.forEachIndexed { index, action ->
                QuickActionRow(
                    action   = action,
                    onClick  = { go(action.route) },
                    modifier = Modifier
                        .alpha(alphas[index])
                        .offset(y = offsets[index].value.dp)
                )
                if (index < actions.size - 1) {
                    HorizontalDivider(
                        modifier  = Modifier
                            .padding(start = 74.dp, end = 20.dp)
                            .alpha(alphas[index]),
                        color     = DividerColor,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    action:   QuickAction,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Soft rounded-square icon bubble
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(action.tint.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector     = action.icon,
                contentDescription = null,
                tint            = action.tint,
                modifier        = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text       = action.label,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = TextPrimary
            )
            Text(
                text     = action.subtitle,
                fontSize = 12.sp,
                color    = TextMuted
            )
        }
        Icon(
            imageVector        = Icons.Default.ChevronRight,
            contentDescription = null,
            tint               = TextMuted.copy(alpha = 0.5f),
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ── More bottom sheet (navigation destinations not covered by +) ──────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiscSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var revealed by remember { mutableStateOf(false) }

    val actions = listOf(
        QuickAction(Icons.Default.Restaurant,     Color(0xFF2E7D52), "Diets",    "Browse & manage diet plans",    Screen.Diets.route),
        QuickAction(Icons.Default.List,           Color(0xFFC05200), "Meals",    "Your meal library",             Screen.Meals.route),
        QuickAction(Icons.Default.Star,           Color(0xFF1E4FBF), "Foods",    "Food catalogue & nutrition",    Screen.Foods.route),
        QuickAction(Icons.Default.FavoriteBorder, Color(0xFFD32F2F), "Health",   "Metrics, weight & activity",    Screen.Health.route),
        QuickAction(Icons.Default.FitnessCenter,  Color(0xFF00796B), "Workouts", "Log & track gym sessions",      Screen.WorkoutHistory.route),
        QuickAction(Icons.Default.ShoppingCart,   Color(0xFF6A1B9A), "Grocery",  "Your shopping lists",           Screen.GroceryLists.route),
        QuickAction(Icons.Default.Settings,       Color(0xFF555555), "Settings", "Preferences & notifications",   Screen.Settings.route),
    )

    val alphas = actions.mapIndexed { i, _ ->
        animateFloatAsState(
            targetValue    = if (revealed) 1f else 0f,
            animationSpec  = tween(durationMillis = 220, delayMillis = i * 65),
            label          = "misc_alpha_$i"
        ).value
    }
    val offsets = remember { actions.map { Animatable(24f) } }
    LaunchedEffect(Unit) {
        delay(60L)
        revealed = true
        actions.indices.forEach { i ->
            launch {
                delay(i * 65L)
                offsets[i].animateTo(
                    targetValue    = 0f,
                    animationSpec  = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    fun go(route: String) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) { onDismiss(); onNavigate(route) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CardBg,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(TextMuted.copy(alpha = 0.3f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            actions.forEachIndexed { index, action ->
                QuickActionRow(
                    action   = action,
                    onClick  = { go(action.route) },
                    modifier = Modifier
                        .alpha(alphas[index])
                        .offset(y = offsets[index].value.dp)
                )
                if (index < actions.size - 1) {
                    HorizontalDivider(
                        modifier  = Modifier
                            .padding(start = 74.dp, end = 20.dp)
                            .alpha(alphas[index]),
                        color     = DividerColor,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}
