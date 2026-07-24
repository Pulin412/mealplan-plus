# Future Improvements & Roadmap

**Last updated:** March 2026

This document tracks planned improvements, their rationale, implementation approach, and priority. Items are organised by priority within each category.

---

## Priority Order

| Priority | Item | Effort | Impact |
|----------|------|--------|--------|
| 🔴 P1 | Firebase Crashlytics | Low | High — visibility before launch |
| 🔴 P1 | Firebase Remote Config (Feature Flags) | Medium | High — control over rollout |
| 🟡 P2 | Cloud Firestore Sync | High | High — cross-device data |
| 🟡 P2 | Firebase Analytics | Low | Medium — usage insights |
| 🟢 P3 | Firebase Cloud Messaging (Push Notifications) | Medium | Medium — engagement |
| 🟢 P3 | iOS Widgets | Medium | Medium — parity with Android |
| 🟢 P3 | iOS Barcode Scanner | Low | Medium — parity with Android |
| ⚪ P4 | Nutritionist/Multi-user sharing | High | Low — future market |

---

## 🔴 P1 — Firebase Crashlytics

### What
Automatic crash reporting with full stack traces, device info, OS version, and affected user count — all visible in the Firebase console in real time.

### Why Now
This is the single most valuable thing to add before any public release. Without it, you are flying blind. When users encounter a crash, you have no visibility into what went wrong, on which device, or how many people are affected.

### What You Get
- **Crash-free users rate** — percentage of users who have not experienced a crash
- **Full stack trace** — exact file, line number, and call stack at the time of crash
- **Device & OS breakdown** — which Android/iOS versions are crashing
- **Non-fatal events** — log important errors that did not crash the app but indicate issues
- **Alerts** — email/Slack notification when crash rate spikes

### Implementation (Android)

**1. Add dependency:**
```kotlin
// android/build.gradle.kts
implementation("com.google.firebase:firebase-crashlytics-ktx")

// root build.gradle.kts plugins block
id("com.google.firebase.crashlytics")
```

**2. Initialise (automatic via google-services plugin).**
No code changes needed — Crashlytics activates automatically.

**3. Optional: log non-fatal events:**
```kotlin
// In repositories or ViewModels for important errors:
FirebaseCrashlytics.getInstance().recordException(e)
```

**4. Optional: set user identifier:**
```kotlin
// After sign-in, to correlate crashes with users:
FirebaseCrashlytics.getInstance().setUserId(userId.toString())
```

**Effort:** ~1 hour
**Cost:** Free (unlimited crash reports on Spark plan)

---

## 🔴 P1 — Firebase Remote Config (Feature Flags)

### What
A key-value store hosted in Firebase that the app fetches at runtime. Allows toggling features on/off, adjusting thresholds, and running A/B tests — all without releasing a new APK.

### Why
- **Safe rollouts:** Enable a new feature for 10% of users first, watch Crashlytics, then roll out to 100%.
- **Kill switch:** If a feature causes problems in production, disable it instantly without waiting for a store review.
- **A/B testing:** Show different UI variants to different user segments and measure which performs better.
- **Configuration:** Change things like API timeouts, max grocery items, or feature thresholds without a release.

### Proposed Feature Flags

| Flag Key | Type | Default | Description |
|----------|------|---------|-------------|
| `cloud_sync_enabled` | Boolean | `false` | Enable Firestore cloud sync |
| `push_notifications_enabled` | Boolean | `false` | Enable FCM notifications |
| `analytics_enabled` | Boolean | `true` | Enable Firebase Analytics |
| `barcode_scanner_enabled` | Boolean | `true` | Enable barcode scan feature |
| `max_custom_diets` | Int | `50` | Cap on user-created diets |
| `onboarding_v2_enabled` | Boolean | `false` | New onboarding flow experiment |

### Implementation (Android)

**1. Add dependency:**
```kotlin
implementation("com.google.firebase:firebase-config-ktx")
```

**2. Create a FeatureFlags singleton:**
```kotlin
@Singleton
class FeatureFlags @Inject constructor() {
    private val remoteConfig = Firebase.remoteConfig

    init {
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate()
    }

    val cloudSyncEnabled: Boolean
        get() = remoteConfig.getBoolean("cloud_sync_enabled")

    val pushNotificationsEnabled: Boolean
        get() = remoteConfig.getBoolean("push_notifications_enabled")
}
```

