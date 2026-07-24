# android/ — MealPlan+ Android

Kotlin + Compose + Hilt + Room app (the canonical client; the old pre-redesign app was removed).
See root `CLAUDE.md` for the spec location.

## Stack
Kotlin, Compose Material 3, Room, Hilt, Retrofit, Firebase Auth/Crashlytics/RemoteConfig/Analytics.

## Commands
- Unit tests: `./gradlew :android:testDebugUnitTest`
- Build: `./gradlew :android:assembleDebug`

## Hard rules — never break
1. **No fallbackToDestructiveMigration.** Room DB starts at v1; every change needs an explicit `MIGRATION_X_Y`. Migrations need human approval before writing.
2. **Firebase free-tier only.** No Firestore, Functions, Storage, or Realtime DB. The build task `verifyNoBillableFirebaseFeatures` enforces this.
3. **applicationId = `com.mealplanplus.v2`** (debug suffix `.dev`). Historically chosen to install alongside the now-removed old app; changing it to `com.mealplanplus` is an open pre-launch decision (Play Store identity).
4. Commit / push only when explicitly asked (see root `CLAUDE.md`).

## Architecture
`Compose UI → @HiltViewModel (StateFlow) → Repository → Room DAO / Retrofit`

## DB
Starts at version 1 — clean slate. Entities are added per screen as features are built.
`AppDatabase` in `data/local/AppDatabase.kt` — register every new entity there.

## After tests pass
Always ask before installing on a device via adb.
