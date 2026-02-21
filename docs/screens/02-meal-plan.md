# Screen 2: Meal Plan

**Branch**: `feature/screen-meal-plan`
**Status**: ✅ Android DONE | ✅ iOS DONE

---

## Design

Dark green header (`#2E7D52`) + white "Meal Plan" title + "✨ Select Diet" pill (top-right, assigns to today).
Calendar card (white, rounded) + Month/Week toggle + selected-date detail panel.

---

## Day Cell States (4)

| State | Bg | Text |
|-------|----|------|
| Completed | `#2E7D52` dark green | white |
| Planned | `#FFFDE7` yellow | black, orange dot |
| Today | transparent + green border | dark green |
| No plan | transparent | grey dot |

---

## Panel States (4)

| State | Trigger | Bg | Actions |
|-------|---------|-----|---------|
| A — Today + diet | today selected, diet exists | light green | "View Log >" |
| B — Future + diet | future date, diet exists | yellow | "Change" + "✕ Remove diet" |
| C — Future no diet | future date, no diet | yellow | "+ Plan" + "+ Plan a Diet" |
| D — Past + diet | past date, diet exists | light green | "View Log >" only |

---

## Data Sources

| Element | Source |
|---------|--------|
| Plans for month | `planRepository.getPlansWithDietNameSnapshot(userId, start, end)` |
| Diet details (macros, meals) | `dietRepository.getDietWithMeals(dietId)` |
| First tag | `dietRepository.getTagsForDiet(dietId)` → `first` |
| Diet list for picker | `dietRepository.getDietSummariesSnapshot(userId)` |
| All meal slots | `DefaultMealSlot` enum (11 slots) |

---

## Macro Tiles

Shows `totalCalories`, `totalProtein`, `totalCarbs`, `totalFat` from `DietWithMeals` (computed, sums all assigned meals).

---

## Meal Slots

All 11 `DefaultMealSlot` values always shown, even if not in diet:

| Slot | Emoji |
|------|-------|
| EARLY_MORNING | 🌙 |
| BREAKFAST | 🌅 |
| NOON | ☀️ |
| MID_MORNING | 🥤 |
| LUNCH | ☀️ |
| PRE_WORKOUT | 💪 |
| EVENING | 🌆 |
| EVENING_SNACK | 🍎 |
| POST_WORKOUT | 🏋️ |
| DINNER | 🌙 |
| POST_DINNER | 🍵 |

Slot with assigned meal → meal name. No meal → "—".

---

## Diet Type / Tags

Shows **first tag only** (purple chip, `#7B1FA2` bg). If no tags, tag row hidden.

---

## Diet Picker Trigger Points

| Trigger | Assigns to |
|---------|-----------|
| "✨ Select Diet" top-right button | today |
| "Change" in future+diet panel | selected date |
| "+ Plan" / "+ Plan a Diet" | selected date |

---

## Files Changed

### Android

| File | Change |
|------|--------|
| `app/.../calendar/CalendarViewModel.kt` | Added `isWeekView`, `selectedDietWithMeals`, `selectedDietTags`, `toggleView()`, `loadDietDetails()` |
| `app/.../calendar/CalendarScreen.kt` | Full rewrite — dark green header, 4-state panel, macros, 11 slots |
| `app/src/test/.../CalendarViewModelTest.kt` | NEW — 6 unit tests |

### iOS

| File | Change |
|------|--------|
| `iosApp/.../Utils/FlowCollector.swift` | Extended `PlansViewModel` — added `selectedDiet`, `selectedDietWithMeals`, `selectedDietTags`, `isWeekView`, `selectDate()`, `loadDietDetails()`, `assignDiet()`, `removeDiet()`, `toggleView()` |
| `iosApp/.../Screens/Calendar/MealPlanScreen.swift` | NEW — full screen |
| `iosApp/.../Screens/Calendar/DietPickerSheet.swift` | NEW — diet picker sheet |
| `iosApp/.../Screens/More/MoreScreen.swift` | Removed old `CalendarScreen` + `CalendarDayCell` stubs; removed Calendar nav link |
| `iosApp/.../Navigation/AppNavigation.swift` | `MealPlanTab` now renders `MealPlanScreen` |
| `iosApp/iosAppTests/MealPlanViewModelTests.swift` | NEW — 4 test classes (panel state, short name, toggle, plans map) |

---

## Verification

```bash
# Android build
./gradlew :app:assembleDebug

# Android tests
./gradlew :app:test --tests "*.CalendarViewModelTest"

# iOS (Xcode)
# MealPlan tab → Month/Week toggle works
# Tap date → correct panel state (today/future/past × diet/no diet)
# Assign diet → cell turns yellow/green, macros + all 11 slots shown
# Remove diet → cell clears, empty state shown
# "✨ Select Diet" → assigns to today
```
