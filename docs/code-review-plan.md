# MealPlan+ — Holistic Code Review & Improvement Plan

> Reviewed: May 7, 2026  
> Scope: Backend (Spring Boot), Web App (Next.js), Android (Kotlin/Compose), Database (Postgres/Room)  
> Perspective: Android + Webapp as two independent channels sharing the backend as the source of truth.

---

## Summary

The codebase is well-structured and the core architecture decisions are sound: a single Spring Boot backend as the source of truth, Firebase JWT auth without the Admin SDK, delta sync with last-write-wins, and fully independent clients. The findings below are divided by priority and layer. Each section ends with a concrete action item.

---

## 1. Backend — Critical Bugs

### 1.1 `isFavorite` on system foods is a shared-state bug

**File:** `backend/src/main/kotlin/com/mealplanplus/api/domain/food/Food.kt`

`Food` has `var isFavorite: Boolean` as a column on the shared `foods` row. System foods (`isSystemFood = true`) are shared across all users. When any user favorites a system food, `isFavorite` is set to `true` for everyone. 

**Fix:** Extract user-specific food preferences into a separate table:

```sql
-- V10__food_user_prefs.sql
CREATE TABLE food_user_prefs (
    firebase_uid  VARCHAR(255) NOT NULL,
    food_id       BIGINT       NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    is_favorite   BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (firebase_uid, food_id)
);
```

Then `FoodService.toggleFavorite` writes to this table, and `FoodService.list` LEFT JOINs it to populate `isFavorite` per requesting user.

---

### 1.2 Android push uses positional index to map response server IDs — fragile

**File:** `android/src/main/java/com/mealplanplus/data/repository/SyncRepository.kt` (lines 116–130)

```kotlin
resp.foods.forEachIndexed { i, dto ->
    rawFoods.getOrNull(i)?.let { foodDao.updateFood(it.copy(serverId = dto.serverId ?: it.serverId, ...)) }
}
```

This relies on the server returning foods in the exact same order they were sent. The backend `SyncController.push` does `req.foods.map { foodService.upsert(it, ...) }` which preserves order today, but it's an implicit contract. If the backend ever batches, reorders, or deduplicates, the client writes the wrong `serverId` to the wrong local row.

**Fix:** The `PushResponse` already returns `FoodDto` with `serverId`. Match by `serverId` instead of index:

```kotlin
val serverIdToDto = resp.foods.associateBy { it.serverId }
rawFoods.forEach { food ->
    val dto = serverIdToDto[food.serverId] ?: return@forEach
    foodDao.updateFood(food.copy(serverId = dto.serverId ?: food.serverId, syncedAt = now))
}
```

---

### 1.3 Android pull does not handle `food` and `daily_log` tombstones

**File:** `android/src/main/java/com/mealplanplus/data/repository/SyncRepository.kt` (lines 262–273)

The tombstone handler only covers `meal`, `diet`, `health_metric`, `grocery_list`. Tombstones for `food` and `daily_log` are silently ignored. If a food is deleted on the web app, the Android app never removes it.

**Fix:** Add the missing cases to the `when` block:

```kotlin
"food" -> foodDao.getAllFoodsOnce().find { it.serverId == t.serverId }
    ?.let { foodDao.deleteFood(it) }
"daily_log" -> dailyLogDao.getLogByServerId(t.serverId)
    ?.let { dailyLogDao.deleteLog(it) }
```

---

### 1.4 Workout sync is absent from Android `SyncRepository`

**File:** `android/src/main/java/com/mealplanplus/data/repository/SyncRepository.kt`

The backend's `SyncController` fully supports workout push/pull (exercises and sessions), but `SyncRepository.push()` never includes workouts, and `pull()` ignores `resp.exercises` and `resp.workoutSessions`. All workout data entered on Android is never synced.

**Fix:** Add workout push in Step 1 and workout upsert handling in pull, matching the food/meal patterns already in place.

---

## 2. Backend — Service Layer Duplication

### 2.1 Identical `upsert` pattern across 6+ services

Every service (`FoodService`, `MealService`, `DietService`, `DailyLogService`, `HealthMetricService`, `GroceryService`, `WorkoutService`) implements the same 3-step `upsert` logic:

1. Look up by `serverId` → if null, `create()`
2. If `dto.updatedAt <= existing.updatedAt` → return existing (last-write-wins)
3. Delete children, reconstruct entity, `save()`

