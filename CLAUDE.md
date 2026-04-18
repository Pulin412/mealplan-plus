# MealPlan+ — Project Context

> See **ROADMAP.md** for the full multi-phase plan and GitHub issue tracking.

## What this app does
Offline-first meal planning and food logging app. Users log meals by slot (BREAKFAST / LUNCH / DINNER), track health metrics, browse/create diets, get shopping lists, and receive smart notifications when they haven't logged a meal yet.

---

## Architecture Direction (agreed)
- **Android** and **Web App** are fully independent codebases — no shared Kotlin code
- **Backend** (Spring Boot) is the source of truth and shared layer for both clients
- **`shared/` KMP module** is disconnected — `MigrationRunner.kt` and `RoomToSQLDelightMigration.kt` deleted; `:shared` removed from `settings.gradle.kts`. Do not add any code there.
- **iOS** is replaced by a Next.js PWA that works on iPhone Safari
- **AI**: Spring AI + PgVectorStore on the backend; Gemini Nano on-device for Android offline use

---

## Module layout
| Module | Role |
|--------|------|
| `android/` | Android app (Kotlin, Compose, Room, Hilt) — **fully self-contained, single production app** |
| `backend/` | Spring Boot 3.2.5 REST API; Firebase JWKS auth, Neon.tech Postgres + pgvector — **Phase 1 complete** |
| `webapp/` | Next.js 14 + TypeScript PWA — **not yet created, Phase 3** |
| `shared/` | KMP module — **disconnected, no code here** |
| `ios/` | SwiftUI app — **superseded by PWA, no new work** |
| `backup/` | `mealplan_data_export.json` + DB snapshot — temporary, used for one-time data import |

---

## Android architecture
**MVVM + Repository + Hilt DI**, following Google's Now in Android guidelines.

```
Compose UI  →  @HiltViewModel  →  Repository  →  Room DAO / Retrofit
                 StateFlow                         Flow<T>
```

- **ViewModels** — `@HiltViewModel`, expose `StateFlow<XxxState>` data classes, use `viewModelScope`
- **Repositories** — `@Singleton`, return `Flow<T>` for reactive observation, suspend funs for writes
- **Room** — 16 entities (v27 target), explicit migrations only (no destructive fallback ever), schema exported to `android/schemas/`. Full schema documented in `docs/DATABASE_SCHEMA.md`.
- **Compose** — Material 3, dynamic color, single-Activity (`MainActivity`), `NavHost` for all navigation
- **DataStore** — all user preferences (theme, notifications); shared `"settings"` store in `ThemePreferences.kt`

### Key package map
```
android/src/main/java/com/mealplanplus/
├── data/
│   ├── healthconnect/  Health Connect SDK wrapper (HealthConnectManager, ActivitySummary)
│   ├── local/          Room DB, DAOs, migrations, importers/exporters
│   ├── model/          Entity + domain model classes
│   ├── remote/         Retrofit API clients (MealPlanApi, OpenFoodFactsApi, UsdaFoodApi)
│   └── repository/     All repositories (one per domain)
│                       HealthConnectRepository — steps, calories burned, weight from HC
├── di/                 Hilt modules (DatabaseModule, NetworkModule, AuthModule)
├── notification/       AlarmManager-based notification system
│   ├── NotificationAlarmReceiver.kt   BroadcastReceiver (goAsync + coroutine)
│   ├── NotificationAlarmBootstrapper.kt  scheduleAll / rescheduleForType
│   └── BootReceiver.kt
├── ui/
│   ├── screens/        One package per screen, each has Screen.kt + ViewModel.kt
│   ├── components/     Shared Compose components
│   ├── navigation/     NavHost + route definitions
│   └── theme/          Material 3 Color / Type / Shape
├── util/               Pure utilities (NotificationDecider, AlarmScheduler, SortUtils, etc.)
├── widget/             Glance home-screen widgets
├── work/               SyncWorker (WorkManager, Hilt entry point pattern)
└── MealPlanApp.kt      Application class (@HiltAndroidApp)
```

---

## Database design

See `docs/DATABASE_SCHEMA.md` for the full schema, ER diagram, slot resolution algorithm, and migration history.

### Key design decisions
- **`slotType` lives in `diet_slots` / `planned_slots`, not on `Meal`** — a meal is a reusable food collection; which slot it fills is context, not identity.
- **`user_id` only where strictly needed** — `day_plans`, `daily_logs`, `health_metrics`, `grocery_lists`. Meals and diets are app-global.
- **Planning vs Logging are separate** — `day_plans`/`planned_slots` = intent; `daily_logs`/`logged_foods` = reality. Streak uses logs only.
- **All dates stored as `Long` (epoch milliseconds at midnight UTC).** Use `LocalDate.toEpochMs()` and `Long.toLocalDate()` from `util/DateUtils.kt`.
- **System foods re-seeded by version** (`DataStore` key `systemFoodsVersion`). Uses safe upsert — never deletes existing food rows (preserves FK refs from `meal_foods` and `logged_foods`).

