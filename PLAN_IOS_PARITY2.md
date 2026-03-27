# iOS Feature Parity Plan — Round 2

## Overview
Multi-phase plan. All changes on `develop` branch.

---

## Phase 1 — Quick Wins: Removals + Legend + Bug Fixes
**Files**: `HomeScreen.swift`, `DietDetailScreen.swift`

### 1a. HomeScreen.swift — Remove 3 sections
- Remove `SyncStatusBanner` from body (just remove the view block; sync stays in profile for later)
- Remove `QuickLogFoodButton` + `showQuickLog` state
- Remove `BloodGlucoseCard` entire section

### 1b. HomeScreen.swift — Add This Week legend row
Inside `ThisWeekCard`, after the 7-day circles row, add a legend row:
- 🟢 "Done" · 🟠 "Planned" · 🔴 "Missed" small colored dots + labels

### 1c. DietDetailScreen.swift — Fix "View Log" button
Action button "View Log" currently has empty action `{}`.
Fix: navigate to `DailyLogScreen` for today's date by passing callback or using NavigationLink.
Add `onViewLog: (() -> Void)? = nil` parameter to `DietDetailScreenNew`.
Wire from DietsScreen/wherever it's called.

---

## Phase 2 — HomeScreen: Today's Plan + Diet Picker + Slot Detail
**Files**: `HomeScreen.swift`, `FlowCollector.swift`

### 2a. "Plan Diet / Change Diet" button in Today's Plan header
- Add button right side of `TodaysPlanCard` header
- If `todayPlanSlots.isEmpty` → "Plan a Diet" (green text), else → "Change Diet"
- Tapping → `DietPickerSheet` (search + list of diets from `DietsViewModel`)
- On select → `HomeViewModel.assignDietForToday(diet:)` → calls `planRepo.insertOrUpdatePlan`
- `HomeViewModel` gets `planRepo = RepositoryProvider.shared.planRepository`
- After assign → reload `todayPlanSlots` via `load(userId:)`

### 2b. Tapping a meal slot → Read-only ingredient view
- Each row in `TodaysPlanCard` (that has `plannedMealId`) → tappable
- Opens `MealDetailSheet(mealId:)` bottom sheet
- Sheet: loads `MealWithFoods` via `MealsViewModel.getMealWithFoods`
- Shows: meal name, total macros header, list of food items (name, qty, kcal, P/C/F)
- Read-only — no edit actions

### 2c. Fix week-day calendar tap → open DailyLogScreen
- In `HomeScreen`, `onNavigateToLogWithDate` callback already exists as param
- In `WeekDayCell` / `ThisWeekCard`, the Button tap must call `onNavigateToLogWithDate(day.isoDate)`
- Check `AppNavigation.swift` / `ContentView.swift` — wire callback to push DailyLogScreen with that date

---

## Phase 3 — DailyLogScreen: Top Bar + Ingredient Ticks + Remove Log Meal
**Files**: `DailyLogScreen.swift`, `FlowCollector.swift`

### 3a. Add to DailyLogViewModel (FlowCollector.swift)
```swift
@Published var currentPlan: Plan? = nil
@Published var isPlanCompleted: Bool = false

func loadPlan(userId: Int64, date: String) async  // called in loadLog
func assignDiet(userId: Int64, date: String, diet: Diet) // insertOrUpdatePlan
func clearDiet(userId: Int64, date: String)        // deletePlan → reload
func completeDay(userId: Int64, date: String)      // insertOrUpdatePlan with isCompleted:true
func reopenDay(userId: Int64, date: String)        // isCompleted:false
```

### 3b. Top bar icons in DailyLogScreen
Add toolbar items:
- **Select Diet** (calendar icon): opens `DietPickerSheet` → on select calls `vm.assignDiet` → reload
- **Clear Plan** (trash icon): visible when `currentPlan?.dietId != nil && !isPlanCompleted`
  → confirm alert → `vm.clearDiet`
- **Complete Day** (checkmark): visible when plan exists & today or past & not completed
  → `vm.completeDay`
- **Reopen Day** (arrow.counterclockwise): visible when `isPlanCompleted`
  → `vm.reopenDay`

### 3c. Planned ingredient rows — tick/untick
`plannedMealsBySlot[slot]` already shows grey items.
Add tick logic: if `loggedFoodsBySlot[slot]` contains same `foodId` → show green tick on that row.
Tapping a planned food row:
- If not logged → call `logFood(userId:, date:, foodId:, quantity:, slotType:)` (log it)
- If logged → call `removeLoggedFood` (unlog it)
This creates bidirectional sync: home screen tick ↔ log screen tick.