This pattern is copy-pasted verbatim. Consider extracting the guard into a shared utility:

```kotlin
// In SyncableEntity companion or a top-level util
fun <T> upsertGuard(
    existing: T?,
    dtoUpdatedAt: Instant?,
    existingUpdatedAt: Instant,
    onCreate: () -> T,
    onUpdate: () -> T,
    onSkip: () -> T
): T {
    if (existing == null) return onCreate()
    if ((dtoUpdatedAt ?: Instant.EPOCH) <= existingUpdatedAt) return onSkip()
    return onUpdate()
}
```

Or, introduce an abstract `SyncableService<Entity, Dto>` base class with the common upsert scaffold.

---

### 2.2 Entity reconstruction instead of field update in all `update`/`upsert` methods

All update and upsert methods construct a brand-new entity object from scratch with every field manually copied. Example from `MealService.upsert`:

```kotlin
val updated = Meal(id = existing.id, firebaseUid = existing.firebaseUid, name = dto.name, slot = dto.slot)
    .also { it.serverId = existing.serverId }
```

This pattern:
- Is verbose and error-prone (easy to forget a field)
- Bypasses JPA dirty checking (forces a full UPDATE even if nothing changed)
- Makes it easy to accidentally lose a field when adding new columns

**Fix:** Either make entities `@Entity data class` and use `copy()`, or use `@Transactional` + direct field mutation via `@ManyToOne` JPA managed state.

---

### 2.3 `require(... firebaseUid == ...)` throws `IllegalArgumentException`, not `403`

Every service has:

```kotlin
require(meal.firebaseUid == firebaseUid) { "Forbidden" }
```

`require()` throws `IllegalArgumentException` which Spring maps to HTTP 400, not 403. And the string "Forbidden" doesn't help the client. `GroceryService` already uses `ResponseStatusException` correctly for one case. Standardize on:

```kotlin
if (meal.firebaseUid != firebaseUid) 
    throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
```

---

## 3. Database — Missing Constraints and Indexes

### 3.1 No FK constraints on child tables

The V1 DDL creates all child tables (`meal_food_items`, `diet_meals`, `logged_foods`, `grocery_items`, `workout_sets`, `template_exercises`) without `REFERENCES` clauses or `ON DELETE CASCADE`. Deleting a parent row silently orphans all children.

```sql
-- V10__add_fk_constraints.sql (or alongside food_user_prefs)
ALTER TABLE meal_food_items
    ADD CONSTRAINT fk_mfi_meal   FOREIGN KEY (meal_id)  REFERENCES meals(id)  ON DELETE CASCADE,
    ADD CONSTRAINT fk_mfi_food   FOREIGN KEY (food_id)  REFERENCES foods(id)  ON DELETE CASCADE;

ALTER TABLE diet_meals
    ADD CONSTRAINT fk_dm_diet    FOREIGN KEY (diet_id)  REFERENCES diets(id)  ON DELETE CASCADE,
    ADD CONSTRAINT fk_dm_meal    FOREIGN KEY (meal_id)  REFERENCES meals(id);

ALTER TABLE logged_foods
    ADD CONSTRAINT fk_lf_log     FOREIGN KEY (daily_log_id) REFERENCES daily_logs(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_lf_food    FOREIGN KEY (food_id)      REFERENCES foods(id);

ALTER TABLE grocery_items
    ADD CONSTRAINT fk_gi_list    FOREIGN KEY (grocery_list_id) REFERENCES grocery_lists(id) ON DELETE CASCADE;

ALTER TABLE workout_sets
    ADD CONSTRAINT fk_ws_session FOREIGN KEY (session_id) REFERENCES workout_sessions(id) ON DELETE CASCADE;

ALTER TABLE template_exercises
    ADD CONSTRAINT fk_te_template FOREIGN KEY (template_id) REFERENCES workout_templates(id) ON DELETE CASCADE;
```

### 3.2 Missing indexes on FK columns of child tables

Without indexes on the FK side, every `findByMealId`, `findByDailyLogId`, etc. is a full table scan.

```sql
-- Add to the same migration
CREATE INDEX idx_mfi_meal_id       ON meal_food_items   (meal_id);
CREATE INDEX idx_dm_diet_id        ON diet_meals         (diet_id);
CREATE INDEX idx_lf_daily_log_id   ON logged_foods       (daily_log_id);
CREATE INDEX idx_gi_grocery_list_id ON grocery_items     (grocery_list_id);
CREATE INDEX idx_ws_session_id     ON workout_sets       (session_id);
CREATE INDEX idx_te_template_id    ON template_exercises (template_id);
CREATE INDEX idx_tombstones_sid    ON tombstones         (server_id);
CREATE INDEX idx_health_uid_type   ON health_metrics     (firebase_uid, type, recorded_at DESC);
```

