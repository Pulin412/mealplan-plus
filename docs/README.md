# MealPlan+ Documentation

**Version:** 3.0 | **Last Updated:** March 2026 | **Status:** Active Development

MealPlan+ is a meal planning and nutrition tracking app for Android and iOS. It helps users manage dietary intake through structured meal planning, food logging, and health metrics monitoring.

---

## Documentation Index

### For Users

| Platform | Document | Description |
|----------|----------|-------------|
| Android | [Getting Started](android/user-guide/getting-started.md) | Installation, account setup, first steps |
| Android | [Food Database](android/user-guide/features/food-database.md) | Adding foods, barcode scanner, online search |
| Android | [Meals](android/user-guide/features/meals.md) | Creating and managing meal templates |
| Android | [Diets](android/user-guide/features/diets.md) | Diet plans, meal slots, tags |
| Android | [Daily Logging](android/user-guide/features/daily-logging.md) | Logging what you eat each day |
| Android | [Meal Planning](android/user-guide/features/meal-planning.md) | Planning meals on the calendar |
| Android | [Health Metrics](android/user-guide/features/health-metrics.md) | Tracking weight, blood glucose, and more |
| Android | [Grocery Lists](android/user-guide/features/grocery-lists.md) | Auto-generating and managing shopping lists |
| Android | [Widgets](android/user-guide/features/widgets.md) | Home screen widgets |
| Android | [FAQ](android/user-guide/faq.md) | Common questions and answers |
| iOS | [Getting Started](ios/user-guide/getting-started.md) | Installation, account setup, first steps |
| iOS | [Features Overview](ios/user-guide/features/) | All iOS features |

### For Contributors

| Document | Description |
|----------|-------------|
| [Android Architecture](android/technical/architecture.md) | System design, patterns, tech stack |
| [Database Schema](android/technical/database-schema.md) | All entities, relationships, migrations |
| [Authentication](android/technical/authentication.md) | Auth system, Firebase integration |
| [Navigation](android/technical/navigation.md) | Navigation architecture and deep links |
| [Widgets](android/technical/widgets.md) | Glance widget system |
| [Contributing Guide](android/technical/contributing.md) | How to set up and contribute |
| [iOS Architecture](ios/technical/architecture.md) | iOS-specific architecture |

### Product & Roadmap

| Document | Description |
|----------|-------------|
| [Future Improvements](roadmap/future-improvements.md) | Planned features and Firebase integrations |

---

## Quick Overview

```
MealPlan+
├── Android (Jetpack Compose + Room + Hilt)
├── iOS     (SwiftUI + SQLDelight)
└── Shared  (Kotlin Multiplatform — business logic)
```

**Core concept:** Build a food database → compose foods into meals → group meals into diet templates → assign diets to calendar days → log what you actually eat → compare planned vs actual.
