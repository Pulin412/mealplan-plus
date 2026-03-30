# Notifications — Technical Reference

## Architecture Overview

```
HomeScreen (bell icon)
    └─→ SettingsScreen (Notifications section)
            └─→ SettingsViewModel
                    └─→ NotificationPreferences (DataStore)
                    └─→ NotificationAlarmBootstrapper

AlarmManager (exact, one-shot)
    ├─→ NotificationAlarmReceiver  (ACTION_NOTIFICATION_ALARM)
    │       ├─→ handleMealAlarm()   → NotificationHelper.postMealReminder()
    │       ├─→ handleStreakAlarm() → NotificationHelper.postStreakAlert()
    │       ├─→ handleWeeklyPlanAlarm() → NotificationHelper.postWeeklyPlanReminder()
    │       └─→ rescheduleNext()    (re-arm for next day / next Monday)
    └─→ BootReceiver  (BOOT_COMPLETED)
            └─→ NotificationAlarmBootstrapper.scheduleAll()
```

---

## Key Files

| File | Purpose |
|------|---------|
| `util/AlarmScheduler.kt` | `NotificationAlarmType` enum; pure time functions (`nextTriggerMillis`, `nextMondayTriggerMillis`); `scheduleAlarm` / `cancelAlarm` / `cancelAllNotificationAlarms`; `scheduleMealAlarm` / `scheduleWeeklyPlanAlarm` |
| `notification/NotificationAlarmBootstrapper.kt` | Coordinator object: `scheduleAll(context)` and `rescheduleForType(context, type)` — reads prefs and delegates to `AlarmScheduler` |
| `notification/NotificationAlarmReceiver.kt` | `BroadcastReceiver` for `ACTION_NOTIFICATION_ALARM`; uses `goAsync()` + coroutine; dispatches per type; re-schedules next occurrence |
| `notification/BootReceiver.kt` | `BroadcastReceiver` for `BOOT_COMPLETED`; delegates to `NotificationAlarmBootstrapper.scheduleAll()` |
| `util/NotificationPreferences.kt` | DataStore reads/writes for all notification settings (booleans, hours, **minutes**) |
| `util/NotificationDecider.kt` | Pure Kotlin decision logic (no Android deps) |
| `util/NotificationHelper.kt` | Notification channel creation and posting |
| `ui/screens/settings/SettingsScreen.kt` | Notifications section UI with hour + minute sliders |
| `ui/screens/settings/SettingsViewModel.kt` | `NotificationState` + setters; calls bootstrapper on every change |

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
| `notifications_breakfast_minute` | Int | `0` |
| `notifications_lunch_minute` | Int | `0` |
| `notifications_dinner_minute` | Int | `0` |
| `notifications_streak_alert_minute` | Int | `0` |

---

## AlarmScheduler

Stateless `object`. All public functions except `scheduleAlarm` / `cancelAlarm` can be
unit-tested on the JVM because they accept an injectable `java.time.Clock`.

```kotlin
// Pure time calculation — testable without Robolectric
AlarmScheduler.nextTriggerMillis(hour: Int, minute: Int, clock: Clock): Long
AlarmScheduler.nextMondayTriggerMillis(hour: Int, minute: Int, clock: Clock): Long

// Android-side scheduling
AlarmScheduler.scheduleAlarm(context, type, triggerAtMillis)
AlarmScheduler.cancelAlarm(context, type)
AlarmScheduler.cancelAllNotificationAlarms(context)
AlarmScheduler.scheduleMealAlarm(context, type, hour, minute)
AlarmScheduler.scheduleWeeklyPlanAlarm(context)   // always next Monday 08:00
```