### 3.3 `tags` had no `firebase_uid` until V9 — existing rows got empty string default

V9 added `firebase_uid` to `tags` with `DEFAULT ''`. Any tags created before this migration have `firebase_uid = ''` in production. `TagService.listTags(firebaseUid)` calls `findByFirebaseUid(firebaseUid)` — those legacy tags are unreachable by any user. A one-time fix script should assign them to the correct user.

### 3.4 `Meal.slot` exists on both `meals` and `diet_meals` — redundant

The `Meal` entity and `meals` table have a `slot` column ("Breakfast", "Lunch", etc.). `DietMeal` also has a `slot`. The CLAUDE.md rule states: *"slotType lives in diet_slots / planned_slots, not on Meal — a meal is a reusable food collection; which slot it fills is context, not identity."* The backend entity and DDL contradict this rule.

**Plan:** Drop `meals.slot` in a future migration, remove it from the `Meal` entity and `MealDto`. The `slot` in `DietMeal` (and `planned_slots` / `logged_foods.meal_slot`) is the correct location.

### 3.5 `users.id` (BIGSERIAL PK) is never used as a FK anywhere

The `users` table has an integer `id` but all domain tables reference `firebase_uid VARCHAR(255)` directly. The integer PK is dead weight. Either:
- Accept the current design (all domain tables reference `firebase_uid` directly, which is fine and avoids a JOIN)
- Or, for consistency, add FK from `firebase_uid` columns to `users.firebase_uid`

