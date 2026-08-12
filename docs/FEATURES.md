# Features

What the app does today, and what's planned. Both clients are at parity unless noted.

## Built ✅

| Area | Notes |
|---|---|
| **Foods** | List/compact views, search, sort, favourites, manual add |
| **Barcode scan** | Android: ML Kit + CameraX · Webapp: `@zxing/browser` |
| **Online food search** | Open Food Facts (free, no key). Android calls OFF directly; webapp via a backend proxy (`/foods/search-online`) |
| **Meals** | Meal builder from foods, per-slot logging (9 slots), copy-from-existing, meal notes; plan a meal to today |
| **Diets & Tags** | Browse/create diets, copy-from-existing, diet notes, tag system, shopping-list source |
| **Home / Today** | Daily summary, log-by-slot, streaks; plan workouts/exercises to today (already-planned workouts hidden from the picker) |
| **Plan** | Assign diets across days |
| **Groceries** | Aisle-grouped shopping list generated from the plan over a date range; editable, reconciling refresh; saved lists |
| **Exercises / Workouts** | Exercise library, workout templates, session runner (add/remove exercises on the fly, mark-done, per-exercise + whole-workout notes, per-workout "last time"), logs + calendar |
| **Social** | Follow, share diets/meals/workouts, copy others' items to your library, block/report, in-app notifications (pull-based, no FCM) |
| **Health** | Glucose / Weight / Blood Pressure metrics with binned trend charts, paginated readings |
| **Onboarding** | First-run guided tour + onboarding data step (both clients) |
| **Feedback** | In-app feedback form → `POST /feedback` (both clients) |
| **Settings** | Export CSV (both) · Notifications (Android local + Webapp Web Push reminders) · Health Connect (Android) |
| **Auth** | Firebase (Google + email/password), per-user data |
| **Sync** | Foods/Meals/Diets offline-first on Android + read-through cache for Today/Plan/Health/Exercises; webapp online-first |

Navigation: bottom nav = **Today · Plan · Exercises · Health · More** (More → Foods/Meals/Diets/Groceries). Profile + Settings are reached from Home.

## Planned 🔜

| Item | Notes |
|---|---|
| **AI agent** | Natural-language food logging ("2 eggs and 80g oats" → logged). Server-side, pgvector + Claude. See `docs/agents/`. |
| **Native iOS app** | Dedicated App Store app (SwiftUI) with HealthKit + notifications parity — **only after the PWA + Android are stable**. Reuses the backend + API contract as-is; UI/local-store/sync are net-new in Swift. Prep now: extract design tokens to one neutral source (see ARCHITECTURE → Design system). Reverses the current "iPhone = PWA" stance. |
| **Android offline-first refactor** | Move all writes behind a single background sync worker (target design; currently per-operation write-through) |
| **Play Store release** | Needs an upload keystore + signed AAB (workflow placeholder exists); decide final `applicationId` before first upload |
| **Per-user reminder time / timezone** | Web Push reminders currently fire from a single daily cron at server (UTC) time for anyone who hasn't logged; add per-user reminder hour + timezone later |

## Dropped

- **Backup & restore** — redundant with sync (all data is in Neon, keyed to Firebase UID; a reinstall re-syncs). Portability is covered by Export CSV.