---

## Hard rules — never break these

1. **No destructive Room migrations.** Always add an explicit `MIGRATION_X_Y` in `DatabaseModule.kt`. The schema files in `android/schemas/` must stay in sync.
2. **Zero billing guardrail.** Do not import Firestore, Cloud Functions, Firebase Storage, or Firebase Realtime Database. The build has a custom task that fails CI if these appear.
3. **Firebase used only for:** Authentication, Crashlytics, Remote Config, Analytics — all free-tier only.
4. **Notifications fire only when the meal slot is not yet logged.** `shouldPostMealAlarm()` in `NotificationAlarmReceiver` enforces this; don't bypass it.
5. **AlarmManager, not WorkManager, for notifications.** WorkManager is only used for `SyncWorker`. The three old notification workers are deleted — do not re-add them.
6. **Never delete system food rows during re-seed.** `DatabaseSeeder` uses upsert-only strategy to preserve FK references from `meal_foods` and `logged_foods`.
7. **`planned_slots` is the source of truth for day planning.** When loading a day, read `planned_slots` first; fall back to `day_plans.template_diet_id`'s `diet_slots` only for slots not yet in `planned_slots`.
8. Always confirm with the user before pushing to a branch or committing files.

---

## Testing conventions
- **TDD when adding features** — write the failing test first, then implement.
- **Unit tests** live in `android/src/test/` and run on the JVM (no emulator needed).
- **Instrumented DAO tests** live in `android/src/androidTest/` using an in-memory Room database.
- Run all unit tests locally: `./gradlew :android:testDebugUnitTest`

### Test patterns used throughout the codebase
```kotlin
// Coroutine dispatcher — use @BeforeClass, not @Before, to avoid re-setting per test
@BeforeClass fun setUpClass() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

// Mock singletons (objects) with mockkObject — always unmock in @After
mockkObject(NotificationPreferences)
coEvery { NotificationPreferences.setBreakfastHour(context, any()) } just Runs
// ...
unmockkObject(NotificationPreferences)

// Test flows with Turbine
viewModel.uiState.test {
    assertEquals(expected, awaitItem())
    cancelAndIgnoreRemainingEvents()
}
```

---

## Notification system (AlarmManager)
- **5 alarm types** defined in `NotificationAlarmType` enum (BREAKFAST, LUNCH, DINNER, STREAK, WEEKLY_PLAN)
- Each has a stable `requestCode` (1–5) used as `PendingIntent` identity for cancel/replace
- Alarms are one-shot (`setExactAndAllowWhileIdle`); receiver re-schedules next occurrence after each fire
- `SCHEDULE_EXACT_ALARM` permission required on API 31+; Settings screen shows a banner if not granted
- `NotificationAlarmBootstrapper.scheduleAll(context)` is the single entry point for (re-)scheduling everything
- Hour + minute precision: both stored in DataStore, both shown in the Settings time picker

---

## CI pipeline
Single `ci.yml` orchestrator. Runs **only on push to main**.

1. `detect-changes` (5 s) — dorny/paths-filter decides which modules changed
2. `android` job — only if `android/**` or `shared/**` changed. Uses `--build-cache --parallel`. **No `--configuration-cache`** — blocked by `verifyNoBillableFirebaseFeatures` task capturing a `Project` reference.
3. `backend` job — only if `backend/**` changed. `--configuration-cache` is safe here.
4. `ios` job — only if `ios/**` or `shared/**` changed. Caches Xcode DerivedData to cut warm builds from ~11 min to ~3-4 min.
5. `backend-deploy.yml` — separate file, deploys to Cloud Run on backend changes to main.

---

## Health Connect integration

MealPlan+ integrates with **Android Health Connect** (free, no cloud costs) to pull activity data from connected fitness watches (Garmin via Garmin Connect, Fitbit, Samsung Galaxy Watch, Polar, etc.).

### Data flow
```
Fitness watch  →  vendor companion app  →  Health Connect (local store)
                                              ↓
                                   HealthConnectManager (SDK reads)
                                              ↓
                                   HealthConnectRepository
                                         ↙          ↘
                              HomeViewModel        SettingsViewModel
                          (activity strip in        (connect / sync
                           home header)              weight → Health)
```