The second option enforces referential integrity (you can't have orphaned records for non-existent users).

---

## 4. Backend — Performance

### 4.1 N+1 queries in `DashboardService`

```kotlin
val recentLogs = logRepo.findTop5ByFirebaseUidOrderByDateDesc(firebaseUid)
    .map { it.toDto(loggedFoodRepo.findByDailyLogId(it.id)) }  // 5 separate queries
```

This is 1 query to get 5 logs + 5 queries to get their logged foods = 6 DB round trips minimum. The same pattern repeats in `list()` methods across `MealService`, `GroceryService`, `DailyLogService`.

**Fix:** Use batch fetch:

```kotlin
val logs = logRepo.findTop5ByFirebaseUidOrderByDateDesc(firebaseUid)
val logIds = logs.map { it.id }
val allFoods = if (logIds.isEmpty()) emptyList() else loggedFoodRepo.findByDailyLogIdIn(logIds)
val foodsByLogId = allFoods.groupBy { it.dailyLogId }
val result = logs.map { it.toDto(foodsByLogId[it.id] ?: emptyList()) }
```

Add `findByDailyLogIdIn(ids: Collection<Long>)` to `LoggedFoodRepository`. Apply the same pattern to `MealService.list`, `GroceryService.list`, `DailyLogService.list`.

### 4.2 System foods fetched from DB on every request — no caching

`FoodService.list()` calls `findByFirebaseUidOrIsSystemFoodTrue()` which returns all system foods + user foods every time. System foods (the large reference set) never change. Add `@Cacheable`:

```kotlin
@Cacheable("system-foods")
fun getSystemFoods(): List<Food> = repo.findByIsSystemFoodTrue()
```

Requires adding Spring Cache (`@EnableCaching` + a `CaffeineCacheManager` or similar).

### 4.3 `FirebaseTokenFilter` logs at INFO level on every authenticated request

```kotlin
log.info("Authenticated Firebase UID: ${claims.subject}")
```

In production this generates one INFO log per request, polluting Cloud Logging. Change to `log.debug(...)`.

---

## 5. Backend — API Design

### 5.1 `PushResponse` is missing `dailyLogs`, `exercises`, `workoutSessions`

```kotlin
data class PushResponse(
    val accepted: Int,
    val foods: List<FoodDto> = emptyList(),
    val meals: List<MealDto> = emptyList(),
    val diets: List<DietDto> = emptyList(),
    val healthMetrics: List<HealthMetricDto> = emptyList(),
    val groceryLists: List<GroceryListDto> = emptyList()
    // dailyLogs, exercises, workoutSessions are missing!
)
```

Android needs the assigned `serverId` back for `daily_log`, `exercise`, and `workout_session` entities, otherwise those entities stay unlinked across devices. Add the missing response fields.

### 5.2 No request validation

No DTO field has `@Valid`, `@NotBlank`, `@Min`, etc. A client can push a food with `name = ""`, `caloriesPer100 = -500`, or a health metric with `type = ""`. Add Jakarta Validation:

```kotlin
data class FoodDto(
    @field:NotBlank val name: String = "",
    @field:Min(0) val caloriesPer100: Double = 0.0,
    ...
)
```

And enable it on controllers with `@Valid` on `@RequestBody`.

### 5.3 No pagination on list endpoints

All `GET /api/v1/foods`, `GET /api/v1/meals`, etc. return the full list. As the food database grows (system + user foods can be thousands of rows), this becomes a memory and latency problem. Add `Pageable` support on at minimum the `/foods` endpoint.

### 5.4 `unit` and `slot` strings not validated

`unit` ("GRAM", "PIECE", "CUP") and `slot` ("Breakfast", "Lunch", "Dinner", "Snack") are free-form strings in DTOs and on entities. Should be Kotlin `enum` or at least validated against a known set. This also helps the TypeScript client get a union type from the OpenAPI spec.

---

## 6. Web App — Duplicated Utilities

### 6.1 Calorie/macro calculation functions duplicated across 3 pages

`calcCalories`, `calcMacro` (or equivalent functions) appear in:
- `app/(app)/dashboard/page.tsx`
- `app/(app)/log/page.tsx`
- `app/(app)/health/page.tsx`

The formula is identical: `(food.xPer100 * grams) / 100` with unit conversion. Move to `lib/utils.ts`:

```typescript
// lib/utils.ts
export function calcNutrient(
  food: Pick<FoodDto, "caloriesPer100" | "proteinPer100" | "carbsPer100" | "fatPer100">,
  loggedFood: Pick<LoggedFoodDto, "quantity" | "unit">,
  key: "caloriesPer100" | "proteinPer100" | "carbsPer100" | "fatPer100"
): number {
  const grams = loggedFood.unit === "GRAM" ? loggedFood.quantity : loggedFood.quantity * 100;
  return (food[key] * grams) / 100;
}
```

### 6.2 `todayStr()` and `formatDateLabel()` duplicated

`todayStr()` is defined identically in `dashboard/page.tsx`, `log/page.tsx`, and `health/page.tsx`. `formatDateLabel()` variants appear in `dashboard/page.tsx` and `log/page.tsx`. Move both to `lib/utils.ts`.

### 6.3 Slot color constants duplicated

The `SLOTS` array with Breakfast/Lunch/Dinner colors and backgrounds is defined in both `dashboard/page.tsx` and `log/page.tsx`, and `SLOT_COLORS` appears in `meals/page.tsx`. Define once in `lib/utils.ts` or a `constants.ts`:

```typescript
// lib/constants.ts
export const MEAL_SLOTS = [
  { key: "Breakfast", emoji: "🌅", color: "#F59E0B", bg: "#FFF8E6" },
  { key: "Lunch",     emoji: "☀️",  color: "#2E7D52", bg: "#E8F5EE" },
  { key: "Dinner",    emoji: "🌙", color: "#7C3AED", bg: "#F3EEFF" },
  { key: "Snack",     emoji: "🍎", color: "#DC2626", bg: "#FFF0F0" },
] as const;
```

### 6.4 `DashboardDto` type defined inline in `dashboard/page.tsx`

```typescript
// dashboard/page.tsx — should not be here
interface DashboardDto {
  todayLog: DailyLogDto | null;
  recentLogs: DailyLogDto[];
  foods: FoodDto[];
  dietCount: number;
  latestWeight: HealthMetricDto | null;
}
```

Add the `DashboardDto` to the backend's OpenAPI spec (`@Schema`) so it appears in `types.generated.ts` automatically.

### 6.5 USDA and OpenFoodFacts API calls live inside a page component

`foods/page.tsx` directly calls the USDA FoodData Central and OpenFoodFacts APIs with in-file type definitions, response parsers, and the literal string `"DEMO_KEY"`. This:
- Cannot be reused on any other page (e.g., if log page ever wants inline food search)
- Uses the demo key which is rate-limited to ~30 requests/hour per IP
- Makes the page component ~500 lines

**Fix:**
1. Move to `lib/api/external-food.ts`
2. Move the USDA key to `NEXT_PUBLIC_USDA_KEY` in `.env.local`
3. Create a `useExternalFoodSearch(query)` hook

### 6.6 No shared data-fetching hooks — every page independently re-fetches

Each page calls `api.get('/api/v1/foods')`, `api.get('/api/v1/meals')`, etc. from within its own `useEffect`. When the user navigates from `/meals` to `/log`, foods are re-fetched even though they were just loaded. With the app growing to 18 screens, adding SWR or TanStack Query would give shared cache, background revalidation, and deduplication automatically.

At minimum, extract per-domain custom hooks:

```typescript
// lib/hooks/useFoods.ts
export function useFoods() {
  const { user } = useAuth();
  const [foods, setFoods] = useState<FoodDto[]>([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { api.get<FoodDto[]>('/api/v1/foods').then(setFoods).finally(...) }, [user]);
  return { foods, loading };
}
```

### 6.7 `export const dynamic = "force-dynamic"` on every page — redundant

All 13 pages have `export const dynamic = "force-dynamic"`. Since the entire `(app)` group is behind Firebase auth (client-side guard in `layout.tsx`) and all data is fetched client-side, this should be set once at the `(app)/layout.tsx` level, or better yet in `next.config.js`:

```js
// next.config.js
experimental: { forceSwcTransforms: true },
```

Or simply accept the default (`'auto'`) since all pages use `"use client"` anyway.

---

## 7. Android — Sync Correctness

### 7.1 In-memory scan for serverId lookup in `pull()`

```kotlin
val existing = dto.serverId?.let { foodDao.getAllFoodsOnce().find { f -> f.serverId == it } }
```

`getAllFoodsOnce()` loads every food into memory, then scans it in Kotlin. If the user has 1000+ foods, this is expensive. Each DAO should have a specific query:

```kotlin
// FoodDao
@Query("SELECT * FROM foods WHERE server_id = :serverId LIMIT 1")
suspend fun getFoodByServerId(serverId: String): FoodItem?
```

### 7.2 `push()` calls `getUnsyncedMeals()` / `getUnsyncedDiets()` but `pull()` upserts all incoming

Push sends only "unsynced" local entities (those without `syncedAt`), but pull upserts all incoming entities regardless. This asymmetry means if a user edits a diet on the web, Android pull correctly merges it. But if the user edits it on Android and it fails to sync, it stays in `getUnsyncedDiets()` until next push. This is correct behaviour, but there should be a corresponding `getUnsyncedDailyLogs()` for consistency — currently `push()` fetches `getAllLogsOnce(userId)` (all logs, not just unsynced).

### 7.3 Two-step push silently drops daily logs on first-time failure

If Step 1 (foods + meals) succeeds but Step 2 (daily logs) fails, the error is logged but not surfaced. The `Result<Int>` returned to `SyncWorker` is still `Success` with the Step 1 count. The user sees "Sync complete" but their logs were not pushed. SyncWorker should get a partial-failure signal.

---

## 8. Architecture — Reusability Between Channels

### 8.1 Streak calculation duplicated: Android + Webapp

The log streak ("N days in a row") is computed:
- On Android in `HealthRepository` or `HomeViewModel` using Room queries
- On the Webapp in `health/page.tsx` (`computeStreak()` function — 12 lines of date math)

Since the backend is the source of truth, streak should be computed server-side and exposed from the Dashboard endpoint (or a dedicated `/api/v1/stats` endpoint). Both channels then consume the same number.

### 8.2 `DashboardService` is a good pattern — expand it

`GET /api/v1/dashboard` aggregates the most common view data in a single round-trip. Both Android and Webapp benefit from this. Consider expanding it to include:
- `currentStreak` (integer, computed server-side)
- `weeklyCalories` (array of 7 day-calorie pairs for the chart)
- `todayMacros` (protein/carbs/fat totals)

This reduces the number of individual API calls on page/screen load from ~4–5 to 1.

### 8.3 OpenAPI-generated types are a good foundation — keep the chain tight

The webapp uses `types.generated.ts` from the OpenAPI spec. This is the right pattern. Currently the `DashboardDto` is missing from the spec (see 6.4). Any new endpoint or field change should also update `docs/openapi.yaml` and regenerate types before the PR merges. Add this as a CI check.

### 8.4 Android Retrofit DTOs and Backend DTOs are defined independently — drift risk

The Android `MealDto`, `FoodDto`, etc. in `data/remote/` are hand-coded Kotlin data classes. They're not generated from the OpenAPI spec. When a backend field changes, Android must be manually updated. Consider using the OpenAPI Generator Kotlin client, or at minimum add a CI job that diffs the OpenAPI spec against the Android DTOs.

---

## 9. Prioritised Action Plan

| Priority | ID     | Area        | Work                                                                  | Effort |
|----------|--------|-------------|-----------------------------------------------------------------------|--------|
| P0 ✅    | CR-01  | Backend/DB  | Fix `isFavorite` shared-state bug (user-prefs table)                  | M      |
| P0 ✅    | CR-02  | Android     | Fix tombstone handling — add `food` case; `daily_log` needs Room migration (TODO) | S |
| P0 ✅    | CR-03  | Android     | Fix positional index serverId matching → use `serverId` map           | S      |
| P0 ✅    | CR-04  | Backend/DB  | Add FK constraints + indexes (V10 migration)                          | S      |
| P1 ✅    | CR-05  | Android     | Add workout sync to `SyncRepository`                                  | M      |
| P1 ✅    | CR-06  | Backend     | Fix `require(...)` → `ResponseStatusException(FORBIDDEN)` everywhere  | S      |
| P1 ✅    | CR-07  | Backend/DB  | Fix N+1 in `DashboardService` + all `list()` methods (batch fetch)    | M      |
| P1 ✅    | CR-08  | Backend     | Add `PushResponse.dailyLogs` / `exercises` / `workoutSessions`        | S      |
| P1 ✅    | CR-09  | Backend     | Add `@Valid` + Jakarta Validation constraints on all DTOs             | M      |
| P1 ✅    | CR-10  | Backend     | Change `log.info` to `log.debug` in `FirebaseTokenFilter`             | XS     |
| P2       | CR-11  | DB          | Drop `meals.slot` column + entity field (design normalization)        | M      |
| P2       | CR-12  | Backend     | Extract shared `upsert` scaffold / abstract service base              | L      |
| P2       | CR-13  | Backend     | Add `@Cacheable` on system foods                                      | S      |
| P2       | CR-14  | Backend     | Add pagination to `/api/v1/foods`                                     | M      |
| P2       | CR-15  | Backend     | Add `currentStreak` + `weeklyCalories` to `DashboardDto`             | M      |
| P2       | CR-16  | Webapp      | Move `todayStr()`, `calcNutrient()`, `MEAL_SLOTS` to `lib/utils.ts`  | S      |
| P2       | CR-17  | Webapp      | Move USDA/OFF search to `lib/api/external-food.ts` + env var key      | M      |
| P2       | CR-18  | Webapp      | Add `DashboardDto` to OpenAPI spec, regenerate types                  | S      |
| P2       | CR-19  | Webapp      | Remove per-page `dynamic = "force-dynamic"`, set globally             | XS     |
| P3       | CR-20  | Android     | Replace in-memory `find { f.serverId == it }` with DAO queries        | M      |
| P3       | CR-21  | Android     | Fix two-step push — surface partial failure to `SyncWorker`           | M      |
| P3       | CR-22  | Webapp      | Add `useFoods()` / `useLogs()` custom hooks (consider SWR)            | L      |
| P3       | CR-23  | Backend/DB  | Add FK from `firebase_uid` columns to `users.firebase_uid`           | M      |
| P3       | CR-24  | Android     | Investigate OpenAPI Generator Kotlin client for drift prevention      | L      |

**Effort key:** XS < 1h | S = 1–2h | M = half-day | L = 1–2 days

---

## 10. What Is Already Good

- `SyncableEntity` base class — clean, covers `createdAt`, `updatedAt`, `serverId`
- `FirebaseTokenFilter` — zero-billing JWT validation via JWKS, no Admin SDK dependency
- `TombstoneService` — correct soft-delete propagation to clients
- `lib/api/client.ts` — clean, centralized fetch wrapper with token injection
- `types.generated.ts` — OpenAPI-to-TypeScript generation prevents type drift for webapp
- Flyway migrations — explicit, versioned, no destructive fallback
- Separate `DashboardService` — good "backend for frontend" aggregation pattern
- Room explicit migrations on Android — no destructive fallback, schema exported
- `BackupRepository` / `DataImporter` — safe upsert-only food seeder (preserves FK refs)
