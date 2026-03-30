# Notifications — Technical Reference

## Architecture Overview

```
HomeScreen (bell icon)
    └─→ SettingsScreen (Notifications section)
            └─→ SettingsViewModel
                    └─→ NotificationPreferences (DataStore)

WorkManager (hourly)
    ├─→ MealReminderWorker
    │       ├─→ NotificationDecider.shouldSendMealReminder()
    │       └─→ NotificationHelper.postMealReminder()
    ├─→ StreakProtectionWorker
    │       ├─→ NotificationDecider.shouldSendStreakAlert() / computeStreak()
    │       └─→ NotificationHelper.postStreakAlert()
    └─→ WeeklyPlanWorker
            ├─→ NotificationDecider.shouldSendWeeklyPlan()
            └─→ NotificationHelper.postWeeklyPlanReminder()
```

---

## Key Files

| File | Purpose |
|------|---------|
| `util/NotificationPreferences.kt` | DataStore reads/writes for all notification settings |
| `util/NotificationDecider.kt` | Pure Kotlin decision logic (no Android deps) |
| `util/NotificationHelper.kt` | Notification channel creation and posting |
| `work/MealReminderWorker.kt` | Hourly WorkManager worker for meal reminders |
| `work/StreakProtectionWorker.kt` | Hourly WorkManager worker for streak alerts |
| `work/WeeklyPlanWorker.kt` | Hourly WorkManager worker for weekly plan reminder |
| `ui/screens/settings/SettingsScreen.kt` | Notifications section UI |
| `ui/screens/settings/SettingsViewModel.kt` | `NotificationState` + setters |

---

## NotificationPreferences

Shares the `"settings"` DataStore (defined in `ThemePreferences.kt`) with all other preference
objects. Keys are prefixed with `notifications_` to avoid collisions.

| Key | Type | Default |
|-----|------|---------|
| `notifications_master_enabled` | Boolean | `false` |
| `notifications_meal_reminders_enabled` | Boolean | `true` |
| `notifications_streak_protection_enabled` | Boolean | `true` |
| `notifications_weekly_plan_enabled` | Boolean | `true` |
| `notifications_breakfast_hour` | Int | `8` |
| `notifications_lunch_hour` | Int | `13` |
| `notifications_dinner_hour` | Int | `19` |
| `notifications_streak_alert_hour` | Int | `21` |

---

## NotificationDecider

Stateless `object` with pure functions — no `Context`, no coroutines. This allows full unit
testing without Android mocks.

```kotlin
NotificationDecider.shouldSendMealReminder(
    currentHour, notificationsEnabled, mealRemindersEnabled, targetHour, isAlreadyLogged
): Boolean

NotificationDecider.shouldSendStreakAlert(
    currentHour, notificationsEnabled, streakProtectionEnabled,
    alertHour, streakDays, todayCalories
): Boolean

NotificationDecider.shouldSendWeeklyPlan(
    dayOfWeek, currentHour, notificationsEnabled, weeklyPlanEnabled, plansThisWeekCount
): Boolean

NotificationDecider.computeStreak(completedDateStrings: List<String>, today: LocalDate): Int
```

---

## NotificationHelper

| Function | Notification ID |
|----------|----------------|
| `postMealReminder(context, "BREAKFAST")` | 1001 |
| `postMealReminder(context, "LUNCH")` | 1002 |
| `postMealReminder(context, "DINNER")` | 1003 |
| `postStreakAlert(context, streakDays)` | 1004 |
| `postWeeklyPlanReminder(context)` | 1005 |

Channel ID: `meal_reminders`. Created once in `MealPlanApp.onCreate()`.

`canPostNotifications(context)` guards all post calls: returns `true` unconditionally on
API < 33; checks `POST_NOTIFICATIONS` permission on API 33+.

---

## WorkManager Workers

All three workers are scheduled in `MealPlanApp.scheduleNotificationWork()` using
`ExistingPeriodicWorkPolicy.KEEP` (no duplicate work if already scheduled).

Each worker uses Hilt's `@EntryPoint` pattern to inject repositories:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun dailyLogRepository(): DailyLogRepository
    fun remoteConfigManager(): RemoteConfigManager
    // ...
}
```

Worker execution order per run:
1. Read `NOTIFICATIONS_ENABLED` Remote Config flag — abort if disabled.
2. Read `masterEnabled` DataStore preference — abort if off.
3. Read type-specific toggle — abort if off.
4. Collect repository data (logs, plans, metrics).
5. Call `NotificationDecider` to decide if alert fires.
6. Call `NotificationHelper` if yes.

---

## Feature Flag

| Flag key | Default | Behaviour |
|----------|---------|-----------|
| `notifications_enabled` | `true` | Kill-switch. Set to `false` in Remote Config to silence all workers globally without an app release. |

The flag is checked as the first guard in each worker, before any DataStore or repository access.

---

## Android Permission

`POST_NOTIFICATIONS` is declared in `AndroidManifest.xml` and required at runtime on
Android 13+ (API 33). The Settings screen handles the permission request:
- If the user tries to enable the master toggle and the permission is not yet granted, the
  system permission dialog is shown.
- The toggle becomes active only after the user grants the permission.

---

## Adding a New Notification Type

1. Add a firing condition function to `NotificationDecider` (pure Kotlin, write test first).
2. Add a `postXxx()` method to `NotificationHelper` with a unique notification ID.
3. Create a new `Worker` subclass following the existing pattern.
4. Schedule it in `MealPlanApp.scheduleNotificationWork()`.
5. Add a preference key (if user-configurable) to `NotificationPreferences`.
6. Add a toggle/time-picker row to `SettingsScreen` Notifications section.
7. Add getter + setter to `NotificationState` / `SettingsViewModel`.

---

## Testing

| Test file | What it covers |
|-----------|---------------|
| `NotificationDeciderTest` | 26 unit tests for all four pure functions |
| `NotificationPreferencesTest` | Default values and constants |
| `SettingsViewModelNotificationTest` | Initial state from flows + all 8 setters |

Run with:
```
./gradlew :android:testDebugUnitTest
```

---

## Phase 2 — Firebase Cloud Messaging (FCM)

Social/push notifications for future features:
- "Your friend commented on your meal"
- "A new diet plan was shared with you"
- "Your nutritionist sent you a message"

These require a server component to trigger. Current local notifications are purely device-side
and do not require FCM.
