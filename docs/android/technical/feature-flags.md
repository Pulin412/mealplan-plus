# Feature Flags

All feature flags are defined as entries in the [`FeatureFlag`](../../../android/src/main/java/com/mealplanplus/util/FeatureFlag.kt) enum and managed via **Firebase Remote Config**.

Each flag has:
- A **Remote Config key** (snake_case, ending in `_enabled`)
- A **local default** that is applied before the first successful fetch (cold start, no network, etc.)

Defaults are registered automatically by `RemoteConfigManager.applyDefaults()` on every app start.

---

## Flags

| Flag | Remote Config Key | Default | Status |
|------|------------------|---------|--------|
| `BARCODE_SCANNER` | `barcode_scanner_enabled` | `true` | ✅ Implemented — kill-switch for the CameraX/MLKit barcode scanner |
| `USDA_SEARCH` | `usda_search_enabled` | `true` | ✅ Implemented — disables USDA FoodData Central search if API key is rate-limited |
| `OFF_SEARCH` | `off_search_enabled` | `true` | ✅ Implemented — disables OpenFoodFacts product search and barcode lookup |
| `SYNC` | `sync_enabled` | `false` | ⏳ Backend not yet live — flag is wired but the feature defaults off |
| `GROCERY_AUTO_GENERATE` | `grocery_auto_generate_enabled` | `true` | ✅ Implemented — automatic grocery list generation from a diet's meal plan |
| `HEALTH_TRACKING` | `health_tracking_enabled` | `true` | ✅ Implemented — health metrics tracking screen |
| `ANALYTICS_ENABLED` | `analytics_enabled` | `false` | ✅ Implemented — Firebase Analytics event collection (off until pre-launch privacy review) |

---

## How flags are evaluated

1. On app start, `RemoteConfigManager.applyDefaults()` registers all local defaults with Firebase.
2. In the background, `RemoteConfigManager.fetchAndActivate()` fetches the latest values from Firebase.
3. Call sites read flags via `remoteConfigManager.isEnabled(FeatureFlag.SOME_FLAG)`.

If the fetch fails (no network, quota exceeded, etc.), the last activated values — or the local defaults — are used.

---

## ANALYTICS_ENABLED

`ANALYTICS_ENABLED` is wired at the application level in `MealPlanApp.initRemoteConfig()`:

```kotlin
val analyticsEnabled = remoteConfigManager.isEnabled(FeatureFlag.ANALYTICS_ENABLED)
FirebaseAnalytics.getInstance(this)
    .setAnalyticsCollectionEnabled(analyticsEnabled)
```

**Current state:** `false` by default. No analytics events are sent until this flag is explicitly enabled in the Firebase Remote Config console after the pre-launch privacy review is complete.

**To enable:** Set `analytics_enabled = true` in the Firebase Remote Config console (no app update required).

---

## Adding a new flag

1. Add an entry to `FeatureFlag.kt`:
   ```kotlin
   MY_FLAG("my_flag_enabled", true),
   ```
2. Update the count guard in `FeatureFlagTest.kt`:
   ```kotlin
   assertEquals(8, FeatureFlag.entries.size) // was 7
   ```
3. Add matching key/default tests to `FeatureFlagTest.kt`.
4. Set the default in the Firebase Remote Config console to match `defaultValue`.
5. Update this document.

---

## Remote Config console defaults

The table below matches the values that should be set as defaults in the Firebase Remote Config console so that the console reflects the in-app defaults:

| Key | Default value |
|-----|---------------|
| `barcode_scanner_enabled` | `true` |
| `usda_search_enabled` | `true` |
| `off_search_enabled` | `true` |
| `sync_enabled` | `false` |
| `grocery_auto_generate_enabled` | `true` |
| `health_tracking_enabled` | `true` |
| `analytics_enabled` | `false` |
