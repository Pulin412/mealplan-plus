# MealPlan+ v2 — Transition Plan

> Source of truth for tracking the complete v2 rebuild.
> Design spec: `design_handoff_mealplan/MealPlan+ Build Spec.dc.html`
> Prototype:   `design_handoff_mealplan/MealPlan Home.dc.html`

---

## For the agent — how to use this file

1. **Read this section first.** "Resume here" tells you exactly where to start.
2. **Check the file map** before creating any file — it may already exist.
3. **Mark tasks done** by changing `⬜` → `✅` as you complete them and update "Resume here".
4. **Run the build command** for the affected module before reporting a task done.
5. **Ask the user** before any commit, push, Room migration, or API contract change.
6. **Never touch** `android/` or `webapp/` — those are the old apps, read-only.

---

## ▶ Resume here

> Update this block every time a task completes.

| | android-v2 | webapp-v2 |
|--|------------|-----------|
| **Last completed** | **Settings wired: Export ✅ · Notifications ✅ · Health Connect ✅ (+Home activity card).** Backup **dropped**. | Settings: Export ✅. Notifications deferred (Web Push memo); Backup **dropped**; Health Connect is Android-only. |
| **Next task** | Settings section complete → pick next (Profile polish / Phase 8 PWA / cross-cutting) | Same |
| **Blocked on?** | Android notifications + Health Connect: user on-device smoke-test pending | — |

**✅ Settings functionality is essentially complete.** Export (both) · Notifications (android; webapp deferred to Web Push) · Health Connect (android; N/A on web) · Backup **dropped as redundant with sync**.

