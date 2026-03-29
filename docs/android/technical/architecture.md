# Android Architecture

**Audience:** Contributors and developers
**Tech Stack:** Kotlin · Jetpack Compose · Room · Hilt · Coroutines · WorkManager

---

## High-Level System Design

```
┌─────────────────────────────────────────────────────────────────────┐
│                        MealPlan+ Android                            │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   PRESENTATION LAYER                         │   │
│  │                                                              │   │
│  │   Jetpack Compose Screens  ←→  ViewModels (StateFlow)       │   │
│  │   Navigation Compose (29+ routes)                            │   │
│  │   Hilt ViewModel injection                                   │   │
│  └─────────────────────┬────────────────────────────────────────┘   │
│                        │ calls                                      │
│  ┌─────────────────────▼────────────────────────────────────────┐   │
│  │                   REPOSITORY LAYER                           │   │
│  │                                                              │   │
│  │   AuthRepository     FoodRepository     MealRepository      │   │
│  │   DietRepository     DailyLogRepository PlanRepository      │   │
│  │   HealthRepository   GroceryRepository                      │   │
│  │                                                              │   │
│  │   • Single source of truth                                   │   │
│  │   • Expose Kotlin Flow for reactive UI updates               │   │
│  │   • Abstract local vs remote data sources                    │   │
│  └──────────────┬────────────────────────────┬───────────────────┘   │
│                 │ local                       │ remote              │
│  ┌──────────────▼──────────┐   ┌─────────────▼───────────────────┐  │
│  │      DATA LAYER          │   │      NETWORK LAYER              │  │
│  │   (Room SQLite)          │   │      (Retrofit)                 │  │
│  │                          │   │                                 │  │
│  │  17 entities             │   │  OpenFoodFactsApi (barcodes)    │  │
│  │  10 DAOs                 │   │  UsdaFoodApi (search)           │  │
│  │  21 migrations           │   │  MealPlanApi (backend sync)     │  │
│  │  Type converters         │   │                                 │  │
│  └─────────────────────────┘   └─────────────────────────────────┘  │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │               CROSS-CUTTING CONCERNS                         │   │
│  │   Hilt DI  •  DataStore Preferences  •  WorkManager         │   │
│  │   Firebase Auth  •  Glance Widgets  •  CSV Export           │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Architecture Pattern: MVVM + Repository

```
Screen (Compose)
    │  observes StateFlow
    ▼
ViewModel
    │  calls suspend fun / collects Flow
    ▼
Repository
    │  queries          │  calls
    ▼                   ▼
Room DAO           Retrofit API
```

- **Screen** — stateless Composable. Receives `UiState` and dispatches events to ViewModel.
- **ViewModel** — holds `MutableStateFlow<UiState>`. Coordinates data fetching and business logic. Survives config changes.
- **Repository** — single source of truth. Decides whether to serve cached local data or fetch remote data.
- **DAO / API** — raw data access with no business logic.

---

## Dependency Injection (Hilt)

Entry points:
- `MealPlanApp` — `@HiltAndroidApp`
- `MainActivity` — `@AndroidEntryPoint`

Modules:

| Module | Provides |
|--------|---------|
| `DatabaseModule` | `MealPlanDatabase`, all 10 DAOs |
| `NetworkModule` | Retrofit instances for each API, OkHttp client |
| `AuthModule` | Auth configuration |

All repositories and ViewModels are `@Singleton` or `@HiltViewModel` scoped — injected automatically via constructor injection.

---

## Reactive Data Flow

```
Room DB (Flow)
    │
    ▼
Repository.getSomethingFlow()       ← returns Flow<T>
    │
    ▼
ViewModel.collect { }               ← updates MutableStateFlow<UiState>
    │
    ▼
Screen: val state by vm.uiState.collectAsState()
    │
    ▼
Composable re-renders only changed parts
```

Key detail: `SharingStarted.WhileSubscribed(5000)` keeps upstream flows alive 5 seconds after the last subscriber, preventing redundant DB queries on quick navigation back.

---

## Navigation

```
MealPlanNavHost
    │
    ├── Login / SignUp / ForgotPassword   (unauthenticated stack)
    │
    └── Scaffold + BottomNavBar
            ├── Home
            ├── Calendar (Meal Plan)
            ├── Daily Log
            ├── Diets
            ├── Health
            └── Grocery
                  │
                  └── ... sub-screens via NavController.navigate()
```

- Routes are defined as `sealed class Screen(val route: String)` — 29+ routes.
- Widget deep links are handled in `MainActivity.onNewIntent()` and passed to `MealPlanNavHost` as `WidgetDeepLink`.
- Logout triggers an **Activity restart** (`FLAG_ACTIVITY_NEW_TASK or CLEAR_TASK`) to fully reset the NavHost, all ViewModels, and the back stack — avoids Compose navigation state bugs.

---

## Background Work (WorkManager)

| Worker | Trigger | Purpose |
|--------|---------|---------|
| `SyncWorker` | Periodic (15 min interval) | Syncs data with backend (future cloud feature) |
| `WidgetUpdateWorker` | Data change events | Keeps home screen widgets current |

WorkManager uses `ExistingPeriodicWorkPolicy.KEEP` to avoid duplicate workers on app restart.

---

## Widget System (Jetpack Glance)

Three Glance AppWidgets:

| Widget | Class | Data source |
|--------|-------|-------------|
| Today's Plan | `TodayPlanWidget` | `WidgetDataRepository.getTodaySlotsFlow()` |
| Diet Summary | `DietSummaryWidget` | `WidgetDataRepository.getTodayDietSummaryFlow()` |
| Mini Calendar | `CalendarWidget` | `PlanRepository` |

Widgets use `collectAsState()` inside `provideContent {}` — they are self-updating. Room emits a new value → widget recomposes automatically. No `AppWidgetManager.updateAll()` needed.

---

## Authentication Flow

```
App Launch
    │
    ▼
AuthPreferences.isLoggedIn() — DataStore Flow<Boolean>
    │
    ├── false → Login screen
    │               │
    │               ├── Email/Password → Firebase signInWithEmailAndPassword
    │               └── Google → Firebase signInWithCredential (GoogleAuthProvider)
    │                               │
    │                               ▼
    │                   AuthPreferences.setLoggedIn(userId)
    │                   AuthPreferences.setProviderSubjectMapping(provider, uid, localUserId)
    │
    └── true → Home screen
                    │
                    ▼
            getCurrentUserId() → load user data
```

See [Authentication](authentication.md) for full details.

---

## Performance Optimisations

| Technique | Where | Effect |
|-----------|-------|--------|
| Batch SQL queries | `DietsViewModel`, `DietPickerViewModel` | Eliminates N+1 query problem on Diets screen |
| `DietDisplayCache` (`@Singleton`) | `DietsViewModel` | Instant re-navigation to Diets — no re-load |
| `remember(key)` in Glance | `DietSummaryWidget` | Skips bitmap redraw when data unchanged |
| `WhileSubscribed(5000)` | All `stateIn` flows | Keeps flows alive across quick back/forward nav |

---

## Tech Stack Summary

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| State management | `StateFlow` + `collectAsState()` |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |
| Network | Retrofit + OkHttp |
| Preferences | DataStore |
| Background jobs | WorkManager |
| Widgets | Jetpack Glance |
| Auth | Firebase Authentication |
| Build | Gradle (KTS) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