**3. Create `res/xml/remote_config_defaults.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<defaultsMap>
    <entry>
        <key>cloud_sync_enabled</key>
        <value>false</value>
    </entry>
    <entry>
        <key>push_notifications_enabled</key>
        <value>false</value>
    </entry>
</defaultsMap>
```

**4. Use in code:**
```kotlin
// In ViewModel or Repository:
if (featureFlags.cloudSyncEnabled) {
    // execute sync logic
}
```

**Effort:** ~1 day (setup + wrapping all feature-gated code)
**Cost:** Free (1M fetches/day on Spark plan)

---

## 🟡 P2 — Cloud Firestore Sync

### What
Sync user data (meals, diets, food database, logs, health metrics) to Firebase Cloud Firestore. This enables:
- **Cross-device restore** — sign in on a new phone and get all your data back
- **Data backup** — protection against device loss or factory reset
- **Future multi-device** — eventually view/edit from a second device

### Data Architecture

Each user gets their own isolated collection tree in Firestore:

```
/users/{userId}/
    ├── profile/
    ├── foods/{foodId}
    ├── meals/{mealId}
    │     └── foods/{mealFoodId}
    ├── diets/{dietId}
    │     └── meals/{dietMealId}
    ├── logs/{date}
    │     └── foods/{loggedFoodId}
    ├── plans/{date}
    ├── health_metrics/{metricId}
    └── grocery_lists/{listId}
          └── items/{itemId}
```

### Sync Strategy: Local-First

The app treats the **local Room database as the source of truth**. Firestore is a remote replica:

```
User action
    │
    ▼
Write to Room (immediate, UI updates instantly)
    │
    ▼
SyncWorker (background, periodic)
    │
    ├── Query entities where syncedAt IS NULL or < lastModifiedAt
    ├── Write changed entities to Firestore
    └── Update syncedAt = now
```

On first sign-in on a new device:
```
No local data detected
    │
    ▼
Fetch all documents from Firestore (one-time restore)
    │
    ▼
Write to Room
    │
    ▼
Normal local-first operation resumes
```

### Conflict Resolution
- **Last-write-wins** — the record with the most recent `updatedAt` timestamp wins.
- Suitable for this app since the same data is rarely edited on two devices simultaneously.

### Offline Support
Firestore's SDK has built-in offline caching. Writes made offline are queued and synced automatically when connectivity restores.

### Implementation Steps

1. Enable Cloud Firestore in Firebase console.
2. Set security rules: `allow read, write: if request.auth.uid == userId`
3. Add `serverId` and `syncedAt` columns (already present in schema v21).
4. Add Firestore dependency: `implementation("com.google.firebase:firebase-firestore-ktx")`
5. Gate behind `FeatureFlags.cloudSyncEnabled` from Remote Config.
6. Implement `FirestoreSyncRepository` with upload/download methods.
7. Update `SyncWorker` to call `FirestoreSyncRepository.sync()`.
8. Add first-launch restore detection and flow.

**Effort:** ~2–3 weeks
**Cost:** Spark plan free tier: 1GB storage, 50K reads/day, 20K writes/day — sufficient for early users. Upgrade to Blaze (pay-per-use) when scaling.

---

## 🟡 P2 — Firebase Analytics

### What
Automatic and custom event tracking. Understand how users navigate the app, which features they use, and where they drop off.

### Why
Once you have real users, Analytics answers:
- Which screens do users visit most?
- Where do they drop off in the onboarding flow?
- How many users actually use the barcode scanner?
- Which diet templates are most popular?

### What You Get Automatically (no code)
- Screen views (each `@Composable` navigation destination)
- Session duration
- User retention (1-day, 7-day, 28-day)
- Device, OS, country breakdown

### Custom Events to Add

| Event | When | Parameters |
|-------|------|-----------|
| `diet_created` | User saves a new diet | `diet_name` |
| `meal_logged` | User logs a meal | `slot_type`, `meal_count` |
| `barcode_scanned` | Barcode scan attempted | `result: found/not_found` |
| `grocery_list_generated` | Auto-generated grocery list | `date_range_days` |
| `health_metric_logged` | Health metric recorded | `metric_type` |
| `diet_assigned_to_date` | Diet assigned on calendar | |
| `password_reset_requested` | Forgot password tapped | |

### Implementation (Android)