### API 31+ exact-alarm fallback

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
    am.setAndAllowWhileIdle(...)   // approximate — user hasn't granted SCHEDULE_EXACT_ALARM
} else {
    am.setExactAndAllowWhileIdle(...)
}
```

`NotificationAlarmType` enum values carry a stable `requestCode` (1–5) used as the
`PendingIntent` request code, which is what allows `cancelAlarm` to target the exact alarm.

---

## NotificationAlarmReceiver

Uses `goAsync()` to keep the receiver process alive while the coroutine runs on
`Dispatchers.IO`. A `withTimeout(9_000)` safety net prevents the process from being held
longer than Android allows (~10 s budget for `goAsync`).

Execution order per alarm fire:
1. Check `NOTIFICATIONS_ENABLED` Remote Config kill-switch — reschedule next but skip post.
2. Read `masterEnabled` — if off, **do not** re-schedule (alarms stay cancelled until user
   re-enables master).
3. Dispatch to `handleMealAlarm` / `handleStreakAlarm` / `handleWeeklyPlanAlarm`.
4. Read type-specific toggle — skip post if off (alarm still re-schedules).
5. Collect repository data, run `NotificationDecider`, call `NotificationHelper` if yes.
6. Call `rescheduleNext()` — re-arm for next day (or next Monday for `WEEKLY_PLAN`).

The `shouldPostMealAlarm(notificationsEnabled, mealRemindersEnabled, isAlreadyLogged)` helper
is exposed as a `companion object` function for pure unit testing.

> **Key behaviour:** meal reminders are suppressed when the slot already has food logged for
> today (`isAlreadyLogged = true`). This is intentional — the reminder fires only when the
> user hasn't logged yet. Re-scheduling still happens regardless, so the alarm is ready for
> the next day.

---

## NotificationAlarmBootstrapper

Called from `MealPlanApp.onCreate()` and from every `SettingsViewModel` setter to keep alarms
in sync with preferences:

```kotlin
NotificationAlarmBootstrapper.scheduleAll(context)        // reads all prefs, cancel+reschedule all
NotificationAlarmBootstrapper.rescheduleForType(context, type)  // single alarm update
```

`scheduleAll` cancels all alarms first when `masterEnabled == false`, ensuring no stale alarms
fire after the user disables notifications.

---

## NotificationDecider

Unchanged from the WorkManager era — pure Kotlin, no Android dependencies.

```kotlin
NotificationDecider.shouldSendMealReminder(...)
NotificationDecider.shouldSendStreakAlert(...)
NotificationDecider.shouldSendWeeklyPlan(...)
NotificationDecider.computeStreak(completedDateStrings, today)
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

---

## Android Permissions

| Permission | Reason |
|------------|--------|
| `POST_NOTIFICATIONS` | Required at runtime on Android 13+ (API 33) to post notifications. Settings UI handles the request flow. |
| `SCHEDULE_EXACT_ALARM` | Required on API 31+ for `setExactAndAllowWhileIdle`. User can grant via **Settings → Apps → Special app access → Alarms & reminders**. App falls back to `setAndAllowWhileIdle` if not granted. |
| `RECEIVE_BOOT_COMPLETED` | Allows `BootReceiver` to re-schedule alarms after device reboot (AlarmManager alarms do not survive reboots). |

---

## Adding a New Notification Type

1. Add a new entry to `NotificationAlarmType` with a unique `requestCode`.
2. Add a firing condition to `NotificationDecider` (pure Kotlin — write test first).
3. Add a `postXxx()` method to `NotificationHelper` with a unique notification ID.
4. Handle the new type in `NotificationAlarmReceiver.handleAlarm()`.
5. Schedule/cancel it in `NotificationAlarmBootstrapper.scheduleAll()` and `rescheduleForType()`.
6. Add preference keys (if user-configurable) to `NotificationPreferences`.
7. Add a toggle/time-picker row to `SettingsScreen` Notifications section.
8. Add getter + setter to `NotificationState` / `SettingsViewModel`.

---

## Testing

| Test file | What it covers |
|-----------|---------------|
| `AlarmSchedulerTest` | 6 pure JVM tests: `nextTriggerMillis` boundary cases, `nextMondayTriggerMillis` |
| `NotificationPreferencesMinuteTest` | 8 tests: minute constants, default values |
| `NotificationAlarmBootstrapperTest` | 4 tests: `scheduleAll` (master on/off), `rescheduleForType` |
| `NotificationAlarmReceiverTest` | 3 tests: `shouldPostMealAlarm` pure logic |
| `SettingsViewModelMinuteTest` | 10 tests: minute state defaults, minute setters, rescheduling side-effects |
| `SettingsViewModelNotificationTest` | 17 tests: all toggle/hour state defaults and setters |
| `NotificationDeciderTest` | 26 tests: all four pure decision functions |
| `NotificationPreferencesTest` | Default values and constants |

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
