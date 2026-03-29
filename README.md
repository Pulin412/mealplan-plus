# MealPlan+

A meal planning and nutrition tracking app for Android and iOS. Build a personal food database, create reusable diet templates, plan your week on a calendar, log daily intake, and track health metrics — all stored privately on your device.

---

## Features

- **Food Database** — Add foods manually, scan barcodes, or search the USDA / OpenFoodFacts databases
- **Meal Templates** — Create reusable meals from your foods with quantities and units
- **Diet Templates** — Group meals into full-day plans across Breakfast, Lunch, Dinner, and snack slots
- **Calendar Planning** — Assign diet templates to specific dates and plan your week ahead
- **Daily Logging** — Log what you actually ate and compare against your plan
- **Health Metrics** — Track weight, blood glucose (fasting/post-meal/random), HbA1c, and custom metrics with trend charts
- **Grocery Lists** — Auto-generate shopping lists from planned diets for any date range
- **Home Screen Widgets** — Today's plan, diet macro summary, and mini calendar (Android)
- **Google & Email Auth** — Sign in with Google or email/password via Firebase

---

## Platform Support

| Platform | UI | Status |
|----------|----|--------|
| Android | Jetpack Compose | ✅ Complete |
| iOS | SwiftUI | 🚧 In Progress |

---

## Tech Stack

### Android
- **UI** — Jetpack Compose + Material 3
- **Architecture** — MVVM + Repository pattern
- **Database** — Room (SQLite), 17 entities, 21 migrations
- **DI** — Hilt
- **Async** — Kotlin Coroutines + Flow
- **Widgets** — Jetpack Glance
- **Auth** — Firebase Authentication (Email/Password + Google)
- **Networking** — Retrofit + OkHttp
- **Background** — WorkManager

### iOS
- **UI** — SwiftUI
- **Shared logic** — Kotlin Multiplatform (KMP)
- **Database** — SQLDelight (NativeSqliteDriver)
- **Auth** — Firebase Authentication

### Shared (KMP)
- Repositories, database queries, and network layer shared across platforms

---

## Project Structure

```
mealplan-plus/
├── android/          # Android app (Jetpack Compose)
├── ios/              # iOS app (SwiftUI)
├── shared/           # Kotlin Multiplatform shared module
├── backend/          # Backend service (future)
├── docs/             # Full documentation
│   ├── android/
│   │   ├── user-guide/       # End-user feature guides
│   │   └── technical/        # Architecture, schema, contributing
│   ├── ios/
│   │   ├── user-guide/
│   │   └── technical/
│   └── roadmap/              # Future improvements
└── data/             # Seed data (foods, diets)
```

---

## Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog or later |
| JDK | 17+ |
| Android SDK | API 26–34 |
| Firebase project | Required for auth |

### 1. Clone

```bash
git clone https://github.com/Pulin412/mealplan-plus.git
cd mealplan-plus
```

### 2. Firebase Setup

The app requires a Firebase project with **Email/Password** and **Google** sign-in enabled.

1. Go to the [Firebase Console](https://console.firebase.google.com) and create a project.
2. Add an Android app with package name `com.mealplanplus`.
3. Download `google-services.json` and place it in `android/`.
4. Enable **Authentication → Sign-in methods → Email/Password** and **Google**.
5. Copy the **Web client ID** from Authentication → Sign-in methods → Google → Web SDK configuration.
6. Set it in `android/src/main/java/com/mealplanplus/ui/screens/auth/LoginScreen.kt`.

Full details: [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

### 3. Build & Run

```bash
# Build debug APK
./gradlew :android:assembleDebug

# Install on connected device
adb install android/build/outputs/apk/debug/android-arm64-v8a-debug.apk
```

---

## Documentation

| Document | Audience | Link |
|----------|----------|------|
| User Guide (Android) | End users | [docs/android/user-guide/](docs/android/user-guide/README.md) |
| User Guide (iOS) | End users | [docs/ios/user-guide/](docs/ios/user-guide/README.md) |
| Architecture | Contributors | [docs/android/technical/architecture.md](docs/android/technical/architecture.md) |
| Database Schema | Contributors | [docs/android/technical/database-schema.md](docs/android/technical/database-schema.md) |
| Authentication | Contributors | [docs/android/technical/authentication.md](docs/android/technical/authentication.md) |
| Contributing Guide | Contributors | [docs/android/technical/contributing.md](docs/android/technical/contributing.md) |
| Roadmap | Everyone | [docs/roadmap/future-improvements.md](docs/roadmap/future-improvements.md) |

---

## Contributing

1. Fork the repository.
2. Create a branch: `git checkout -b feature/your-feature`
3. Follow the conventions in the [Contributing Guide](docs/android/technical/contributing.md).
4. Open a pull request against `main`.

Commit format: `feat(scope): description` · `fix(scope): description` · `docs: description`

---

## Required for Contributors

Before opening a PR, make sure you have:

- [ ] `android/google-services.json` — from your own Firebase project (never commit this)
- [ ] Firebase project with Email/Password and Google sign-in enabled
- [ ] Android Studio Hedgehog+ with JDK 17
- [ ] A device or emulator running API 26+

Files that must **never** be committed:
- `android/google-services.json` — contains Firebase credentials
- `ios/iosApp/GoogleService-Info.plist` — iOS Firebase credentials
- `.env` files of any kind

Both credential files are listed in `.gitignore`.

---

## License

Private repository. All rights reserved.
