# Contributing Guide — Android

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Android Studio | Hedgehog or later | Recommended IDE |
| JDK | 17+ | Required by AGP 8.x |
| Android SDK | API 26–34 | Min 26, target 34 |
| Gradle | 8.x | Via wrapper (`./gradlew`) |
| Firebase project | — | Required for auth (see Firebase Setup) |

---

## Project Setup

```bash
# 1. Clone the repo
git clone https://github.com/Pulin412/mealplan-plus.git
cd mealplan-plus

# 2. Add your google-services.json
# Copy your Firebase google-services.json to:
cp /path/to/google-services.json android/google-services.json

# 3. Build
./gradlew :android:assembleDebug

# 4. Install on connected device
adb install android/build/outputs/apk/debug/android-arm64-v8a-debug.apk
```

---

## Project Structure

```
android/
├── src/main/java/com/mealplanplus/
│   ├── data/
│   │   ├── local/          DAOs + Room database + type converters
│   │   ├── model/          Room entities + display models
│   │   ├── remote/         Retrofit API interfaces + response DTOs
│   │   └── repository/     Repositories (single source of truth)
│   ├── di/                 Hilt modules (Database, Network, Auth)
│   ├── ui/
│   │   ├── screens/        One folder per feature (screen + ViewModel)
│   │   ├── navigation/     NavHost.kt + Screen sealed class
│   │   ├── components/     Shared Composables
│   │   └── theme/          MaterialTheme config
│   ├── util/               AuthPreferences, CsvExporter, ThemePreferences
│   └── widget/             Glance widgets + WidgetDataRepository
├── src/main/res/
│   ├── xml/                Widget provider XML
│   └── raw/                Seed data JSON files
└── build.gradle.kts
```

---

## Branching Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Production-ready code. Always deployable. |
| `feature/<name>` | New features — branch from `main`, PR back to `main` |
| `fix/<name>` | Bug fixes |
| `docs/<name>` | Documentation only |

**Never commit directly to `main`.** Always use a PR.

---

## Adding a New Feature

### 1. New Screen

1. Create `ui/screens/<feature>/` folder.
2. Add `<Feature>Screen.kt` — a stateless `@Composable` that takes callbacks and `UiState`.
3. Add `<Feature>ViewModel.kt` — `@HiltViewModel`, exposes `StateFlow<UiState>`.
4. Define a `<Feature>UiState` data class.
5. Register the route in `Screen` sealed class.
6. Add `composable(Screen.<Feature>.route)` in `MealPlanNavHost`.
7. Add navigation entry points from parent screens.

### 2. New Database Entity

1. Add entity data class in `data/model/` with `@Entity` annotation.
2. Create a DAO in `data/local/` with `@Dao`.
3. Add the entity to `MealPlanDatabase.entities` list.
4. Write a Room migration (increment `DATABASE_VERSION`).
5. Add the new DAO to `DatabaseModule`.
6. Inject the DAO into the relevant `Repository`.

### 3. New Repository Method

1. Add a `suspend fun` or `Flow`-returning function in the relevant `Repository`.
2. Use `suspend` for one-shot operations, `Flow` for reactive/observed data.
3. Expose it from the ViewModel via a `viewModelScope.launch` or `stateIn`.

---

## Code Conventions

**Kotlin style:**
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use `data class` for UI state — always immutable, update with `.copy()`.
- Prefer `Flow` over callbacks for async data.

**Compose:**
- Screens should be stateless — receive `UiState` and lambdas, no direct ViewModel reference in sub-composables.
- Use `remember` and `rememberSaveable` appropriately — `rememberSaveable` for state that survives config change.
- Always add `contentDescription` to images/icons for accessibility.

**Database:**
- All queries must be `suspend` or return `Flow` — never block the main thread.
- Add indices for any column used in `WHERE` or `JOIN` clauses.
- Write migration SQL in `MealPlanDatabase.MIGRATION_X_Y` — never use `fallbackToDestructiveMigration()`.

**Error handling:**
- Repositories return `Result<T>` for operations that can fail.
- ViewModels fold `Result` into `UiState.error: String?`.
- Never swallow exceptions silently — at minimum `Log.w(TAG, message)`.

---

## Running the App

```bash
# Debug build + install
./gradlew :android:assembleDebug
adb install -r android/build/outputs/apk/debug/android-arm64-v8a-debug.apk

# Run all checks
./gradlew :android:check

# Clean build
./gradlew :android:clean :android:assembleDebug
```

---

## Firebase Setup

The app requires a Firebase project with:
- **Authentication** enabled: Email/Password + Google Sign-in
- `google-services.json` placed in `android/`

See `FIREBASE_SETUP.md` at the project root for step-by-step instructions.

---

## Commit Message Format

```
<type>(<scope>): <short description>

<optional body>

Co-Authored-By: ...
```

Types: `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `chore`

Examples:
```
feat(diets): add tag filtering on Diets screen
fix(auth): handle Firebase EMAIL_ALREADY_IN_USE on sign-up
perf(diets): batch tag queries to eliminate N+1 problem
docs(android): add database schema reference
```