### What is synced
| Data | Record type | Where shown |
|------|-------------|-------------|
| Steps today | `StepsRecord` | Home screen activity strip |
| Calories burned today | `TotalCaloriesBurnedRecord` | Home screen activity strip |
| Latest weight (last 30 days) | `WeightRecord` | Auto-logged to Health screen (once per day) |

### Permissions
Three read-only permissions declared in `AndroidManifest.xml`:
- `android.permission.health.READ_STEPS`
- `android.permission.health.READ_TOTAL_CALORIES_BURNED`
- `android.permission.health.READ_WEIGHT`

Permission is requested via `PermissionController.createRequestPermissionResultContract()` in `SettingsScreen`. The `MainActivity` declares `ACTION_SHOW_PERMISSIONS_RATIONALE` intent-filter so Health Connect can show our app in its permission management UI.

### Key classes
- `HealthConnectManager` — `@Singleton`, direct SDK calls (availability, permissions, reads)
- `HealthConnectRepository` — `@Singleton`, safe wrapper (returns empty/defaults when unavailable)
- `ActivitySummary` — data class carrying steps + calories burned + `isConnected` flag
- `HomeViewModel.loadActivityData()` — fetches on init and refresh
- `SettingsViewModel.checkHealthConnectStatus()` — checks connection, syncs weight, called on resume

### Availability
- Android 14+ (API 34): Health Connect is part of the OS — always available.
- Android 9–13: user must install the Health Connect app from Play Store. The Settings screen shows an "Install" prompt if unavailable.

---

## Current Status (as of April 18, 2026)

**Phase 1 (Backend Sync API) is COMPLETE on `feature/phase1-backend-sync`.** Foundation is merged to `main`.

### Backend (Spring Boot — `backend/`)
- All 7 domain CRUD endpoints (User, Food, Meal, Diet, Grocery, HealthMetric, DailyLog)
- `POST /api/v1/sync/push` + `GET /api/v1/sync/pull?since=<ISO>` — delta sync with last-write-wins
- **Tombstones** — `Tombstone` entity + `TombstoneService`; all `delete()` methods write a tombstone; pull response includes `tombstones[]`
- **Flyway** — `V1__init.sql` (full schema) + `V2__pgvector.sql` (pgvector extension + `entity_embeddings` table with HNSW index). Flyway disabled for H2 dev, enabled for docker/prod profile.
- Firebase JWT auth via JWKS; `SecurityConfig` secures all endpoints
- `docs/openapi.yaml` — hand-crafted OpenAPI 3.0 spec (source of truth for TypeScript codegen in Phase 3)
- Jackson configured to serialize `Instant` as epoch milliseconds for Android compatibility

### Android (`android/`)
- `SyncWorker` — 15-min periodic WorkManager job; reads `lastSyncTimestamp` from DataStore, saves `serverTime` after pull
- `SyncRepository` — push unsynced records, pull delta, apply tombstones (delete local records by `serverId`), conflict resolution: remote wins only when `remoteUpdatedAt > localUpdatedAt` (server wins on tie)
- `SyncPreferences` — `last_sync_timestamp` DataStore key
- `HomeUiState.lastSyncedAt` — exposed to UI for sync status display
- `MealPlanApi` — Firebase token injected via OkHttp interceptor (`Tasks.await` on background thread)

### CI/CD
- `ci.yml` — build + test on push to `main` (Android, Backend, iOS path-filtered)
- `backend-deploy.yml` — Cloud Run deploy; triggered on `backend/**` changes to `main`; uses Cloud Run secrets for DB credentials; verifies `/actuator/health` after deploy
- **Cloud Run service:** `mealplan-api` in `europe-west1` (deploy URL available after first run — update `MEAL_PLAN_API_URL` in `NetworkModule.kt`)

### What is next — Phase 2 (Workout Logging)
See `ROADMAP.md`. After merging `feature/phase1-backend-sync` → `main`: #89 (Android workout entities) → #90 (workout screens) → #91 (backend workout sync)

---

## Key dependency versions
| Library | Version |
|---------|---------|
| Kotlin | 1.9.24 |
| AGP (Android Gradle Plugin) | 8.5.0 |
| Compose BOM | 2024.02.00 |
| Room | 2.6.1 |
| Hilt | 2.51.1 |
| Coroutines | 1.7.3 |
| Firebase BOM | 32.7.0 |
| Retrofit | 2.9.0 |
| MockK | 1.13.10 |
| Turbine | 1.1.0 |
| DataStore | 1.0.0 |
| Glance (widgets) | 1.1.0 |
| Health Connect | 1.1.0-alpha08 |
