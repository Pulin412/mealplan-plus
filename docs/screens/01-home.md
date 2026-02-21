# Screen 1: Home

**Doc version**: 1.0 | **Status**: ✅ Android DONE | ✅ iOS DONE

---

## Design

Dark green (`#2E7D52`) header hero + light green (`#F0F9F4`) body. 7 sections stacked vertically.

| Section | Detail |
|---------|--------|
| Header | Green bg · greeting + bell + avatar · calorie target card with progress bar |
| Macro rings | 4 circular arcs: Carbs=orange, Protein=blue, Fat=pink, Calories=green |
| Quick Log Food | Full-width dark green button + book icon |
| This Week | S M T W T F S · today=filled green · logged=light green · future=grey |
| Blood Glucose | Fasting value · In Range badge · 7-day line chart · target 80–130 |
| Stats row | A1C (purple) · Weight (blue) · Day streak (orange) |
| Today's Plan | 4 slots: Breakfast/Lunch/Dinner/Snacks · emoji circle + logged count + progress bar |

**Bottom nav**: Home · Meal Plan · Log · Health · Grocery

---

## Data Sources

| UI element | Source |
|-----------|--------|
| User name/initial | `AuthRepository.getCurrentUser(userId)` |
| Calories consumed | `DailyLogRepository.getLogWithMeals(today).totalCalories` |
| Protein/carbs/fat | Same log |
| Calorie goal | Hardcoded 2000 |
| Week logged dates | `getCompletedDaysCalories(last7)` → dates where calories > 0 |
| Fasting glucose today | `HealthRepository.getMetricsForDate(today)` → FASTING_SUGAR |
| Glucose 7-day history | `HealthRepository.getMetricsByTypeInRange(FASTING_SUGAR, last7)` |
| A1C | `getMetricsForDate(today)` → HBA1C |
| Weight | `getMetricsForDate(today)` → WEIGHT |
| Day streak | Consecutive days with calories > 0 from weekly data |
| Meal slot progress | `getLogWithMeals(today)` grouped by slotType; totals B=3 L=3 D=3 S=2 |

---

## Files

### Android

| File | Change |
|------|--------|
| `app/.../home/HomeViewModel.kt` | Added `userName`, `userInitial`, `calorieGoal`, `latestHba1c`, `glucoseHistory`, `dayStreak`, `weeklyLoggedDates`, `todayPlanSlots`; new `MealSlotProgress` data class; `loadUserName()`, `loadGlucoseHistory()`, `loadTodaySlots()`, `computeStreak()` |
| `app/.../home/HomeScreen.kt` | Full rewrite — 7 sections matching Figma |
| `app/.../navigation/NavHost.kt` | Added `NavigationBar` with 5 tabs: Home · Meal Plan · Log · Health · Grocery |

### iOS

| File | Change |
|------|--------|
| `iosApp/.../Utils/FlowCollector.swift` | Added `HomeViewModel` with async load of all home data |
| `iosApp/.../Screens/Home/HomeScreen.swift` | Full rewrite — 7 sections matching Figma, Swift Charts glucose chart |
| `iosApp/.../Navigation/AppNavigation.swift` | `MainTabView` updated to 5 Figma tabs |

---

## Verification

- [ ] Green header shows greeting with correct name
- [ ] Calorie progress bar updates from real log data
- [ ] 4 macro rings show correct values + percentages
- [ ] Quick Log Food button visible
- [ ] Week calendar: today filled green, logged days tinted, future grey
- [ ] Blood glucose card shows fasting value + In Range badge + line chart
- [ ] Stats row: A1C / Weight / Day streak with real data
- [ ] Today's Plan: 4 slots with correct logged count + progress bars
- [ ] Bottom nav: Home · Meal Plan · Log · Health · Grocery
- [ ] Tapping bottom nav tabs navigates correctly
