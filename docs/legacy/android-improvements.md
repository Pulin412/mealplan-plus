# Android Improvements

Tracks UI/UX and feature improvements made to the Android app.
Each entry notes what changed and the file(s) affected.

---

## 2026-03-29 — Firebase Analytics (P2)

**Branch:** `feature/analytics`

### What changed

Added Firebase Analytics for user journey tracking before public launch. All analytics flows through an injectable wrapper so call sites are decoupled from the SDK.

**Files added:**
- `android/src/main/java/com/mealplanplus/util/AnalyticsEvent.kt` — Plain string constants for all event names and parameter keys. Contract tests enforce exact values so renaming silently won't break dashboards.
- `android/src/main/java/com/mealplanplus/util/AnalyticsManager.kt` — Injectable `@Singleton` wrapper around `FirebaseAnalytics`. Provides typed log methods for each event, `setUserId`/`clearUserId`, `setUserProperty`, and `logScreenView`.

**Files modified:**
- `android/build.gradle.kts` — Added `firebase-analytics-ktx` under the existing Firebase BOM.
- `MealPlanApp.kt` — Injects `AnalyticsManager` (no-op at app level; events fired from repositories/screens).
- `AuthRepository.kt` — `setUserId`/`clearUserId` + event logging on sign-in (email + Google), sign-up, and sign-out.

### Events tracked

| Event | When fired | Key params |
|---|---|---|
| `sign_in` | Successful email or Google sign-in | `provider` |
| `sign_up` | Successful email registration | `provider` |
| `sign_out` | User signs out or deletes account | — |
| `food_searched` | User searches for a food | `search_query`, `source` |
| `food_added` | Food item added to a meal | `food_name`, `source` |
| `barcode_scanned` | Barcode scan attempted | `success` |
| `diet_created` | New diet saved | `diet_id` |
| `diet_viewed` | Diet detail screen opened | `diet_id` |
| `meal_plan_viewed` | Calendar/meal plan screen opened | — |
| `grocery_list_created` | Manual grocery list created | — |
| `grocery_list_generated` | Grocery list auto-generated from diet | — |
| `health_metric_logged` | Health metric recorded | `metric_type` |

### Tests added (TDD)

| File | Tests | What's covered |
|---|---|---|
| `AnalyticsEventTest` | 24 | All 12 event name constants, all 8 param constants, uniqueness, count guards |
| `AnalyticsManagerTest` | 18 | All typed log methods delegate to `FirebaseAnalytics.logEvent` with correct event name; `setUserId`/`clearUserId`/`setUserProperty`; `logScreenView` uses `SCREEN_VIEW` |
| `AuthRepositoryTest` | Extended | Analytics `setUserId` + event logged alongside Crashlytics on sign-in, sign-up, sign-out |