### 3d. Remove "Log Meal" button from SlotCard
Remove "Log Meal" `Button` + `showLogMeal` / `logMealSlot` state from `DailyLogScreen`.
Remove `LogMealPickerSheet`.
Keep only "Add Food" button per slot.

### 3e. Slot-level log button (optional enhancement)
In slot header row, add small "Log All" button or chevron action to log all planned foods for that slot at once (iterate `plannedMealsBySlot[slot]` → logFood for each).

---

## Phase 4 — Diets Screen Redesign
**Files**: `DietsScreen.swift`, `DietDetailScreen.swift`

### 4a. DietsScreen — Real data + tags + filters
- Replace `SeedDataLoader` with `@StateObject private var vm = DietsViewModel()`
- `onAppear`: `vm.loadDiets(userId:)` + `vm.getAllTags(userId:)` → store in `@State tags: [Tag]`
- **Tags filter row** (horizontal scrollable chips): "All N", then per tag
  - Selected tag → filter diets (need tag→diet relationship; use `DietsViewModel.getTagsForDiet` or local map)
- **Search bar**: filter by name (existing)
- **Diet card**: expandable (tap header → toggle expanded)
  - Collapsed: name, description, macro pills, tags (up to 2 + "+N more")
  - Expanded action row: **View** | **Edit** | **Duplicate** | **Delete** (color-coded)
    - View → `DietDetailScreenNew(dietId: id, isReadOnly: true)`
    - Edit → `AddDietScreenNew(existingDiet:)` sheet
    - Duplicate → create Diet copy with name + " (Copy)", insertDiet, reload
    - Delete → confirm alert → `vm.deleteDiet(id:)`, reload

### 4b. DietDetailScreen — Read-only mode + delete/edit in view mode
- Add `isReadOnly: Bool = false` parameter to `DietDetailScreenNew`
- When `isReadOnly = true`: slot cards NOT tappable (remove Button wrapper)
- When `isReadOnly = false` (default): existing edit behavior preserved
- Top bar: when view mode (not editing), add Delete icon button → confirm → `vm.deleteDiet` → dismiss
- "View Log" button fix wired here too

---

## Phase 5 — Health + Grocery Screen Theme Matching
**Files**: `HealthMetricsScreen.swift`, `MoreScreen.swift`

### 5a. HealthMetricsScreen
- Add green gradient header card (matching HomeScreen `HomeHeaderSection` style):
  - Background: `Color(0xFF2E7D52)` → opacity gradient
  - Title: "Health Metrics" white bold
- Background: `Color(hex: "F0F9F4")` (consistent with home)
- Metric cards: white bg, 16dp corners, subtle shadow
- Macro/metric color tokens: same as home (orange carbs, blue protein, pink fat, green calories)

### 5b. GroceryListsScreen (in MoreScreen.swift)
- Add green navigation bar background (`.toolbarBackground(.visible, for: .navigationBar)` + `.toolbarBackground(Color(hex: "2E7D52"), for: .navigationBar)`)
- List row cards: white bg, 16dp corners, shadow (matching diet/meal card style)
- FAB or top-right "+" button: green background, white icon
- Progress bar: green tint
- Background: `Color(hex: "F0F9F4")`

---

## File Change Summary

| File | Phase | Change |
|---|---|---|
| `HomeScreen.swift` | 1,2 | Remove 3 sections; add legend; diet picker; slot detail; week tap |
| `DietDetailScreen.swift` | 1,4 | Fix view log; add isReadOnly; delete button |
| `DietsScreen.swift` | 4 | Full redesign with real data, tags, filters, action buttons |
| `DailyLogScreen.swift` | 3 | Top bar icons; ingredient ticks; remove Log Meal button |
| `FlowCollector.swift` | 2,3 | Add assignDiet/clearDiet/completeDay to DailyLogVM; assignDietForToday to HomeVM |
| `HealthMetricsScreen.swift` | 5 | Green header + theme tokens |
| `MoreScreen.swift` | 5 | GroceryListsScreen green theme + card styling |

---

## Unresolved Questions
1. DietDetailScreen "View Log" — navigate to DailyLogScreen for today or for the diet's assigned date? (assume today)
2. For tag filtering in DietsScreen — load tags per diet upfront or only show tags that exist on currently-loaded diets?
3. "Log All" slot button — log each food at its planned qty, or let user set multiplier?
4. Should "Complete Day" prevent adding more foods, or just mark as done?
5. Move sync button to profile — should we add it inside `ProfileScreen` or `MoreScreen` settings?
