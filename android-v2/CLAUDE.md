# android-v2/ — MealPlan+ Android (redesign)

Fresh Kotlin + Compose + Hilt + Room app. **Do not import or copy code from `android/`.**
Screens are built one at a time as the design doc is shared — see root `CLAUDE.md` for the spec location.

## Stack
Same as `android/`: Kotlin, Compose Material 3, Room, Hilt, Retrofit, Firebase Auth/Crashlytics/RemoteConfig/Analytics.

## Commands
- Unit tests: `./gradlew :android-v2:testDebugUnitTest`
- Build: `./gradlew :android-v2:assembleDebug`

## Hard rules — never break
1. **No fallbackToDestructiveMigration.** Room DB starts at v1; every change needs an explicit `MIGRATION_X_Y`. Migrations need human approval before writing.
2. **Firebase free-tier only.** No Firestore, Functions, Storage, or Realtime DB. The build task `verifyNoBillableFirebaseFeatures` enforces this.
3. **applicationId = `com.mealplanplus.v2`** so it installs alongside the old app on a device.
4. **`android/` is read-only.** Never modify files in `android/`.
5. Commit / push only when explicitly asked (see root `CLAUDE.md`).

## Architecture
`Compose UI → @HiltViewModel (StateFlow) → Repository → Room DAO / Retrofit`

## DB
Starts at version 1 — clean slate. Entities are added per screen as features are built.
`AppDatabase` in `data/local/AppDatabase.kt` — register every new entity there.

## After tests pass
Always ask before installing on a device via adb.