### Notes
- Analytics is free on the Firebase Spark plan — no per-event billing.
- `setUserId(null)` clears the identity on sign-out (Firebase requirement for GDPR compliance).
- `logScreenView` is available for explicit screen tracking in Compose (where automatic Firebase screen tracking doesn't fire).
- To add a new event: add constants to `AnalyticsEvent`, add a typed method to `AnalyticsManager`, update `AnalyticsEventTest` count guard.

---

## 2026-03-29 — Firebase Remote Config / Feature Flags (P1)

**Branch:** `feature/remote-config`

### What changed

Added Firebase Remote Config so feature flags can be toggled remotely without an app release. All flags have safe local defaults applied before the first fetch.

**Files added:**
- `android/src/main/java/com/mealplanplus/util/FeatureFlag.kt` — Enum of all feature flags. Each entry holds a Remote Config key and a boolean default. Adding a flag here automatically registers its default in `RemoteConfigManager.applyDefaults()`.
- `android/src/main/java/com/mealplanplus/util/RemoteConfigManager.kt` — Injectable `@Singleton` wrapper around `FirebaseRemoteConfig`. Provides `applyDefaults()`, `fetchAndActivate()`, `isEnabled(FeatureFlag)`, `getString()`, `getLong()`, `getDouble()`. Fetch failures are caught and logged — cached/default values remain available.

**Files modified:**
- `android/build.gradle.kts` — Added `firebase-config-ktx` under the existing Firebase BOM.
- `MealPlanApp.kt` — Injects `RemoteConfigManager`; calls `applyDefaults()` synchronously then `fetchAndActivate()` in a background coroutine on startup.

### Feature flags

| Flag | Key | Default | Purpose |
|---|---|---|---|
| `BARCODE_SCANNER` | `barcode_scanner_enabled` | `true` | Kill-switch for CameraX/MLKit barcode flow |
| `USDA_SEARCH` | `usda_search_enabled` | `true` | USDA FoodData Central API search |
| `OFF_SEARCH` | `off_search_enabled` | `true` | OpenFoodFacts search + barcode lookup |
| `SYNC` | `sync_enabled` | `false` | Background sync — off until backend is live |
| `GROCERY_AUTO_GENERATE` | `grocery_auto_generate_enabled` | `true` | Auto-generate grocery lists from diet |
| `HEALTH_TRACKING` | `health_tracking_enabled` | `true` | Health metrics tracking screen |

### Tests added

| File | Tests | What's covered |
|---|---|---|
| `FeatureFlagTest` | 14 | Key names, default values, uniqueness, count guard |
| `RemoteConfigManagerTest` | 13 | `applyDefaults` (map contents + all keys), `fetchAndActivate` (true/false/exception), `isEnabled` (delegation + key correctness for each flag), `getString`/`getLong`/`getDouble` |

### Notes
- Remote Config is free on the Firebase Spark plan — no per-event billing.
- `fetchAndActivate` uses Firebase's built-in 12-hour minimum fetch interval to avoid quota issues.
- Fetch failure is non-fatal: the app continues with local defaults (or last-cached values) — no user-visible impact.
- To add a new flag: add an entry to `FeatureFlag` enum — defaults and Remote Config registration are automatic.

---

## 2026-03-29 — Firebase Crashlytics (P1)

**Branch:** `feature/crashlytics`

### What changed

Added Firebase Crashlytics for crash visibility before public launch. All crashes are automatically captured; key non-fatal errors and user-journey breadcrumbs are reported explicitly.

**Files added:**
- `android/src/main/java/com/mealplanplus/util/CrashlyticsReporter.kt` — Injectable singleton wrapper around `FirebaseCrashlytics`. Provides `recordNonFatal()`, `log()`, `setUserId()`, `clearUserId()`, and `setKey()`. Keeps call sites decoupled from the Firebase SDK and makes unit testing straightforward.

**Files modified:**
- `build.gradle.kts` (root) — Added `com.google.firebase.crashlytics` Gradle plugin declaration (`v3.0.2`).
- `android/build.gradle.kts` — Applied `com.google.firebase.crashlytics` plugin; added `firebase-crashlytics-ktx` under the existing Firebase BOM.
- `MealPlanApp.kt` — Injects `CrashlyticsReporter`; enables collection on startup and logs `app_start` breadcrumb.
- `AuthRepository.kt` — Sets Crashlytics user ID on sign-in (email + Google), clears on sign-out/account deletion. Reports non-fatals for unexpected auth/seed exceptions.
- `SyncRepository.kt` — Non-fatal on push/pull failures; breadcrumb on successful sync.
- `OpenFoodFactsRepository.kt` — Non-fatal on barcode lookup and product search failures.
- `UsdaFoodRepository.kt` — Non-fatal on search and detail-fetch failures.

### Breadcrumbs logged
| Event | Detail |
|---|---|
| `app_start` | `version=<versionName>` |
| `sign_in` | `provider=email\|google` |
| `sign_up` | `provider=email` |
| `sign_out` | — |
| `account_deleted` | — |
| `sync_push` | `accepted=<n>` |
| `sync_pull` | item counts per entity type |

### Non-fatals reported
| Context key | Trigger |
|---|---|
| `sign_in_email` / `sign_in_google` | Unexpected exception during sign-in |
| `sign_up_email` | Unexpected exception during sign-up |
| `sign_up_seed` / `google_sign_in_seed` | Failure seeding initial data for new user |
| `password_reset` | Unexpected exception sending reset email |
| `update_profile` | DB exception updating user profile |
| `delete_account` | Exception during account deletion |
| `sync_push` / `sync_pull` | Network/DB error during background sync |
| `off_barcode` / `off_search` | OpenFoodFacts API failure |
| `usda_search` / `usda_details` | USDA API failure |

### Notes
- Crashlytics is free on the Firebase Spark plan — no per-event billing.
- `CrashlyticsReporter` is a regular `@Singleton` injected via Hilt; no static access in call sites.
- If a user opt-out preference is added later, pass that flag to `setCrashlyticsCollectionEnabled()` in `MealPlanApp.initCrashlytics()`.

---

## 2026-03-27 — iOS Parity

iOS parity implemented on branch `feature/ios-android-parity` for all changes below.

### Shared KMP module — SERVING unit added
**Files:** `shared/.../FoodUnit.kt`, `shared/.../FoodItem.kt`

**What changed:**
- Added `SERVING("Servings", "srv")` to the shared KMP `FoodUnit` enum (was missing; only existed in the Android-only enum)
- `FoodItem.toGrams()` in shared now handles `FoodUnit.SERVING → quantity × 100.0`
- `PIECE` label updated to "Pieces" / shortLabel "pcs" to match Android

### iOS — Meal Plan calendar dots fix
**File:** `ios/.../MealPlanScreen.swift`

- `loadData()` range extended ±6 days (matches Android `CalendarViewModel` fix)
- Week navigation prev/next buttons now check month boundary: if the new week lands in a different month, `currentMonth` is updated and `loadData()` is called to reload plans for the new month
- `slotInfo(for:)` and `slotDisplayName(_:)` now handle `CUSTOM:` prefix for custom slots

### iOS — Meal Plan tappable slots
**File:** `ios/.../MealPlanScreen.swift`

- `MealPlanSlotRow` gains `showChevron: Bool = false` — shows chevron `>` when meal has ingredients
- `dietDetailSection` now includes custom slots (keys with `CUSTOM:` prefix from `mealsMap`)
- Tapping a slot with ingredients presents `MealPlanDetailSheet` (read-only, `readOnly = true`)
- `MealPlanDetailSheet`: macro tiles + A–Z / Qty sort chips + ingredients list with unit-aware quantity display
- Added `IdentifiableMealWrapper` struct for `sheet(item:)` presentation

### iOS — HomeMealDetailSheet overhaul (Meal Detail checklist)
**File:** `ios/.../HomeScreen.swift`

- Unit display fixed: `"\(Int(quantity))g"` → unit-aware string (`g`, `ml`, `srv`, `pcs`)
- Added ingredient **checkboxes** — tap row or checkbox to mark as prepared; checked rows get strikethrough + dimmed
- Added **A–Z / Qty** sort chips
- Added **progress bar** ("X of N prepared") with "Clear all" button

### iOS — Create Diet + custom meal slots
**Files:** `ios/.../DietDetailScreen.swift`

- `dietSlotDisplayName()` and `dietSlotIcon()` now handle `CUSTOM:` prefix:
  - Display name: splits by `_`/space, title-cases each word (e.g. `CUSTOM:EVENING_TEA` → "Evening Tea")
  - Icon: `star.fill` for custom slots
- `mealSlotsSection()` now includes custom slots from `mealsMap` (those with `CUSTOM:` prefix)
- **"Add Custom Meal Slot"** button shown in edit mode (purple dashed border); opens alert to type a name
- Custom slot name saved as `CUSTOM:<name>` slotType — no DB migration needed
- `MealSlotCardNew` and `EmptyMealSlotCard` accept `isCustom: Bool` — shows a purple "custom" badge next to the slot label

### iOS — Food picker: Pieces + Servings units + correct unit storage
**Files:** `ios/.../AddMealScreen.swift`, `ios/.../FoodsScreen.swift`, `ios/.../Data/DataImporter.swift`

- `FoodItemUI` gains `quantity: Double = 100.0` and `unitKmpName: String = "GRAM"` (backward-compatible defaults)
- `FoodPickerScreen` no longer immediately adds food; tapping a food now shows `FoodQuantitySheetView`
- `FoodQuantitySheetView`:
  - Segmented unit picker: Grams / ml / Servings / Pieces
  - Quantity text field + quick-select chips (50/100/150/200g, 50/100/200/250ml, 0.5/1/1.5/2 srv, 1/2/3/4 pcs)
  - Live macro preview (calories/protein/carbs/fat) calculated from actual quantity × unit
  - "1 piece ≈ Xg" hint when Pieces is selected
- `populateIfEditing()` now stores actual quantity + unit in `FoodItemUI` (was always "g")
- `saveMeal()` now calls `addFoodToMeal(quantity: food.quantity, unit: foodUnitFromKmpName(food.unitKmpName))` instead of hardcoded `100.0, .gram`
- `DataImporter.parseUnit()` updated to map "serving/srv" → `.serving`, "slice" → `.slice`, "scoop" → `.scoop`

---

## 2026-03-27 (3)

### Meal Plan calendar — dots missing on future weeks / cross-month navigation
**File:** `CalendarViewModel.kt`

**Root cause:** `loadPlansForMonth()` queried only `month.atDay(1)` → `month.atEndOfMonth()`. Week navigation calls only `selectDate()`, never updating `currentMonth` or reloading plans — so navigating into April while `currentMonth` was still March returned no plans for April, leaving all dots blank.

**Fixes:**
1. `selectDate()` now checks `YearMonth.from(date) != currentMonth`; when the selected week crosses into a new month it updates `currentMonth` and triggers a reload
2. `loadPlansForMonth()` range extended to `month.atDay(1).minusDays(6)` → `month.atEndOfMonth().plusDays(6)` — covers weeks that straddle two months (e.g. March 29–April 4 all get dots even when currentMonth = April)

---

## 2026-03-27 (2)

### Food picker — Pieces unit + correct unit display everywhere
**Files:** `FoodUnit.kt`, `FoodItem.kt`, `FoodPickerScreen.kt`, `NavHost.kt`, `DietDetailScreen.kt`, `DietMealSlotScreen.kt`, `AddMealScreen.kt`, `EditMealScreen.kt`, `DietDetailViewModel.kt`, `DietMealSlotViewModel.kt`, `AddMealViewModel.kt`, `EditMealViewModel.kt`

**What changed:**

**New unit — Pieces:**
- Added `PIECE("Pieces")` to the `MeasureUnit` dropdown in the food picker bottom sheet
- Quick-select chips for Pieces: 1 / 2 / 3 / 4
- Hint text shows "1 piece ≈ Xg" when `gramsPerPiece` is set on the food, or "1 piece ≈ ~100g" as fallback
- `gramsPerPiece` is now passed from `FoodPickerScreen` into `FoodDetailBottomSheet` so the macro preview is accurate

**New unit — Servings (stored correctly):**
- Added `SERVING("Servings", "srv")` to `FoodUnit` enum (was missing; existed only in the UI `MeasureUnit` but was never persisted)
- `FoodItem.toGrams` now handles `FoodUnit.SERVING → quantity × 100.0` (servingSize is always 100g)

**Root-cause fix — unit was always stored as GRAM:**
- `FoodDetailBottomSheet.onConfirm` was `(Double)` — only passed a multiplier, losing the selected unit
- Changed to `onConfirm: (Double, FoodUnit)` — passes the actual quantity and the correct `FoodUnit`
- The "Add to Meal" button maps `MeasureUnit → FoodUnit` and calls `onConfirm(quantity, foodUnit)`
- Previously, "2 servings of eggs" was stored as `quantity=2, unit=GRAM` → displayed as "2g"
- Now stored as `quantity=2, unit=SERVING` → displays as "2 srv"; "2 pieces" → "2 pcs"

**Macro preview fix:**
- Old multiplier formula for SERVING was `quantity × 1` (treating servings as raw 100g-based multiplier)
- New formula: `quantity × servingSize / 100` — all units now compute correct gram-equivalent for the preview

**Plumbing changes:**
- `FoodPickerScreen.onFoodSelected` / `onUsdaFoodSelected` now include `FoodUnit` as third param
- NavHost saves `"selected_unit"` (enum name string) into `savedStateHandle` for all 3 `FoodPickerScreen` routes
- All savedStateHandle consumers read `"selected_unit"` back and pass it to their ViewModels
- All ViewModel `addFoodById` / `addUsdaFood` methods now accept `unit: FoodUnit = FoodUnit.GRAM`

---

## 2026-03-27

### Meal Detail Screen — checklist, sorting, proper sizing
**Files:** `MealDetailScreen.kt`, `MealDetailViewModel.kt`

**What changed:**
- Ingredient rows now have a **Checkbox** — tap a row or the checkbox to mark it as prepared
- Checked items get a strikethrough name and dimmed colour to visually distinguish done vs pending
- **Sort chips** (A–Z / Qty) at the top of the ingredients card let the user reorder the list alphabetically or by quantity descending
- **Progress bar** below the sort chips shows "X of N prepared" with a "Clear all" shortcut
- All text sizes bumped up: food name uses `bodyLarge`, quantity uses `bodyMedium`, macro totals use `titleMedium` — previously everything was `bodySmall`/`labelSmall`
- Section headers ("Instructions", "Ingredients", "Totals") use `titleMedium` instead of `titleSmall`
- Macro values per ingredient are displayed inline on the right side of each row (kcal prominent, P/C/F as small pills) rather than in a dense table with columns
- Totals icons use `headlineSmall` for better visual weight

**ViewModel additions:**
- `IngredientSortOrder` enum (`ALPHABETICAL`, `QUANTITY`)
- `checkedFoodIds: Set<Long>` in `UiState`
- `toggleChecked(foodId)` and `setSortOrder(order)` actions
- `sortedFoods` derived list kept in state (sorted on load + on sort change)

---

### Meal Plan screen — tappable meal slots with read-only ingredient view
**Files:** `CalendarScreen.kt`, `MealDetailScreen.kt`, `NavHost.kt`

**What changed:**
- Meal slot rows in the Meal Plan (Calendar) screen are now tappable when the slot has ingredients — shows a chevron `>` on the right
- Tapping navigates to the same `MealDetailScreen` with `readOnly = true` — no checkboxes, no progress bar, ingredients are display-only with sorting still available
- Slots with no meal or empty ingredients remain non-interactive (no chevron)
- `MealSlotRow` got minor size bumps: 36dp icon circle, `labelMedium` slot label, `FontWeight.Medium` for meal name

**Route change:**
- `Screen.MealDetail` now accepts an optional `?readOnly=true/false` query param
- Home screen navigates with `readOnly = false` (checklist behaviour unchanged)
- Calendar screen navigates with `readOnly = true`

---

### Create Diet flow — simplified form + custom meal slots
**Files:** `AddDietScreen.kt`, `AddDietViewModel.kt`, `DietDetailScreen.kt`, `DietDetailViewModel.kt`, `DietFormComponents.kt`, `CalendarScreen.kt`, `HomeViewModel.kt`, `NavHost.kt`

**What changed:**

**Create Diet screen (AddDiet):**
- Removed the broken "Estimated Totals" card and all pre-populated slot sections — the form now only shows diet name, description, and tags
- On Save, instead of going back to the list, the app navigates directly to the Diet Detail screen in edit mode so the user can immediately add foods to each slot

**Custom meal slots:**
- In Diet Detail edit mode, a new "Add Custom Meal Slot" button appears below all default slots
- Tapping it opens a dialog to enter any name (e.g. "Pre-Sleep", "Evening Tea")
- Custom slots are stored as `DietMeal` rows with `slotType = "CUSTOM:<name>"` — no new table or migration required
- Custom slots show an `⭐` icon and a "custom" badge to distinguish them from default slots
- Each custom slot supports: adding foods, instructions, remove-slot (in edit mode), and tap-to-view-ingredients (in view mode)
- Custom slots appear everywhere the diet is shown:
  - **Diet Detail screen** — view and edit mode
  - **Home screen Today's Plan** — display name extracted from `"CUSTOM:"` prefix (was showing garbled `"custom:pre-sleep"` before)
  - **Meal Plan (Calendar) screen** — new `CustomMealSlotRow` composable renders them with a purple "custom" badge; tappable if they have ingredients

**ViewModel changes:**
- `AddDietViewModel` now only handles name/description/tags; removed all food/slot state
- `DietDetailViewModel.currentPickingSlot: DefaultMealSlot?` replaced with `currentPickingSlotType: String?` (unified for default and custom slots)
- New actions: `addCustomSlot(name)`, `removeCustomSlot(slotType)`, `setPickingSlotType(slotType)`, `removeFoodFromSlot(slotType, item)`, `incrementQtyInSlot`, `decrementQtyInSlot`, `updateSlotInstructionsForType`
- `loadDiet()` now also extracts `customSlotTypes: List<String>` from `dietWithMeals.meals` keys
