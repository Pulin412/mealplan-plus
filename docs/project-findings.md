# MealPlan+ Project Findings (Current State)

## Project Snapshot
- Product: Meal planning + nutrition logging app, diabetes-oriented.
- Platforms: Android (`app`), iOS (`iosApp`), shared KMP module (`shared`).
- Runtime model: local-first SQLite persistence; optional online food lookup APIs.
- Build/tooling: Gradle KMP + Android app + Xcode iOS app.
- Scope in this doc: implemented behavior only (with explicit gaps).

## Architecture
- Module split:
  - `shared`: domain models, SQLDelight schema/queries, repositories, Ktor APIs, preferences abstraction.
  - `app`: Android UI (Jetpack Compose), Hilt DI, Android-specific repositories/DAOs, scanner/charts/export/import flows.
  - `iosApp`: SwiftUI UI and navigation; mix of shared-backed + local/sample-data-backed screens.
- Data layer:
  - Shared DB: SQLDelight (`MealPlanDatabase`) with platform drivers:
    - Android: `AndroidSqliteDriver`
    - iOS: `NativeSqliteDriver`
- UI/state:
  - Android: ViewModel + `StateFlow`, Navigation Compose, Hilt injection.
  - iOS: `@State`/`@StateObject` + `NavigationStack`/`TabView`.
- Networking:
  - Shared Ktor clients for OpenFoodFacts + USDA FoodData Central.
  - Android also contains Retrofit API clients for equivalent use-cases.

## Data Model (SQLDelight schema)

### Tables
| Table | PK | Key Relationships | Notes |
|---|---|---|---|
| `users` | `id` | parent for user-owned data | `email` unique/indexed |
| `food_items` | `id` | referenced by meal/log/grocery items | indexes: `name`, `barcode` |
| `meals` | `id` | FK `userId -> users` | optional `customSlotId` |
| `meal_food_items` | `(mealId, foodId)` | FKs to `meals`, `food_items` | meal-food junction |
| `custom_meal_slots` | `id` | FK `userId -> users` | user slot ordering |
| `diets` | `id` | FK `userId -> users` | diet template root |
| `diet_meals` | `(dietId, slotType)` | `dietId -> diets`, `mealId -> meals` | slot-to-meal mapping |
| `tags` | `id` | FK `userId -> users` | unique `(userId,name)` |
| `diet_tags` | `(dietId, tagId)` | FKs to `diets`, `tags` | diet-tag junction |
| `plans` | `(userId, date)` | FK `dietId -> diets` nullable | scheduled daily diet |
| `daily_logs` | `(userId, date)` | FK `userId -> users` | daily log root |
| `daily_log_slot_overrides` | `(userId, logDate, slotType)` | FK to `daily_logs`, optional `overrideMealId -> meals` | per-slot overrides |
| `logged_foods` | `id` | FK to `daily_logs`, `food_items` | individual food intake |
| `logged_meals` | `id` | FK to `daily_logs`, `meals` | meal intake entries |
| `custom_metric_types` | `id` | FK `userId -> users` | user-defined health metrics |
| `health_metrics` | `id` | FK `userId -> users` | typed/custom measurements |
| `grocery_lists` | `id` | FK `userId -> users` | date-range shopping lists |
| `grocery_items` | `id` | FK `listId -> grocery_lists`, optional `foodId -> food_items` | checklist items |

### Key relational/DB decisions
- Composite primary keys for daily domain boundaries: `plans(userId,date)`, `daily_logs(userId,date)`.
- FK delete policies:
  - `ON DELETE CASCADE` for ownership/junction cleanup.
  - `ON DELETE SET NULL` where references may be removed (`dietId`, `mealId`, `foodId`).
- Query performance: explicit indexes on high-selectivity columns (`userId`, `date`, `barcode`, join FKs).

## Feature Matrix (Android vs iOS)
Legend: `Y` implemented, `P` partial/basic, `N` not implemented.

