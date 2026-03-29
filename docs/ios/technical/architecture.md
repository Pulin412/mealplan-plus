# iOS Architecture

**Tech Stack:** Swift · SwiftUI · SQLDelight · Firebase Authentication · Ktor (via KMP shared module)

---

## Overview

The iOS app is a **native SwiftUI application** that consumes the shared Kotlin Multiplatform (KMP) business logic layer for repositories and database access.

```
┌────────────────────────────────────────────────────┐
│                   iOS App                          │
│                                                    │
│  ┌──────────────────────────────────────────────┐  │
│  │         PRESENTATION (SwiftUI)               │  │
│  │                                              │  │
│  │  @State / @Observable ViewModels             │  │
│  │  NavigationStack + TabView                   │  │
│  │  Manual DI (no framework)                    │  │
│  └──────────────────┬───────────────────────────┘  │
│                     │ calls                        │
│  ┌──────────────────▼───────────────────────────┐  │
│  │    SHARED KMP MODULE (import shared)         │  │
│  │                                              │  │
│  │  Repositories (Kotlin → Swift via @ObjC)     │  │
│  │  SQLDelight queries (NativeSqliteDriver)      │  │
│  │  Ktor HTTP client (Darwin engine)             │  │
│  └──────────────────────────────────────────────┘  │
│                                                    │
│  ┌──────────────────────────────────────────────┐  │
│  │     iOS-SPECIFIC SERVICES                    │  │
│  │                                              │  │
│  │  Firebase Auth (GoogleService-Info.plist)    │  │
│  │  BackgroundSyncScheduler (BGTaskScheduler)   │  │
│  │  NSUserDefaults (preferences)                │  │
│  │  URLSession (fallback networking)            │  │
│  └──────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────┘
```

---

## Navigation

```
@main MealPlanPlusApp
    └── ContentView
            └── AppNavigation (NavigationStack + TabView)
                    ├── HomeView
                    ├── DailyLogView
                    ├── MealPlanView (Calendar)
                    ├── HealthView
                    └── MoreView
                            ├── MealsView
                            ├── DietsView
                            ├── FoodsView
                            ├── GroceryView
                            └── SettingsView
```

---

## KMP Integration

The shared module exposes Kotlin coroutines `Flow` as callbacks that the iOS app bridges using `FlowCollector.swift`:

```swift
// FlowCollector.swift bridges Kotlin Flow → Swift async
func collect<T>(_ flow: Kotlinx_coroutines_coreFlow,
                onChange: @escaping (T) -> Void)
```

Repositories from the shared module are instantiated once and passed through the view hierarchy via the environment or initialiser injection.

---

## Authentication

Firebase is configured in `AppDelegate`:
```swift
FirebaseApp.configure()
```

Sign-in uses the same Firebase SDK as Android — accounts created on one platform work on the other.

---

## Database

The shared module uses **SQLDelight** with `NativeSqliteDriver` on iOS:
```kotlin
// In iosMain
actual fun createDriver(schema: SqlSchema, name: String): SqlDriver =
    NativeSqliteDriver(schema, name)
```

The database file is stored in the app's Documents directory.

---

## Background Sync

`BackgroundSyncScheduler` uses `BGTaskScheduler` to register a background task that syncs data with the backend when the device is idle and connected.

---

## Differences vs Android

| Feature | Android | iOS |
|---------|---------|-----|
| UI framework | Jetpack Compose | SwiftUI |
| DI | Hilt | Manual / environment |
| DB driver | AndroidSqliteDriver | NativeSqliteDriver |
| Preferences | DataStore | NSUserDefaults |
| Background work | WorkManager | BGTaskScheduler |
| Widgets | Jetpack Glance | Not yet implemented |
| Barcode scanner | ML Kit | Not yet implemented |