**1. Add dependency:**
```kotlin
implementation("com.google.firebase:firebase-analytics-ktx")
```

**2. Log custom events:**
```kotlin
Firebase.analytics.logEvent("diet_created") {
    param("diet_name", dietName)
}
```

**3. Gate behind feature flag:**
```kotlin
if (featureFlags.analyticsEnabled) {
    Firebase.analytics.logEvent(...)
}
```

**Effort:** ~1 day (dependency + ~10 custom event calls)
**Cost:** Free, unlimited events on Spark plan

---

## 🟢 P3 — Firebase Cloud Messaging (Push Notifications)

### What
Send push notifications to users' devices via Firebase Cloud Messaging (FCM).

### Proposed Notifications

| Notification | Trigger | Message |
|-------------|---------|---------|
| Daily log reminder | 8pm if no food logged today | "You haven't logged your meals today 🍽" |
| Morning planning reminder | 7am if no diet assigned today | "Plan your meals for today 📋" |
| Weekly summary | Sunday evening | "Your week in review — tap to see your stats" |
| Blood glucose reminder | Configurable time | "Time to log your fasting glucose 🩸" |

### Architecture

```
Firebase Console (or backend)
    │  sends FCM message
    ▼
FCM SDK on device
    │
    ▼
MealPlanFirebaseMessagingService extends FirebaseMessagingService
    │
    ├── onMessageReceived() → show local notification
    └── onNewToken()        → store token for targeting
```

### Implementation Steps

1. Add dependency: `implementation("com.google.firebase:firebase-messaging-ktx")`
2. Create `MealPlanFirebaseMessagingService`.
3. Request `POST_NOTIFICATIONS` permission (Android 13+).
4. Register service in `AndroidManifest.xml`.
5. Store FCM token against user in Firestore (when cloud sync is available).
6. Add notification preferences in Settings screen.
7. Gate behind `FeatureFlags.pushNotificationsEnabled`.

**Effort:** ~3 days (service + permission flow + preferences UI)
**Cost:** Free, unlimited messages on Spark plan

---

## 🟢 P3 — iOS Widgets

### What
Home screen widgets for iOS equivalent to the Android Glance widgets.

### Proposed Widgets
- **Today's Plan** — shows planned meals for today
- **Diet Summary** — calorie ring with macro totals

### Implementation
- Use **WidgetKit** (iOS 14+) with SwiftUI widget views.
- Share data with the main app via **App Groups** and `UserDefaults(suiteName:)`.
- Widget timeline reloads triggered by the main app after data changes.

**Effort:** ~1 week

---

## 🟢 P3 — iOS Barcode Scanner

### What
Barcode scanning using the device camera to look up food products on iOS.

### Implementation
- Use **AVFoundation** for camera access and barcode detection.
- Feed the barcode to the same OpenFoodFacts/USDA API calls used on Android (via KMP shared networking).

**Effort:** ~3 days

---

## ⚪ P4 — Nutritionist / Multi-User Sharing

### What
Allow a nutritionist or caregiver to create diet templates and share them with a patient/client.

### Approach
- Shared Firestore collection `/shared_diets/{shareCode}`
- Client enters a share code to import the diet template
- Read-only import — client cannot edit the shared template directly

**Effort:** ~2 weeks (requires Firestore + UI for sharing flow)

---

## Technical Debt

| Item | Priority | Description |
|------|----------|-------------|
| iOS feature parity | 🟡 P2 | Barcode scanner, widgets, CSV export |
| Unit tests | 🟡 P2 | ViewModels and Repositories have no unit test coverage |
| Accessibility audit | 🟡 P2 | Screen reader and content description audit |
| Backend API | 🟢 P3 | The `MealPlanApi` Retrofit client points to localhost — backend is not deployed |
| App Store submission | 🔴 P1 | Privacy policy, screenshots, app review preparation |

---

## Release Checklist (Before Public Launch)

- [ ] Add Firebase Crashlytics
- [ ] Add Firebase Remote Config with feature flags
- [ ] Publish privacy policy
- [ ] Add "Data is stored locally" disclosure in App Store description
- [ ] Test on multiple Android versions (API 26, 30, 34)
- [ ] Test on small screen (360dp width)
- [ ] Verify all confirmation dialogs before destructive actions
- [ ] Verify forgot-password email delivery
- [ ] Test cold start performance
- [ ] Test widget behaviour after device reboot