| Capability | Android | iOS | Evidence |
|---|---:|---:|---|
| Auth (login/signup/logout/local state) | Y | Y | Android auth screens + prefs, iOS `AppState` |
| Food CRUD + browse/search | Y | P | iOS foods list uses `SeedDataLoader` sample path |
| Barcode scanner | Y | Y | `BarcodeScannerScreen` on both platforms |
| Online food lookup (OpenFoodFacts/USDA) | Y | P | iOS flow exists; many iOS lists still sample-backed |
| Meal CRUD/composition | Y | P | iOS meals use sample loader currently |
| Diet templates + tags | Y | P | iOS diet list/detail sample-backed |
| Daily logging (planned vs actual interactions) | Y | P | iOS daily log screen currently local sample state |
| Calendar/meal plan scheduling | Y | P | iOS meal-plan has ViewModels, mixed maturity |
| Grocery lists/detail/check-off | Y | P | iOS base list present, depth differs |
| Health metrics CRUD | Y | P | iOS has screens/tabs; Android has richer repository/UI paths |
| Charts/analytics | Y | P | Android charts VM + Vico; iOS home uses Swift Charts but not full parity |
| Import/export (CSV/JSON/CSV import conversion) | Y | P | Android full import/export utils; iOS lighter |

## Data-Based Decisions in Code
- Nutrition math/normalization:
  - Food macros normalized per 100g; runtime unit conversion via `FoodUnit` and `NutritionCalculator`.
  - Daily and diet totals computed by SQL aggregation and/or composed model calculations.
- Query-driven summaries:
  - Daily macro summary query in `DailyLog.sq` (`SUM(calories/protein/carbs/fat)` grouped by date).
  - Diet summary query in `Diet.sq` computes meal count + total calories.
- Naming/sorting decisions:
  - Natural sorting for `Diet-M1`, `Diet-R12`, `Diet-10` style names.
  - Calendar short label extraction (`Diet-M1 -> M1`, etc.).
- Seed strategy:
  - System foods seeded once (`isSystemFood=1`) from bundled JSON.
  - User-specific defaults (tags/diets/meals) seeded on signup (Android path).
- External data decision:
  - USDA client defaults to `DEMO_KEY` (rate-limited; suitable for testing, risky for production).

## Known Gaps / Risks
- Finding: iOS app includes many production-like screens, but several primary flows still load local sample data (`SeedDataLoader`) instead of persistent shared repository paths.
  - Impact: Product docs can overstate iOS parity if not flagged; analytics and CRUD expectations differ.
- Finding: Android contains a mixed persistence surface (legacy Room artifacts + shared SQLDelight layer).
  - Impact: Migration/maintenance complexity and documentation drift risk.
- Finding: Existing architecture/PRD docs claim broader parity than current code in some areas.
  - Impact: onboarding and QA may test against aspirational behavior, not actual behavior.
- Finding: USDA API key strategy uses `DEMO_KEY` by default.
  - Impact: throttling risk in production-like usage.

## User-Guide Ready Flows
- Auth onboarding:
  1. Sign up with email/password.
  2. App persists login state and user ID.
  3. User lands on main tab layout.
- Add food and reuse in meal:
  1. Create food manually or fetch via scanner/online search.
  2. Save nutrition values.
  3. Select food in meal composer with quantity/unit.
- Build a diet template:
  1. Create diet.
  2. Assign meals to slots.
  3. Optionally attach tags for filtering.
- Plan and log a day:
  1. Assign diet to calendar date.
  2. Log meals/foods by slot.
  3. Compare consumed macros against planned structure.
- Grocery flow:
  1. Create list (manual/date-range based depending on platform).
  2. Add/edit items and quantities.
  3. Check off purchased items.

## Resume TODOs
- [ ] `TODO-01` Owner: TBD | Status: Open | Validate every matrix row against latest iOS screen/viewmodel wiring.
- [ ] `TODO-02` Owner: TBD | Status: Open | Add direct file references per feature row for faster QA traceability.
- [ ] `TODO-03` Owner: TBD | Status: Open | Split this doc into `technical-reference.md` + `user-guide.md` while keeping this as index.
- [ ] `TODO-04` Owner: TBD | Status: Open | Add release-readiness checklist (prod API keys, telemetry, error handling, offline conflict policy).
- [ ] `TODO-05` Owner: TBD | Status: Open | Create parity tracker table by feature with acceptance criteria and target release.
