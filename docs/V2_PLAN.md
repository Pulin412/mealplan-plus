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
| **Last completed** | FoodsScreen (Phase 2) | FoodsPage `/foods` (Phase 2) |
| **Next task** | Phase 1 — Firebase Auth + Login/Register screens | Phase 1 — Auth guard + Bottom nav layout shell |
| **Blocked on?** | — | — |

**Suggested next session start:**
- **android-v2**: Add Firebase Auth dep to `build.gradle.kts`, wire token injection in `NetworkModule`, then build Login + Register screens (prototype 6a).
- **webapp-v2**: Create `(app)/layout.tsx` auth guard + `components/layout/BottomNav.tsx` (4 tabs: Today · Plan · Exercises · Health), wire root redirect.
- Do android-v2 and webapp-v2 in parallel — they don't depend on each other.

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
- ⬜ **MealsScreen** — same shell (sort/list/compact/fav); ＋ → new meal sheet: name + slot + add food items
- ⬜ MealViewModel, MealDao, MealRepository, Meal entity (Room)
- ⬜ **DietsScreen** — same shell; ＋ → new diet: name + build by adding meals/foods into slots
- ⬜ DietViewModel, DietDao, DietRepository, Diet entity (Room)
- ⬜ Wire Foods/Meals/Diets into NavGraph (accessible from Today FAB or menu)

#### webapp-v2
- ✅ **FoodsPage** `/foods` — list/compact, search, sort, favourites, FAB speed-dial, manual/online/barcode sheets
- ✅ `useFoods` hook, `lib/api/foods.ts`, `types/food.ts`
- ⬜ **MealsPage** `/meals` — same shell; add-meal sheet (name + slot + food picker)
- ⬜ `useMeals` hook + `lib/api/meals.ts`
- ⬜ **DietsPage** `/diets` — same shell; build-diet sheet (slot-grouped food/meal picker)
- ⬜ `useDiets` hook + `lib/api/diets.ts`
- ⬜ Reusable `CrudShell` component (search bar + sort + fav toggle + view toggle) — shared across Foods/Meals/Diets

---

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

#### android-v2
- ⬜ **ExercisesScreen** (Exercises tab) — 3 tabs: Exercises · Workouts · Logs
- ⬜ **Exercises tab**: list (name + tag chips); ＋ → new/edit form (name + tag toggles); delete cascades to workouts
- ⬜ ExerciseViewModel, ExerciseDao, ExerciseRepository, Exercise entity (Room)
- ⬜ **Workouts tab**: list (name + count + sets preview); ＋ → builder (name + searchable exercise picker + sets×reps steppers per exercise)
- ⬜ WorkoutViewModel, WorkoutDao, WorkoutRepository, Workout entity (Room)
- ⬜ **Logs tab**: month calendar (green logged days) + recent sessions list (last 30 days); detail sheet (per-exercise per-set rows)

#### webapp-v2
- ⬜ **ExercisesPage** `/exercises` — 3-tab segmented control: Exercises · Workouts · Logs
- ⬜ Exercises tab: list + add/edit sheet (name + tag toggles using tag palette)
- ⬜ Workouts tab: list + builder sheet (name + exercise picker with stepper for sets×reps)
- ⬜ Logs tab: mini calendar + 30-day sessions list + detail sheet
- ⬜ `useExercises`, `useWorkouts`, `useLogs` hooks + `lib/api/exercises.ts`, `lib/api/workouts.ts`
- ⬜ Tag chip component (uses exercise tag palette — 10 fixed colours, 12% alpha bg)
- ⬜ Segmented tab control component

---

### Phase 5 — Plan → Session Runner → Workout Logs

#### android-v2
- ⬜ **PlanScreen** (Plan tab) — month calendar (teal dot = diet, green dot = workout, today filled); ‹ › month nav; next-7-days summary list; tap any day → day-plan sheet
- ⬜ Day-plan bottom sheet: pick diet (radio list), add/remove workouts (chips + library); ▶ Start Workout button; Clear day
- ⬜ PlanViewModel, DayPlanDao, DayPlanRepository (Room)
- ⬜ **Session Runner** (full-screen overlay, 3 phases):
  - Ready: template (sets×reps) + last-time history per exercise + Start button
  - Active: per-exercise set rows (reps stepper), Copy last, Add/remove set, Finish button
  - Done/Logged: read-only set/reps table; writes WorkoutLog (upsert); re-open → read-only + Edit action
- ⬜ SessionViewModel, WorkoutLogDao, WorkoutLogRepository (Room)

#### webapp-v2
- ⬜ **PlanPage** `/plan` — month calendar + next-7 list + day-plan sheet
- ⬜ Day-plan bottom sheet (diet picker + workout chips + Start Workout)
- ⬜ Session Runner page/overlay `/session` (3 phases: Ready → Active → Done)
- ⬜ `usePlan` hook + `lib/api/plans.ts`
- ⬜ `useSession` hook + `lib/api/workout-sessions.ts`
- ⬜ Calendar grid component (reusable across Plan + Exercise Logs)
- ⬜ Stepper component (− / value / +)

---

### Phase 6 — Health

#### android-v2
- ⬜ **HealthScreen** (Health tab) — 3 tabs: Glucose · Weight · BP
- ⬜ Each tab: latest reading + unit + delta vs range start (green when improving), trend graph (7D/30D/90D toggle), streak (current + best), **readings-in-range count**, recent readings list, FAB → log sheet
- ⬜ Trend graph composable (single line + point dots for glucose/weight; dual systolic + diastolic lines for BP)
- ⬜ Log sheet: one numeric field (glucose/weight) or two (sys/dia for BP)
- ⬜ HealthViewModel, HealthReadingDao, HealthReadingRepository (Room)

#### webapp-v2
- ⬜ **HealthPage** `/health` — 3-tab segmented control: Glucose · Weight · BP
- ⬜ Each tab: latest reading + delta, streak (current + best), **readings-in-range count**, recent list
- ⬜ Trend chart component (line + point dots, 7/30/90D range toggle; dual systolic+diastolic lines for BP)
- ⬜ Log sheet (bottom sheet — 1 numeric field for glucose/weight, 2 fields sys/dia for BP)
- ⬜ `useHealth` hook + `lib/api/health.ts`

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
| 1 | Which food/nutrition API for "Search online"? (Open Food Facts is free + no key; USDA needs key; Nutritionix paid) | Phase 2 foods search | — |
| 2 | Barcode scanning library for android-v2? (ML Kit is free; ZXing is open source) | Phase 2 barcode sheet | — |
| 3 | Barcode scanning for webapp-v2? (browser `BarcodeDetector` API or QuaggaJS) | Phase 2 barcode sheet | — |
| 4 | Should Plan's "workouts" be hard-linked to Workout entities (recommended) or free-text strings? | Phase 5 day-plan sheet | Recommend: hard-link |
| 5 | Add **weight per set** to Session Runner, or reps-only for now? (spec logs reps per set only) | Phase 5 session runner | — |
| 6 | Timezone / day-rollover for streaks: use device local midnight or UTC? | Phase 3 + Phase 6 streaks | — |
| 7 | Meal slot enum — spec lists 11 fixed slots + user custom. Should custom slots be supported in v2 or deferred? | Phase 2 meals, Phase 3 home | — |
| 8 | Google OAuth for webapp-v2 — is `NEXT_PUBLIC_GOOGLE_CLIENT_ID` configured in `.env.local`? | Phase 1 auth | — |
| 9 | Notifications for android-v2 — carry over AlarmManager from `android/` or redesign? | Phase 8 | — |

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
