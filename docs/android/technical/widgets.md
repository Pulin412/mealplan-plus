# Widget System

MealPlan+ provides three Jetpack Glance home screen widgets. They are reactive — updating automatically whenever the underlying Room data changes, with no manual refresh required.

---

## Widgets Overview

| Widget | Class | Size | Description |
|--------|-------|------|-------------|
| Today's Plan | `TodayPlanWidget` | 4×2 | Planned meals for today by slot |
| Diet Summary | `DietSummaryWidget` | 2×2 | Calorie ring + macro totals |
| Mini Calendar | `CalendarWidget` | 4×2 | Month calendar with plan indicators |

---

## Architecture

```
Room DB (Flow)
    │
    ▼
WidgetDataRepository
    ├── getTodaySlotsFlow()        → Flow<Pair<String?, List<TodaySlot>>>
    └── getTodayDietSummaryFlow()  → Flow<DietSummaryData?>
    │
    ▼
Widget.provideGlance(context, id) {
    val data by flow.collectAsState(initial = ...)
    provideContent {
        GlanceTheme { WidgetContent(data) }
    }
}
```

The key insight: `collectAsState()` inside `provideContent {}` makes widgets **self-updating**. Room emits a new value → the Glance composable recomposes → the widget updates on the home screen. No `AppWidgetManager.updateAll()` call needed.

---

## TodayPlanWidget

**Data flow:**
```
DailyLogRepo.getLogWithFoods(today)
    │  map to slot list
    ▼
List<TodaySlot>(slotType, mealName, calories)
```

**Deep links:** Each slot row is tappable and navigates to `DailyLogWithDate(today)` inside the app.

---

## DietSummaryWidget

**Data flow:**
```
DailyLogRepo.getLogWithFoods(today)
    │  map to macros
    ▼
DietSummaryData(consumedCalories, goalCalories, protein, carbs, fat)
```

**Calorie ring:** Rendered as a Canvas bitmap using `macroRingBitmap()`. The bitmap is `remember(summary)`-keyed — it is only redrawn when `summary` changes, avoiding unnecessary Canvas work on every recomposition.

```kotlin
val caloriesBitmap = remember(summary) {
    summary?.let { s ->
        macroRingBitmap(s.consumedCalories, s.goalCalories, "kcal", CaloriesColor, density)
    }
}
```

---

## CalendarWidget

**Data flow:**
```
PlanDao.getPlansForMonth(userId, yearMonth)
    │  map to highlighted dates
    ▼
Set<LocalDate>   (dates with diet plans)
```

**Deep links:** Tapping a date navigates to `CalendarWithDate(date)` inside the app.

---

## WidgetDataRepository

Central data access layer for all widgets. Avoids direct DAO dependency in widget classes:

```kotlin
class WidgetDataRepository(context: Context) {

    fun getTodaySlotsFlow(): Flow<Pair<String?, List<TodaySlot>>>
        = logRepo.getLogWithFoods(LocalDate.now())
            .map { logWithFoods -> /* build slot list */ }
            .catch { emit(null to emptyList()) }

    fun getTodayDietSummaryFlow(): Flow<DietSummaryData?>
        = logRepo.getLogWithFoods(LocalDate.now())
            .map { logWithFoods -> /* build summary */ }
            .catch { emit(null) }
}
```

The `.catch` operator ensures the widget always shows something (empty state) even if the DB query fails.

---

## WidgetPreferences

User-configurable appearance settings stored in SharedPreferences:

```kotlin
data class WidgetAppearance(
    val theme: WidgetTheme,      // LIGHT / DARK / SYSTEM
    val showMacros: Boolean,
    val compactLayout: Boolean
)
```

Accessed via `WidgetPreferences.getAppearance(context)` in each widget's `provideGlance`.

---

## Deep Link Architecture

```
WidgetDeepLink data class
    ├── id: String          (UUID — unique per tap for LaunchedEffect keying)
    ├── target: String      (NAV_HOME / NAV_CALENDAR / NAV_LOG_FOR_DATE / …)
    ├── date: String?       (yyyy-MM-dd for date-specific navigation)
    └── dietId: Long?       (for NAV_DIET_DETAIL)
```

Created in widget click handlers:
```kotlin
actionStartActivity(
    Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_WIDGET_DEEP_LINK, Json.encodeToString(deepLink))
        addFlags(FLAG_ACTIVITY_SINGLE_TOP)
    }
)
```

`MainActivity` picks this up in `onNewIntent()` and passes it down to `MealPlanNavHost`.

---

## Adding a New Widget

1. Create a new class extending `GlanceAppWidget`.
2. Implement `provideGlance(context, id)` with `collectAsState` for reactive data.
3. Create a `GlanceAppWidgetReceiver` subclass.
4. Register in `AndroidManifest.xml` with `<receiver>` and `appwidget-provider` metadata.
5. Add widget XML in `res/xml/` with dimensions, preview, and description.
6. Add data method to `WidgetDataRepository` if new data is needed.