**Settings wiring — Export CSV (android-v2, DONE, device-verified 2026-07-24):**
- Single sectioned `.csv` (one file, 4 labelled sections): **Meals** + **Diets** (all, from Room) · **Workouts** (completed sessions, **last 7 days**, one row per set, 1-based set# grouped per exercise) · **Health** (all built-in types, **last 90 days**). Numbers `Locale.US`, RFC-4180 escaping, ISO dates.
- Files: `data/export/{ExportData,CsvExporter,ExportRepository}.kt`, `ui/screens/settings/SettingsViewModel.kt` (one-shot Share event), `SettingsScreen.kt` wired (Export button → FileProvider share sheet), `AndroidManifest.xml` + `res/xml/file_paths.xml` (FileProvider, cacheDir/exports). Unit test `CsvExporterTest` (7 tests).
- **✅ Webapp DONE (browser-verified, parity):** `lib/export/{exportData,csvExporter,collectExport}.ts` + `app/settings/page.tsx` Export button. Reuses `foodMacros` resolver; Blob download; same single-sectioned format. Health section byte-identical to android; workouts 1-based. (Webapp has no test runner — format logic covered by android `CsvExporterTest`.)

**Settings wiring — Notifications (2026-07-24):**
- **✅ android-v2 DONE** (built + installed; **awaiting user on-device smoke-test before commit**). All 5 types (Meal/Water/Workout/Weigh-in/Glucose) + quiet hours. **Fixed configurable times**, on-device **AlarmManager** (inexact `setAndAllowWhileIdle` — dodges the exact-alarm Play gate), no backend. Files: `data/notifications/{NotificationType,NotificationStore,NotificationHelper,NotificationScheduler}.kt`, `receiver/{NotificationReceiver,BootReceiver}.kt`, `res/drawable/ic_notification.xml`, manifest receivers, `MealPlanApplication` (channel), `MainActivity` (reschedule on launch), `SettingsViewModel` + `SettingsScreen` (toggles + `POST_NOTIFICATIONS` prompt). Prefs = SharedPreferences (`notifications`). Test `NotificationSettingsTest` (4, quiet-hours wrap-around). Verified: 17 alarms scheduled (4 enabled types; weigh-in off by default). Defaults: Meal 8/13/19h · Water 8–20h/2h · Workout 18h · Weigh-in Sun 8h · Glucose ×6. Follow-ups: per-time editor UI; "skip if already logged" guard.
- **⏸ webapp/PWA DEFERRED — removed from UI.** iOS has **no on-device scheduler** → reminders must be **server-sent Web Push** (iOS 16.4+, installed PWA only, VAPID). Full design memo (architecture, 2 new tables, 5 endpoints, Cloud Scheduler cost options, sign-off gates): **[iOS Web Push design memo](https://claude.ai/code/artifact/dc89e1d8-cdf3-4f63-a360-505125ab948b)** (2026-07-24). Needs: Flyway migration + API-contract change + Cloud Scheduler — all sign-off gates. The webapp Settings **Notifications section was removed** (a no-op toggle is misleading); see the code comment in `app/settings/page.tsx`. Revisit alongside Phase 8 (PWA hardening).

**Settings wiring — Health Connect (android-v2, DONE 2026-07-24, committed; user smoke-test pending):**
- Read-only (steps, calories burned, latest weight) — **free, no cloud cost**. Settings card = connect flow (grant → "Connected" + today summary; toggle off → `revokeAll()`). **Home Activity card** shows steps + kcal burned when connected (compact, no icons; refreshes on resume). Files: `data/healthconnect/HealthConnectManager.kt` (safe SDK wrapper), `SettingsViewModel`/`SettingsScreen` (card + permission launcher), `Home{ViewModel,Screen}` (activity card). Manifest gained the HC rationale + `VIEW_PERMISSION_USAGE`/`HEALTH_PERMISSIONS` intents (SDK dep + read perms were already present). Verified on emulator: 3 perms `granted=true`, read path runs (reads 0 — no fitness source on emulator; real data needs a device). **N/A on webapp** (HC is an Android API). Follow-up: pipe HC weight into the Health screen (would create server records — separate design).

**Settings wiring — Backup & restore: DROPPED (2026-07-24).**
- **Removed from both clients' Settings** (redundant with backend sync). Rationale: all data lives in **Neon Postgres keyed to the Firebase UID** (server-backed REST domains + Room domains that sync via `/sync/push,pull`), so a reinstall/new device **re-syncs** — that IS the restore. Only the grocery working list is local-only, and it's **regenerable** (Refresh recomputes from the plan), so not worth backing up. Portability is already covered by **Export CSV**. Point-in-time undo, if ever wanted, is better solved by **soft-delete/trash** than a backup blob. Server-side backup would've needed Flyway + API-contract + Cloud Scheduler sign-offs for little gain.

**Barcode scanner — Foods → ＋ → Scan barcode (BOTH clients, 2026-07-24; android device-verified via manual-entry path, webapp compiles):**
- Scan → **Open Food Facts** lookup (free, no key, called directly from the client) → product card (name, brand, per-100g macros) → **Add to my foods** (reuses the online-add/create path; android saves to Room + syncs, webapp POSTs `/foods` keeping brand+barcode). **Manual-entry fallback** on both (damaged barcodes + emulator/testing). Open-questions #1/#2/#3 resolved.
- **Android:** ML Kit `barcode-scanning` (bundled, on-device) + CameraX preview/analysis; CAMERA permission + runtime prompt. Files: `data/remote/OpenFoodFactsApi.kt`, `data/repository/BarcodeRepository.kt`, `ui/screens/foods/BarcodeScanSheet.kt` (scanner + phases), FoodViewModel/FoodsScreen wiring, NetworkModule OFF provider (separate Retrofit, no auth header). Verified on emulator: Nutella `3017620422003` → 539/6.3/57.5/30.9 → added (foods 25→26).
- **Webapp:** `@zxing/browser` (works on **iOS Safari**; native `BarcodeDetector` does not). `lib/api/barcode.ts` (lookup + `createScannedFood`), `useFoods.addScannedFood`, `app/foods/page.tsx` `BarcodeSheet` (live `<video>` scan, dynamic-imported). Needs HTTPS/localhost for the camera. tsc+lint clean.

**Foods "Search online" → Open Food Facts (2026-07-25; android device-verified, backend curl-verified, webapp compiles):**
- Replaced the old backend-DB search in the **Foods "Search online" sheet only** (Meal builder keeps `/foods/search`).
- **Android** calls OFF directly (`search.openfoodfacts.org`, no CORS constraint natively): `OpenFoodFactsApi.search` + `BarcodeRepository.search` → `FoodViewModel.searchOnline`. Verified: "yogurt" → real products → +Add persists (26→27).
- **Webapp** goes through a **backend proxy** — the reliable OFF search host sends no CORS header, so the browser can't call it. **New endpoint `GET /api/v1/foods/search-online`** (openapi + `OpenFoodFactsClient` via Spring `RestClient`, no new dep; `FoodController.searchFoodsOnline`). Curl-verified (23 mapped FoodDto). Webapp `searchFoodsOnline` → proxy; `addOnlineFood` now **creates** the food (OFF results have no id). **API-contract change (pre-approved).** Additive/non-breaking, but the ISO-date "don't deploy backend to main yet" gate still applies before prod.

**Done since Health:**
- **Dates**: backend now serializes `date-time`/`date` as ISO-8601 (`WRITE_DATES_AS_TIMESTAMPS=false`, regression test `JsonDateSerializationTest`). Android Gson adapter + webapp both consume ISO directly; the old epoch-millis/`[y,m,d]` workarounds are gone. ⚠ **Do NOT deploy backend to `main`** until the old prod Android app is retired — `android/SyncRepository.kt` still expects epoch millis and would break.
- **Groceries** (both clients): shopping list generated from the plan's diets over a picked date range, grouped by aisle. **Server-REST source** (reuses Plans/Diets/Meals/Foods APIs like Plan; NOT offline/Room, NOT the grocery sync contract). Live working state = **independent rows** persisted locally (android SharedPreferences `GroceryStore`, webapp `localStorage`). The list is a **stable snapshot** — only **Refresh** (or a day change) recomputes it; a plan edit does NOT auto-reflect. Refresh reconciles: checked (bought) rows kept (capped to need), each ingredient's to-buy row = new total − bought, and a checked item that grows spawns a **separate to-buy row** for the delta.
- **Settings screen (both)** — design = prototype frame **13a** in the newer `design_v2/*.zip`. Started as UI-only (Backup & restore, Health Connect, Export data, Notifications); now **wired** (see the Settings-wiring notes at the top): Export ✅, Notifications ✅ (android), Health Connect ✅ (android). **Backup & restore removed** (redundant with sync). Webapp: Export ✅; Notifications + Backup removed; Health Connect is Android-only.
- **Navigation restructure (both)**: bottom nav = Today · Plan · Exercises · Health · **More**. The **More** tab (android `MiscScreen`, webapp `/misc`) lists Foods/Meals/Diets/Groceries. Home/Today's top-left is a **Settings gear**; the avatar → Profile. **Profile + Settings are reachable only from Home.** (Removed the old ☰ page-cycle, Groceries' profile avatar, Profile's gear.)

**Settings functionality — DONE** (see the per-section wiring notes at the top). Export ✅ · Notifications ✅ (android; webapp → Web Push memo, deferred) · Health Connect ✅ (android; N/A web) · Backup **dropped** (redundant with sync). No backend endpoints were needed — all of it is client-side. Next up is a fresh screen/phase (Profile polish, Phase 8 PWA hardening, or the cross-cutting tasks).

---

## Modules

| Module | Role | Status |
|--------|------|--------|
| `backend/` | Spring Boot REST API — **shared by both clients** | ✅ Done |
| `android-v2/` | Fresh Kotlin/Compose redesign (`com.mealplanplus.v2`) | 🔄 In progress |
| `webapp-v2/` | Fresh Next.js 14 redesign PWA | 🔄 In progress |

> `android/` and `webapp/` are the old apps — **read-only, do not modify**.

---

## Backend — DONE

All DB migrations (V13–V20), all API domains, sync controller complete.

<details>
<summary>What was done</summary>

- V13 users profile enums + targets  
- V14 foods.verified  
- V15 universal tags (entity_tags, entity_type)  
- V16 exercises cleanup  
- V17 diet_food_items  
- V18 day_plans + planned_workouts  
- V19 workout_sessions uniqueness + logged_meal_slots  
- V20 is_favorite on meals/diets  
- All domain controllers/services/repos (Food, Meal, Diet, Exercise, Workout, Plan, Health, Log, Dashboard, User)  
- Sync push + pull includes all domains, loggedMealSlots, dayPlans  
- All tests pass  

</details>

---

## File map — what already exists

> Verify a file exists with `ls` before creating it. Never recreate something that's already done.

### android-v2 (`android-v2/src/main/java/com/mealplanplus/`)

| File | What it is |
|------|-----------|
| `MainActivity.kt` | Entry point, sets content to `MealPlanNavHost()` |
| `MealPlanApplication.kt` | Hilt application class |
| `ui/navigation/NavGraph.kt` | 4-tab bottom nav (Today/Plan/Exercises/Health); `startDestination` = Foods (temp) |
| `ui/theme/Color.kt` | Teal, Ink, AppBg, DmMono, Success, CardBorder, etc. |
| `ui/theme/Theme.kt` | `MealPlanV2Theme` |
| `ui/theme/Type.kt` | `AppTypography` with DM Mono |
| `ui/screens/home/HomeScreen.kt` | **Stub only** |
| `ui/screens/plan/PlanScreen.kt` | **Stub only** |
| `ui/screens/exercises/ExercisesScreen.kt` | **Stub only** |
| `ui/screens/health/HealthScreen.kt` | **Stub only** |
| `ui/screens/foods/FoodsScreen.kt` | ✅ Full implementation (list/compact, search, sort, fav, FAB, sheets) |
| `ui/screens/foods/FoodViewModel.kt` | ✅ Full implementation |
| `data/local/AppDatabase.kt` | Room DB v1; currently only Food entity registered |
| `data/local/Converters.kt` | Type converters |
| `data/local/dao/FoodDao.kt` | ✅ Done |
| `data/model/Food.kt` | ✅ Room entity |
| `data/repository/FoodRepository.kt` | ✅ Done |
| `di/AppModule.kt` | Hilt module (repo bindings) |
| `di/DatabaseModule.kt` | Provides `AppDatabase` + DAOs |
| `di/NetworkModule.kt` | Retrofit + OkHttp; **token injection not yet wired** |
| `build/generated/openapi/…/api/*.kt` | Retrofit interfaces generated from openapi.yaml |
| `build/generated/openapi/…/model/*.kt` | Generated DTOs |

### webapp-v2 (`webapp-v2/src/`)

| File | What it is |
|------|-----------|
| `app/layout.tsx` | Root layout — **no auth guard yet** |
| `app/page.tsx` | Root page — returns null, redirect not wired |
| `app/globals.css` | Design tokens as CSS variables (teal, ink, bg, etc.) |
| `app/foods/page.tsx` | ✅ Full FoodsPage (list/compact, search, sort, fav, FAB, sheets) |
| `components/ui/BottomSheet.tsx` | ✅ Reusable bottom sheet + SheetField |
| `hooks/useFoods.ts` | ✅ Foods state + API calls |
| `lib/api/client.ts` | `apiFetch<T>` with Firebase Bearer token |
| `lib/api/foods.ts` | ✅ Foods API calls |
| `lib/api/types.generated.ts` | Generated from openapi.yaml — do not hand-edit |
| `lib/auth/firebase.ts` | Firebase init |
| `lib/utils/cn.ts` | Tailwind class helper |
| `types/food.ts` | `FoodDto`, `FoodSort` types |

---

## Design system tokens (complete reference)

> Implement these in `android-v2` theme files and `webapp-v2/globals.css`. Do not deviate.

| Token | Value |
|-------|-------|
| Primary / teal | `oklch(0.62 0.09 210)` |
| Ink | `#14181b` |
| App bg | `#f7f9fa` |
| Success / workout green | `oklch(0.66 0.13 150)` |
| Streak flame | `oklch(0.7 0.18 45)` |
| Danger | `#b23b3b` |
| Muted text ramp | `#5b666e` → `#8a949b` → `#9aa4aa` → `#a2abb1` |
| Border ramp | `#eaeef0`, `#eef1f3`, `#dfe6e8`, `#e4e8eb`, `#f2f4f5` |
| **Macro — Protein** | `oklch(0.60 0.10 200)` (blue-teal) |
| **Macro — Carbs** | `oklch(0.60 0.11 255)` (violet-blue) |
| **Macro — Fat** | `oklch(0.62 0.11 150)` (green) |
| UI font | system-ui / -apple-system |
| Numeral / data font | DM Mono (400/500), `font-variant-numeric: tabular-nums` |
| Card | white, 1px `#eaeef0` border, 12–16px radius |
| FAB — Android | teal **circle** |
| FAB — iOS / PWA | teal **rounded-square** |
| Min tap target | 44px |

**Exercise tag palette** (fixed hue map, all `oklch(~0.52 ~0.13 hue)`, chips = 12% alpha bg + full text):

| Tag | Hue |
|-----|-----|
| Chest | 25° |
| Back | 255° |
| Legs | 150° |
| Shoulders | 65° |
| Arms | 310° |
| Core | 200° |
| Cardio | 20° |
| Push | 215° |
| Pull | 285° |
| Mobility | 170° |

---

## Build phases — spec §11 order

Each phase lists tasks for **android-v2** and **webapp-v2**.
Status: ✅ Done · 🔄 In progress · ⬜ Not started

---

### Phase 1 — Auth + account + sync skeleton

#### android-v2
- ⬜ Firebase Auth integration (GoogleSignIn, email/password)
- ⬜ Login screen (email/password + Google button) — spec §6 / prototype 6a
- ⬜ Register screen
- ⬜ Forgot password screen
- ⬜ Auth guard in MainActivity (redirect to login if not signed in)
- ⬜ Token injection in NetworkModule (attach Firebase ID token to every API call)
- ⬜ SyncWorker (Room ↔ backend delta sync, queues writes when offline)
- ⬜ Offline banner (shown in top bar when no network)

#### webapp-v2
- ⬜ Login page `/login` (email + Google) — prototype 6a; logo = "macro plate" conic ring with check centre; footer "© Pulin 2026"
- ⬜ Register page `/register`
- ⬜ Forgot-password page + "Check your inbox" confirmation state → back to login
- ⬜ Auth guard in `(app)/layout.tsx` — redirect to `/login` if not signed in
- ⬜ Token injection already in `lib/api/client.ts` (verify it works)
- ⬜ Bottom nav layout shell (`components/layout/BottomNav.tsx` + `(app)/layout.tsx`) — 4 tabs: Today · Plan · Exercises · Health
- ⬜ **Full-height sub-screen pattern**: bottom nav hides during create/edit flows (Foods add, Meal builder, Diet builder, Exercise editor, Workout builder, Session Runner) — app chrome must disappear for these full-screen overlays
- ⬜ Root `page.tsx` redirect → `/today` (or `/login`)
- ⬜ Offline banner component + service worker sync queue

---

### Phase 2 — Foods → Meals → Diets (nutrition core)

#### android-v2
- ✅ **FoodsScreen** — list/compact toggle, search, sort, favourites, FAB speed-dial (manual/online/barcode sheets)
- ✅ FoodViewModel, FoodDao, FoodRepository, Food entity
- ✅ **MealsScreen** — same shell (sort/list/compact/fav); slot filter chips + badges; ＋ → New Meal builder (name + multi-select slots + add-food panel: search-your-foods / online / manual). Offline-first, one reusable meal tagged to many slots (no per-slot dupes).
- ✅ MealViewModel, MealDao, MealRepository, Meal entity (Room, DB v5) + backend `slots` (V21) + `foodServerId` resolution
- ✅ **DietsScreen** — slot-grouped New Diet builder; **tags** (normalized Tag/EntityTag: assign + filter row + tag-aware search); offline-first (DB v7/v8).
- ✅ DietViewModel, DietDao, DietRepository, DietMappers, Diet entity + TagRepository (online tags via TagsApi, Hilt-provided).
- ✅ **Edit for Foods/Meals/Diets** — in-card ✎ Edit reopens the builder pre-filled + Delete + Save-as-update (offline-first `update()`); row-tap expands (chevrons dropped).
- ⬜ Real bottom-nav wiring (temp: back-arrow cycles Meals→Diets→Foods; bottom nav is Phase 3)

#### webapp-v2
- ✅ **FoodsPage** `/foods` — list/compact, search, sort, favourites, FAB speed-dial, manual/online/barcode sheets
- ✅ `useFoods` hook, `lib/api/foods.ts`, `types/food.ts`
- ⬜ **MealsPage** `/meals` — same shell; add-meal sheet (name + slot + food picker)
- ⬜ `useMeals` hook + `lib/api/meals.ts`
- ⬜ **DietsPage** `/diets` — same shell; build-diet sheet (slot-grouped food/meal picker)
- ⬜ `useDiets` hook + `lib/api/diets.ts`
- ⬜ Reusable `CrudShell` component (search bar + sort + fav toggle + view toggle) — shared across Foods/Meals/Diets

---

#### ✅ DONE — Per-food units + text-input quantities (2026-07-20)

Both shipped & device-verified. Food carries a `unit` (g/ml/pcs/cup/tbsp/tsp); count units
capture a grams-per-unit factor at creation; meal calorie math is unit-aware; the meal builder
uses numeric text fields instead of -/+ steppers. Backend `Food.unit` + Flyway **V22**; android
Room **DB v6**. Verified: Egg (pcs, 50g/pcs, 155kcal) → 1 pcs = 78 kcal, 3 pcs = 233 kcal;
synced to backend with `unit=PIECE`. Original notes below.

Two refinements requested before moving to Diets. Approach is **decided** — build directly.

**1. Per-food measurement unit (g / ml / pieces / cup / tbsp / tsp).**
Each food carries its own natural unit so eggs log in *pieces*, milk in *ml*, coffee in *g*.
- **Model (decided):** add a direct **`unit` field on `Food`** (a `FoodUnit`) — NOT a "category" (category is indirect; unit is exactly what's needed). Keep macros **per 100 g** (comparable, label-style).
- Infra already exists: `FoodUnit` enum (GRAM/ML/PIECE/CUP/TBSP/TSP), meal **items already carry a `unit`**, and `Food` already has `gramsPerPiece/Cup/Tbsp/Tsp` conversion factors. Only the food's **default `unit`** is missing.
- **Create-food form:** add a unit picker. For piece/cup/tbsp/tsp, also capture **grams-per-unit** (e.g. 1 egg = 50 g) so calories still compute. For g/ml, factor ≈ 1.
- **Calorie math:** a meal item's quantity is in the food's unit; convert to grams via the factor, then macros = per-100g × grams/100. Adding an egg defaults the item unit to *pieces*, milk to *ml*.
- **Touches:** `docs/openapi.yaml` (FoodDto.unit), backend `Food`/`FoodDto` + small Flyway migration (V22), android-v2 `Food` entity (+ Room migration → DB v6) + create-food form + `Meal.resolve` calorie math, webapp-v2 later.

**2. Text-input quantities instead of − / + steppers.**
Replace the steppers with a numeric text field for quantity — both when adding a food item to a meal **and** in the add-food rows. User types any value in the food's unit.
- **Touches:** android-v2 `MealsScreen.kt` (the `Stepper`/`PickRow` composables in `NewMealSheet`/`AddFoodPanel`).

Reminder: Room migrations + Flyway migrations need explicit human approval before writing (per `android-v2/CLAUDE.md` + root `CLAUDE.md`). Backend runs locally on **H2** (never prod — V13–V21 unapplied on Neon).

### Phase 3 — Home / Today

#### android-v2
- ⬜ **HomeScreen** (Today tab) — app bar (☰ + bell + avatar), date + current diet name, calorie ring card, meals checklist, streak card, FAB → "Add to today" sheet
- ⬜ Calorie ring composable (arc draws consumed/remaining; macro chips P/C/F)
- ⬜ Meals checklist composable (check toggle → logs/unlogs the slot → ring + macros update live)
- ⬜ Meal detail bottom sheet (cooking checklist — checking ingredients does NOT log the meal)
- ⬜ Streak card composable (current streak, best, 7-day dots)
- ⬜ "Add to today" sheet (search planned meals, ＋ new food / ＋ new recipe)
- ⬜ HomeViewModel (loads DayPlan for today, logged slots, calorie ring data from Dashboard API)

#### webapp-v2
- ⬜ **TodayPage** `/today` — same layout as android spec (calorie ring, meals checklist, streak card, FAB)
- ⬜ Calorie ring component (CSS/SVG arc)
- ⬜ Meals checklist component (check toggle, live ring update)
- ⬜ Meal detail bottom sheet
- ⬜ Streak card component
- ⬜ "Add to today" bottom sheet
- ⬜ `useToday` hook + `lib/api/dashboard.ts` + `lib/api/logging.ts`

---

### Phase 4 — Exercises → Workouts (library + templates)

> **Data-layer note:** the exercise/workout domain is NOT in the offline sync contract
> (`SyncPushRequest` has exercises + workoutSessions but not workout *templates*). Decision:
> android-v2 exercises/workouts/logs are **server-backed REST** (ExercisesApi, WorkoutTemplatesApi,
> WorkoutSessionsApi), like the Plan screen — **no Room entities**. Needs a backend up.

#### android-v2 — ✅ Exercises + Workouts done (server-backed); Logs = empty state (needs Session Runner)
- ✅ **ExercisesScreen** (Exercises tab) — 3-tab segmented control: Exercises · Workouts · Logs
- ✅ **Exercises tab**: list (name + tag chips); ＋ → new/edit editor (name + **description** + tag toggles); delete
- ✅ ExercisesViewModel + ExerciseRepository (server REST) + ExerciseTags palette (10 fixed oklch colours)
- ✅ **Workouts tab**: list (name + count + total sets + preview); ＋ → builder (searchable exercise picker;
  **per-set targets**: reps stepper + optional weight (kg) per set; **copy-set** icon per row; scrollable full view)
- ✅ WorkoutRepository (server REST). ~~Workout entity (Room)~~ → not used (server-backed).
- ✅ **Logs tab**: **month calendar** (logged days filled green, tap a day → session detail) + recent-sessions list
  **capped to last 7 days**; read-only detail grouping sets by exercise (Set/Reps/Weight). WorkoutSessionRepository
  (server REST). Backed by real sessions created by the Session Runner (Phase 5).

**Spec change (contract-first, done):** `ExerciseDto.description`; `TemplateExerciseDto` now holds
`sets: [TemplateSetDto{setNumber, reps, weightKg}]` (replaced single targetSets/targetReps/targetWeightKg) —
mirrors `WorkoutSetDto`. Backend: new `template_exercise_sets` table/entity, WorkoutService mapping, **Flyway V23**
(docker/prod). Seed script (`scripts/dev-seed-h2.py`) extended: 10 exercise tags + 10 exercises + 3 workouts.
**Follow-ups:** weight shown/entered in kg only (not yet converted to Profile unit); webapp still to do (below).

#### webapp-v2 — ✅ Exercises + Workouts + Logs done (same contract; types via `npm run gen:api`)
- ✅ **ExercisesPage** `/exercises` — 3-tab segmented control: Exercises · Workouts · Logs
- ✅ Exercises tab: list + full-screen editor (name + description + tag toggles using tag palette)
- ✅ Workouts tab: list + builder (searchable exercise picker; per-set reps stepper + optional weight kg; copy-set)
- ✅ Logs tab: **month calendar** (logged days green, tap → detail) + recent list (last 7 days) + full-screen detail (sets grouped by exercise)
- ✅ `useExercises` hook + `lib/api/exercises.ts`, `lib/api/workouts.ts`, `lib/api/sessions.ts`, `lib/exerciseTags.ts`
- ✅ Tag chip + selectable tag toggle (exercise tag palette via `color-mix`)
- ⬜ Home "Today's workout" card + Session Runner + ad-hoc single-exercise logging — NOT yet ported (android done)

---

### Phase 5 — Plan → Session Runner → Workout Logs

> **Note:** the whole training domain is **server-backed REST** (no Room) — DayPlan/Session/WorkoutLog
> aren't in the sync contract. Endpoints already in openapi; no contract change needed for the runner.

#### android-v2 — ✅ done
- ✅ **PlanScreen** — month calendar (teal dot = diet, green dot = workout, today filled) + next-7 list + day-plan sheet
- ✅ Day-plan sheet: pick diet; **add workout from library** (template picker) + remove chip; tap chip → **read-only workout detail**; Clear day
- ✅ PlanViewModel (server REST via PlansApi `/workouts` add/remove; WorkoutTemplatesApi)
- ✅ **Session Runner** (`runner?templateId=…` / `?exerciseId=…`, full-screen, bottom nav hidden), 3 phases:
  - Ready: template sets×reps + "Last time" per exercise + Start button
  - Active: per-exercise set rows (reps stepper + weight kg), Copy last, Add/remove set, **auto-save (PUT) → resume**, Finish
  - Done: read-only set table; `POST /finish` writes the day's log (upsert); re-open → read-only Done + **Edit**
  - Exercise **description** shown under each exercise name (from ExerciseDto, mapped by id)
- ✅ SessionRunnerViewModel + WorkoutSessionRepository (`start`/`create`/`update`/`finish`/`listForDate`/`lastForExercise`)
- ✅ **Home "Today's workout" card** — planned + ad-hoc sessions with status (Planned/In progress/✓ Done), tap → runner,
  reload on resume; empty → **Add sheet with Workouts | Exercises tabs** (workout → plan; exercise → ad-hoc runner, no-template mode)
- ⚠️ **StateFlow gotcha:** multi-loader VMs use `_state.update{}` not `_state.value=copy()` (lost-update race → stuck loading)

#### webapp-v2 — ✅ done (parity with android)
- ✅ **PlanPage** `/plan` — calendar + next-7 + day sheet; add workout from library + remove; tap → read-only workout detail
- ✅ `usePlan` extended (`addPlannedWorkout`/`removePlannedWorkout`), `lib/api/plans.ts` (+`getPlan`) + `lib/api/sessions.ts`
- ✅ **Home "Today's workout" card** (`useTodayWorkouts`) — planned + ad-hoc sessions with status, tap → `/session`;
  empty → Add sheet with **Workouts | Exercises** tabs (workout → plan; exercise → ad-hoc runner)
- ✅ **Session Runner** route `/session?templateId=…`/`?exerciseId=…` (`useSession`) — Ready→Active→Done, per-set
  logging + auto-save/resume (nav unmounts the page so Home reloads on return), Copy last/Last time, Finish, Edit,
  exercise descriptions; no-template ad-hoc mode. `lib/api/sessions.ts` (`start`/`create`/`update`/`finish`/`lastForExercise`), `lib/api/workouts.ts` (+`getWorkout`)
- ⬜ Calendar grid component (reusable across Plan + Exercise Logs)
- ⬜ Stepper component (− / value / +)

---

### Phase 6 — Health ✅ (both clients, server-backed REST)

> **Data-layer decision:** Health is **server-backed REST** (`HealthMetricsApi`), NOT Room — no migration.
> Metrics also live in the sync contract, but REST kept it consistent with Exercises/Plan and avoided a
> Room migration. Built-in types: WEIGHT (kg), GLUCOSE (mg/dL), BLOOD_PRESSURE (value=systolic, secondaryValue=diastolic).

#### android-v2 — ✅ done
- ✅ **HealthScreen** — 3 metric tabs (Glucose · Weight · BP); latest value + delta vs range start (green when
  improving/lower), 7D/30D/90D toggle, streak (current + best), readings-logged count, recent readings, FAB → log sheet
- ✅ **Trend chart** (Canvas): single line + dots (glucose/weight), dual systolic+diastolic (BP, diastolic = violet `#c7a4dd`)
- ✅ **Range-aware binning** so long ranges don't over-populate: 7D raw · 30D daily-avg · **90D weekly-avg (no dots)**
  → caps 90D at ≤13 points. **Tap a point** → guide line + value bubble (TextMeasurer).
- ✅ HealthViewModel (multi-loader → uses `_state.update{}`), HealthRepository (`HealthMetricsApi`), AppModule provider
- Log sheet: one field (glucose/weight) or two (sys/dia for BP)

#### webapp-v2 — ✅ done (parity)
- ✅ **HealthPage** `/health` — same layout; SVG trend chart with the same binning + tap tooltip; dual BP lines
- ✅ `useHealth` hook + `lib/api/health.ts`; added ❤️ Health tab to the temp `NutritionNav`
- ⚠️ **`recordedAt` is epoch millis on the wire** (not the ISO string the type claims) → parsed with `new Date(millis)`
  in `dateOf`/sort. Calling string methods on it silently emptied the screen (fixed). See known-issue note above.
- Local seeder (`scripts/dev-seed-h2.py`, gitignored) extended with ~52 readings (glucose/weight/BP) for demo/QA.

**Follow-ups (deferred):** "readings-in-range" tile shows total readings logged (no target ranges defined in spec —
its own "try next" suggests adding them); app-bar avatar is decorative (Profile = Phase 7).

---

### Phase 7 — Profile

#### android-v2
- ⬜ **ProfileScreen** — identity header; collapsible sections: Body, Goal & targets, Energy (BMR/TDEE), Preferences (Metric/Imperial toggle), Account (log out, Clear all data)
- ⬜ BMR/TDEE formula implementation (Mifflin–St Jeor)
- ⬜ Metric/Imperial live toggle (store metric, convert at display)
- ⬜ Clear all data (purge Room DB + trigger server-side deletion)
- ⬜ ProfileViewModel

#### webapp-v2
- ⬜ **ProfilePage** `/profile` — same sections as spec
- ⬜ BMR/TDEE computed display
- ⬜ Metric/Imperial toggle (live conversion)
- ⬜ Clear all data action
- ⬜ `useProfile` hook + `lib/api/users.ts`

---

### Phase 8 — PWA hardening (webapp-v2 only)

- ⬜ Verify serwist service worker caches all routes + API responses
- ⬜ Offline shell: show app with cached data + offline banner when network unavailable
- ⬜ Write queue: buffer mutations offline, flush on reconnect
- ⬜ Install prompt (Add to Home Screen) — tested on iOS Safari + Android Chrome
- ⬜ Manifest: icons (all sizes), splash, `display: standalone`, correct `theme_color`
- ⬜ Lighthouse: PWA + Performance + Accessibility + Best Practices all green
- ⬜ **iOS Web Push reminders** (deferred from Settings/Notifications) — builds on the manifest + service worker above. Design memo: https://claude.ai/code/artifact/dc89e1d8-cdf3-4f63-a360-505125ab948b — needs Flyway migration + API-contract change + Cloud Scheduler (all sign-off gates).

---

## Cross-cutting tasks (both clients)

- ⬜ **Feature flags / kill-switch** for risky features: barcode scanning, online food search, notifications (so they can be disabled remotely without a release)
- ⬜ **Unit tests** — formulas (BMR/TDEE, calorie remaining, streak, unit conversion) with boundary cases
- ⬜ **Empty states** — every screen needs a meaningful empty state (not just a blank list)
- ⬜ **Error + loading states** — every async operation needs a loading indicator and error message
- ⬜ **Input validation** — no negative reps, absurd weights, malformed BP; friendly error copy
- ⬜ **Accessibility** — ≥44px tap targets, icon button labels, chart data table fallback

---

## Open questions / decisions needed

These are from spec §10 and project-specific gaps. Answer before implementing the affected feature.

| # | Question | Affects | Decision |
|---|----------|---------|----------|
| 1 | Which food/nutrition API for "Search online"? (Open Food Facts is free + no key; USDA needs key; Nutritionix paid) | Phase 2 foods search | ✅ **Open Food Facts** (free, no key) — barcode lookup + Foods "Search online". Android calls OFF directly; **webapp via a backend proxy** `/foods/search-online` (OFF search host has no CORS). Foods sheet only — Meal builder keeps the DB search. |
| 2 | Barcode scanning library for android-v2? (ML Kit is free; ZXing is open source) | Phase 2 barcode sheet | ✅ **ML Kit barcode-scanning (bundled) + CameraX** (2026-07-24). |
| 3 | Barcode scanning for webapp-v2? (browser `BarcodeDetector` API or QuaggaJS) | Phase 2 barcode sheet | ✅ **`@zxing/browser`** (native BarcodeDetector isn't on iOS Safari) (2026-07-24). |
| 4 | Should Plan's "workouts" be hard-linked to Workout entities (recommended) or free-text strings? | Phase 5 day-plan sheet | Recommend: hard-link |
| 5 | Add **weight per set** to Session Runner, or reps-only for now? (spec logs reps per set only) | Phase 5 session runner | — |
| 6 | Timezone / day-rollover for streaks: use device local midnight or UTC? | Phase 3 + Phase 6 streaks | — |
| 7 | Meal slot enum — spec lists 11 fixed slots + user custom. Should custom slots be supported in v2 or deferred? | Phase 2 meals, Phase 3 home | — |
| 8 | Google OAuth for webapp-v2 — is `NEXT_PUBLIC_GOOGLE_CLIENT_ID` configured in `.env.local`? | Phase 1 auth | — |
| 9 | Notifications for android-v2 — carry over AlarmManager from `android/` or redesign? | Phase 8 | ✅ Fresh AlarmManager impl, fixed configurable times, all 5 types (2026-07-24). Webapp deferred → iOS Web Push memo. |

---

## Key formulas (for test coverage)

```
BMR (male)   = 10·kg + 6.25·cm − 5·age + 5
BMR (female) = 10·kg + 6.25·cm − 5·age − 161
TDEE         = BMR × activityFactor
               (1.2 / 1.375 / 1.55 / 1.725 / 1.9)
remaining    = kcalTarget − Σ(logged slot kcal)
streak       = consecutive days back from today with ≥1 log; stop at first gap
lb           = kg × 2.20462  (display only, store metric)
```

---

## Acceptance criteria quick-ref (spec §12)

- **Nutrition**: create/edit/delete foods, meals, diets; totals compute correctly; favourites/sort/views work; deletions cascade
- **Home**: logging a slot updates ring + macros + streak immediately and persists across reload
- **Workouts**: template sets×reps persist; deleting an exercise removes it from all workouts
- **Session**: Start shows template + last session; Copy last works; Finish writes exactly one dated log; re-open → read-only + Edit; edit updates same day's log (no duplicate); appears in Logs
- **Plan**: dots reflect planned diet/workout; day sheet edits persist; next-7 stays in sync
- **Health**: logging updates latest/delta/graph/streak/list; range toggle changes series; BP shows two lines
- **Profile**: BMR/TDEE match formulas; unit toggle converts live; clear-data purges everything
- **Cross-cutting**: works offline; syncs on reconnect; iOS PWA + Android both correct

---

## Build commands

```bash
# android-v2
./gradlew :android-v2:assembleDebug
./gradlew :android-v2:testDebugUnitTest

# webapp-v2
cd webapp-v2 && npm run build && npm run lint
cd webapp-v2 && npm run gen:api          # regen types from openapi.yaml
```
