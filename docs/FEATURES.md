# Features

What the app does today, and what's planned. Both clients are at parity unless noted.

## Built ✅

| Area | Notes |
|---|---|
| **Foods** | List/compact views, search, sort, favourites, manual add |
| **Barcode scan** | Android: ML Kit + CameraX · Webapp: `@zxing/browser` |
| **Online food search** | Open Food Facts (free, no key). Android calls OFF directly; webapp via a backend proxy (`/foods/search-online`) |
| **Meals** | Meal builder from foods, per-slot logging (breakfast/lunch/dinner) |
| **Diets & Tags** | Browse/create diets, tag system, shopping-list source |
| **Home / Today** | Daily summary, log-by-slot, streaks |
| **Plan** | Assign diets across days |
| **Groceries** | Aisle-grouped shopping list generated from the plan over a date range; editable, reconciling refresh; saved lists |
| **Exercises / Workouts** | Exercise library, workout templates, session runner, logs + calendar |
| **Health** | Glucose / Weight / Blood Pressure metrics with binned trend charts |
| **Settings** | Export CSV (both) · Notifications (Android local + Webapp Web Push reminders) · Health Connect (Android) |
| **Auth** | Firebase (Google + email/password), per-user data |
| **Sync** | Foods/Meals/Diets offline-first on Android; webapp online-first |

Navigation: bottom nav = **Today · Plan · Exercises · Health · More** (More → Foods/Meals/Diets/Groceries). Profile + Settings are reached from Home.

## Planned 🔜

| Item | Notes |
|---|---|
| **AI agent** | Natural-language food logging ("2 eggs and 80g oats" → logged). Server-side, pgvector + Claude. See `docs/agents/`. |
| **Android offline-first refactor** | Move all writes behind a single background sync worker (target design; currently per-operation write-through) |
| **Play Store release** | Needs an upload keystore + signed AAB (workflow placeholder exists); decide final `applicationId` before first upload |
| **Per-user reminder time / timezone** | Web Push reminders currently fire from a single daily cron at server (UTC) time for anyone who hasn't logged; add per-user reminder hour + timezone later |

## Dropped

- **Backup & restore** — redundant with sync (all data is in Neon, keyed to Firebase UID; a reinstall re-syncs). Portability is covered by Export CSV.
