# Navigation Architecture

MealPlan+ uses **Jetpack Navigation Compose** with a sealed `Screen` class for type-safe route definitions.

---

## Route Definitions

All routes are defined in `sealed class Screen(val route: String)` inside `NavHost.kt`:

```kotlin
sealed class Screen(val route: String) {
    object Login              : Screen("login")
    object SignUp             : Screen("signup")
    object ForgotPassword     : Screen("forgot_password")
    object Home               : Screen("home")
    object Profile            : Screen("profile")
    object Foods              : Screen("foods")
    object AddFood            : Screen("add_food")
    object Meals              : Screen("meals")
    object AddMeal            : Screen("add_meal")
    object EditMeal           : Screen("edit_meal/{mealId}")
    object Diets              : Screen("diets")
    object AddDiet            : Screen("add_diet")
    object DietDetail         : Screen("diet_detail/{dietId}?autoEdit={autoEdit}")
    object DietMealSlot       : Screen("diet_meal_slot/{dietId}/{slotType}")
    object MealDetail         : Screen("meal_detail/{dietId}/{slotType}?readOnly={readOnly}")
    object DietMealPicker     : Screen("diet_meal_picker/{slotType}")
    object DailyLog           : Screen("daily_log")
    object DailyLogWithDate   : Screen("daily_log/{date}")
    object LogMealPicker      : Screen("log_meal_picker/{date}/{slotType}")
    object DietPicker         : Screen("diet_picker/{date}")
    object Calendar           : Screen("calendar")
    object CalendarWithDate   : Screen("calendar_date/{initialDate}")
    object Health             : Screen("health")
    object Charts             : Screen("charts")
    object Settings           : Screen("settings")
    object WidgetSettings     : Screen("widget_settings")
    object BarcodeScanner     : Screen("barcode_scanner")
    object OnlineSearch       : Screen("online_search")
    object GroceryLists       : Screen("grocery_lists")
    object CreateGroceryList  : Screen("create_grocery_list")
    object GroceryDetail      : Screen("grocery_detail/{listId}")
    // ... food pickers
}
```

Route helpers (e.g. `DietDetail.createRoute(dietId, autoEdit)`) generate parameterised URLs safely.

---

## Navigation Graph

```
Login ──────────────────────────────────────┐
SignUp ─────────────────────────────────────┤
ForgotPassword ─────────────────────────────┤
                                            ▼
                              Scaffold (BottomNavBar)
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
            Home               Calendar               Daily Log
              │                     │                     │
              ├── Foods             ├── DailyLogWithDate  ├── LogMealPicker
              │   └── AddFood       │   └── LogMealPicker │   └── (back)
              │   └── BarcodeScanner│   └── DietPicker    └── DietPicker
              │   └── OnlineSearch  └── CalendarWithDate      └── (back)
              │
              ├── Meals
              │   ├── AddMeal ──── FoodPicker
              │   └── EditMeal─── FoodPickerForEdit
              │
              ├── DietPicker (today)
              │   └── (back to Home)
              │
              └── Profile / Settings / WidgetSettings

           Diets               Health               Grocery
              │                  │                     │
              ├── AddDiet        ├── Charts            ├── CreateGroceryList
              └── DietDetail     └── (back)            └── GroceryDetail
                  ├── FoodPickerForDietSlot
                  ├── DietMealSlot
                  │   ├── DietMealPicker
                  │   └── FoodPickerForDietSlot
                  └── MealDetail (readOnly)
```

---

## Bottom Navigation

Six tabs with `saveState = true` for all except Home:

```kotlin
navController.navigate(route) {
    popUpTo(Screen.Home.route) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

Home tab always navigates (clears its own back stack). Other tabs restore saved state when re-selected.

---

## Inter-Screen Data Passing

Complex data is passed back from sub-screens using `SavedStateHandle`:

```
// Sub-screen sets data:
navController.previousBackStackEntry?.savedStateHandle?.set("selected_food_id", food.id)
navController.popBackStack()

// Parent screen observes:
val foodId by savedStateHandle.getStateFlow("selected_food_id", -1L).collectAsState()
```

This pattern is used for:
- `FoodPicker` → `AddMeal` / `EditMeal` (selected food ID, quantity, unit)
- `DietPicker` → `DailyLog` / `Home` (selected diet ID, date)
- `DietMealPicker` → `DietMealSlot` (selected meal ID, slot type)

---

## Widget Deep Links

Widgets deep link into specific app screens. The flow:

```
Widget tap
    │
    ▼
WidgetDeepLink (Intent extra: target, date, dietId)
    │
    ▼
MainActivity.onNewIntent() → updates widgetDeepLink state
    │
    ▼
MealPlanNavHost receives widgetDeepLink
    │
    ▼
LaunchedEffect(widgetDeepLink?.id) {
    when (target) {
        NAV_HOME           → navigate to Home
        NAV_CALENDAR       → navigate to Calendar
        NAV_CALENDAR_FOR_DATE → navigate to CalendarWithDate(date)
        NAV_LOG_FOR_DATE   → navigate to DailyLogWithDate(date)
        NAV_DIET_DETAIL    → navigate to DietDetail(dietId)
    }
}
```

Keying the `LaunchedEffect` on `widgetDeepLink?.id` (a unique ID per tap) ensures that tapping the same widget type twice in a row still triggers navigation.

---

## Logout Navigation

When `isLoggedIn` transitions from `true → false`, the Activity is restarted:

```kotlin
if (previousLoggedIn == true && isLoggedIn == false) {
    activity.startActivity(
        Intent(activity, activity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
        }
    )
    activity.finish()
}
```

**Why restart the Activity instead of just navigating to Login?**
- Clears all `@HiltViewModel` instances (ensures no stale user data in memory)
- Resets the entire NavHost back stack
- Prevents accidental back navigation into authenticated screens
