# MealPlan+ — Product Roadmap

> Last updated: May 2, 2026  
> Track progress via [GitHub Issues](https://github.com/Pulin412/mealplan-plus/issues)
>
> **Design spec:** `design-future.html` (committed to `main`) — interactive mockups for all 19 screens across every phase. Open in a browser and use the group tabs to navigate. This file is the single source of visual truth for Android (Compose) and Web (Next.js/Tailwind).

---

## Architecture Decisions (agreed, not up for re-discussion)

| Decision | Choice | Reason |
|---|---|---|
| Android ↔ Web sharing | **None — clean split** | KMP adds complexity; backend API is the shared layer |
| Web framework | **Next.js + TypeScript** | Mature PWA support, works on iOS Safari, large ecosystem |
| Backend role | **Source of truth for all user data** | Enables sync, backup/restore, web, and AI from one place |
| Sync strategy | **Last-write-wins on `updatedAt`** | Simple, correct for a single-user health app |
| AI backend | **Spring AI + PgVectorStore (RAG)** | Spring-native, provider-agnostic, same Postgres DB |
| AI Android | **Cloud + Gemini Nano hybrid** | Cloud for depth, on-device for offline/speed |
| iOS | **Dropped from roadmap** | Replaced by PWA on iPhone via web app |

---

## Repository Layout (target state)

```
mealplan-plus/
├── android/          ← Kotlin, Compose, Room, Hilt — fully self-contained
├── backend/          ← Spring Boot 3 + Kotlin — source of truth
├── webapp/           ← Next.js 14 + TypeScript — PWA (also serves as Apple PWA)
├── shared/           ← DISCONNECTED — to be archived after Foundation phase
├── backup/           ← seed data + DB backup files (temporary)
├── CLAUDE.md         ← AI assistant context (always keep up to date)
└── ROADMAP.md        ← this file
```

---

## Foundation — Android Stability + Design System
> **Goal:** Clean, stable Android app with a polished minimalist design as the v1.0 baseline before sync work begins.

| GH Issue | Task | Status |
|---|---|---|
| [#81](https://github.com/Pulin412/mealplan-plus/issues/81) | Remove `shared/` KMP module dependency from Android | ✅ Done |
| [#82](https://github.com/Pulin412/mealplan-plus/issues/82) | Stabilise Room schema — document v32 as clean baseline | ✅ Done |
| [#83](https://github.com/Pulin412/mealplan-plus/issues/83) | Merge `feature/foundation` → `main`, single app going forward | ✅ Done |
| [#98](https://github.com/Pulin412/mealplan-plus/issues/98) | Android — Implement minimalist UI redesign across all screens | ✅ Done |

### Foundation Checklist
- [x] #81 — `MigrationRunner.kt` and `RoomToSQLDelightMigration.kt` removed; `:shared` dependency removed from `settings.gradle.kts`
- [x] #82 — Schema exported up to v32 in `android/schemas/`; `docs/DATABASE_SCHEMA.md` written; `SeederIdempotencyTest.kt` added in `androidTest/`
- [x] #83 — `feature/foundation` merged to `main` (April 15, 2026); `.dev` app ID suffix removed; single codebase, single app
- [x] #98 — Full minimalist redesign across all 19 screens; `DesignTokens.kt` with dynamic light/dark tokens; `FormComponents.kt` for shared UI; user-scoped meals & diets (Room v32); backup data imported; swipe navigation; meal names in log slots; interactive diet picker with meal detail

### What was delivered in Foundation
- Full minimalist design system: `DesignTokens.kt`, `FormComponents.kt`, global font scale, dark mode across all screens
- All 19 screens redesigned to match `design-future.html`
- User-scoped meals and diets (Room schema v30→v32; `userId` on `meals` + `diets` tables)
- `BackupDataImporter` — one-time import of all 3 users' data from `backup/mealplan_data_export.json`
- `AlarmManager` notification system (5 alarm types; replaced old WorkManager notification workers)
- Health Connect integration (steps, calories burned, weight from fitness watches)
- Streak & Stats screen; Grocery screens; Auth redesign; Profile screen
- Diets, Meals, Foods accessible from More sheet with full navigation
- `design-future.html` stays as the design reference for all future phases

---

## Phase 1 — Backend Sync API
> **Goal:** Backend becomes the source of truth. Android syncs data. Backup/restore is a free side-effect.  
> **Unblocks:** Phase 3 (web app needs API), Phase 4 (AI needs data in Postgres)

| GH Issue | Task | Status |
|---|---|---|
| [#84](https://github.com/Pulin412/mealplan-plus/issues/84) | Design OpenAPI spec for all core domain endpoints | ✅ Done |
| [#85](https://github.com/Pulin412/mealplan-plus/issues/85) | Backend — implement all domain JPA entities + CRUD endpoints | ✅ Done |
| [#86](https://github.com/Pulin412/mealplan-plus/issues/86) | Backend — implement delta sync push/pull + enable pgvector | ✅ Done |
| [#87](https://github.com/Pulin412/mealplan-plus/issues/87) | Android — SyncWorker calling backend push/pull | ✅ Done |
| [#88](https://github.com/Pulin412/mealplan-plus/issues/88) | Backend — Cloud Run deployment + CI/CD pipeline | ✅ Done |

### Phase 1 Checklist

**Backend setup (start here)**
- [ ] #84 — Add `springdoc-openapi`; design DTOs for all domains; define sync metadata fields; export `backend/docs/openapi.json`
- [ ] #85 — JPA entities for: FoodItem, Meal, MealFoodItem, Diet, DietSlot, Tag, DailyLog, LoggedFood, HealthMetric, GroceryList, Plan, PlannedSlot; Flyway migrations; service + controller layers; Firebase JWT scoping; unit tests
- [ ] #86 — `GET /api/v1/sync/pull?since=<epochMs>`; `POST /api/v1/sync/push`; soft-delete tombstones; enable pgvector on Neon.tech (`CREATE EXTENSION IF NOT EXISTS vector`); create `entity_embeddings` table

**Android sync**
- [ ] #87 — Retrofit `MealPlanApiService`; update `SyncRepository` push/pull logic; store `lastSyncTimestamp` in DataStore; Firebase token injection; sync status on HomeScreen; unit tests for conflict resolution

**Infrastructure**
- [ ] #88 — `Dockerfile` + `docker-compose.yml`; GitHub Actions deploy job; Cloud Run secrets; health check endpoint; update `CLAUDE.md` with deployed URL

### Key Design Notes
- Sync metadata already on Android entities: `updatedAt`, `syncedAt`, `serverId` — use these
- Conflict resolution: if `updatedAt` (client) > `updatedAt` (server) → client wins, else server wins
- pgvector: enable NOW even though AI is Phase 4 — seeds months of embeddings before you need them
- OpenAPI spec (`backend/docs/openapi.json`) auto-generates TypeScript types for the web app

---

## Phase 2 — Workout Logging
> **Goal:** Add workout tracking alongside nutrition, using the same sync infrastructure.  
> **Depends on:** Phase 1 sync API deployed  
> **Design spec:** `design-future.html` → _Workouts_ group (3 screens: Workout Home, Active Session, Exercise Catalogue)

| GH Issue | Task | Status |
|---|---|---|
| [#89](https://github.com/Pulin412/mealplan-plus/issues/89) | Android — Workout domain model and Room entities (v33 migration) | ✅ Done |
| [#90](https://github.com/Pulin412/mealplan-plus/issues/90) | Android — Workout screens (Log, History, Exercise catalogue, edit mode) | ✅ Done |
| [#91](https://github.com/Pulin412/mealplan-plus/issues/91) | Backend — Workout JPA entities + sync extension | ⬜ Open |

### Phase 2 Checklist

**Android**
- [x] #89 — `Exercise`, `WorkoutSession`, `WorkoutSet` entities; DAOs; Room migration; `WorkoutRepository`; `exercises.json` asset + `ExerciseSeeder` with version guard
- [x] #90 — `WorkoutLogScreen`, `WorkoutHistoryScreen`, `ExerciseCatalogueScreen`; workout templates; edit mode for past sessions; plan screen workout names; add "Log Workout" to Quick Add FAB; register routes in NavHost

**Backend**
- [ ] #91 — `Exercise`, `WorkoutSession`, `WorkoutSet` JPA entities; Flyway migration; CRUD endpoints; include in `/sync/pull` + `/sync/push`; OpenAPI docs

### Key Design Notes
- Domain model: `Exercise` (catalogue) → `WorkoutSession` (per occurrence) → `WorkoutSet` (sets/reps/weight/duration)
- Design entities compatible with **Android Health Connect** schema — makes future integration free
- Use same `updatedAt/syncedAt/serverId` sync pattern from Phase 1 — no new protocol needed
- Exercise categories: STRENGTH, CARDIO, FLEXIBILITY, OTHER

---

## Phase 3 — Web App (Next.js PWA)
> **Goal:** Full-featured web app that works as a PWA on iPhone (replacing the need for an iOS native app).  
> **Depends on:** Phase 1 backend API deployed and stable  
> **Design spec:** `design-future.html` — **all groups apply here**. The web app should share the same design language as the Android redesign (#98). Adapt layout to desktop (sidebar nav) and mobile web (bottom nav), but keep the same tokens, card patterns, and color system.

| GH Issue | Task | Status |
|---|---|---|
| [#92](https://github.com/Pulin412/mealplan-plus/issues/92) | Web App — Project scaffold (Next.js + TypeScript + PWA + Firebase Auth) | ✅ Done |
| [#93](https://github.com/Pulin412/mealplan-plus/issues/93) | Web App — Core screens (Dashboard, Diets, Meals, Daily Log, Calendar, Foods, Exercises, Workouts) | ✅ Done |
| [#94](https://github.com/Pulin412/mealplan-plus/issues/94) | Web App — Health Metrics, Grocery, Settings + data export | ✅ Done |

### Phase 3 Checklist

**Scaffold (do first, blocks everything else)**
- [ ] #92 — `npx create-next-app@latest webapp/`; TypeScript strict; Tailwind + shadcn/ui; Firebase Auth (Google + email); `next-pwa`/`serwist`; `public/manifest.json`; `openapi-typescript` codegen (`npm run gen:api`); typed API client; GitHub Actions lint+build job

**Core features**
- [ ] #93 — Layout (sidebar desktop / bottom nav mobile); Dashboard; Diets; Meals; Daily Log; Calendar; offline service worker cache; mobile-responsive for iPhone PWA

**Remaining domains**
- [ ] #94 — Health charts (recharts); Grocery (mark as bought); Settings; JSON data export (backup)

### Key Design Notes
- TypeScript types auto-generated: `npm run gen:api` reads `backend/docs/openapi.json` — never hand-write API types
- Same Firebase project as Android — tokens work immediately, no backend auth changes
- PWA on iOS: requires `display: standalone` in manifest + HTTPS; push notifications limited to iOS 16.4+
- Offline: service worker caches last API responses; show "You are offline" banner; no stale writes

---

## Phase 3a — Web App Parity with Android
> **Goal:** Close all feature gaps between the webapp and Android identified by the android-app-spec.yaml parity matrix.  
> **Depends on:** Phase 3 scaffold + Phase 1 backend (both ✅ done)  
> **Reference:** `docs/android-app-spec.yaml` → `parity` section is the source of truth for this phase.

| GH Issue | Task | Backend change? | Status |
|---|---|---|---|
| [#99](https://github.com/Pulin412/mealplan-plus/issues/99) | Day Planning — server-backed plan screen + apply diet to log | ✅ needs `/plans` endpoints | ✅ Done |
| [#100](https://github.com/Pulin412/mealplan-plus/issues/100) | Health Charts + Streak counter | ❌ frontend only | ✅ Done |
| [#101](https://github.com/Pulin412/mealplan-plus/issues/101) | Workout Templates — full CRUD + pyramid set display + log from template | ✅ needs `/workout-templates` endpoints | ✅ Done |
| [#102](https://github.com/Pulin412/mealplan-plus/issues/102) | Diet enhancements — tags display, duplicate, generate grocery list | ✅ needs duplicate + grocery-from-diet endpoints | ✅ Done |
| [#103](https://github.com/Pulin412/mealplan-plus/issues/103) | Profile & Settings — edit profile, TDEE calc, dark mode toggle, font scale, data export | ✅ needs `PUT /users/me` | ✅ Done |
| [#104](https://github.com/Pulin412/mealplan-plus/issues/104) | Sync push + food favorites | ❌ frontend only (sync client already partially done) | ✅ Done |

### Phase 3a — Full Pending Feature List

**#99 — Day Planning (server-backed)**
- [x] Backend: `GET/PUT/DELETE /api/v1/plans/{date}` — store `(firebaseUid, date, dietId)` per day; V4 Flyway migration
- [x] Plan screen: replaced localStorage with real API; diet picker; assign/remove per day
- [x] Apply diet to day: on Log screen, "Apply diet" button → load diet meals → pre-fill all 3 slots

**#100 — Health Charts + Streak**
- [x] Weight trend chart on Health screen (recharts LineChart, last 30 entries)
- [x] Calorie trend chart (recharts BarChart from daily_logs, last 30 days)
- [x] Streak counter: calculate client-side from `GET /api/v1/daily-logs` (consecutive days with ≥1 logged food)
- [x] Stats cards: latest weight, 30-day avg weight, streak, total logged days

**#101 — Workout Templates**
- [x] Backend: V6 migration; `WorkoutTemplate` + `TemplateExercise` entities; full CRUD endpoints; `POST /{id}/start` creates pre-filled session
- [x] Workouts page restructured: Log | Templates | History tab switcher
- [x] Create/edit template: name, category, add exercises with targetSets/reps/weightKg
- [x] Template card: expand to see exercise breakdown (N × reps @ kg); edit + delete inline
- [x] "Start session from template" → server creates full session → prepended to History tab

**#102 — Diet Enhancements**
- [x] Backend: `POST /api/v1/diets/{id}/duplicate` — create copy with "(copy)" suffix
- [x] Backend: `POST /api/v1/grocery-lists/from-diet/{dietId}` — aggregate grams per food across all diet meals
- [x] Diets page: duplicate button per diet card
- [x] Grocery page: "From diet" panel → diet picker → Generate button

**#103 — Profile & Settings**
- [x] Backend: `PUT /api/v1/users/me`; V5 migration (age, weightKg, heightCm, gender, activityLevel, targetCalories, goalType)
- [x] Settings screen: editable profile form with save; stats preview strip when not editing
- [x] TDEE calculator: live preview (Mifflin-St Jeor formula, all activity multipliers)
- [x] Dark mode toggle (CSS `dark` class on `<html>`)
- [x] Data export: `GET /api/v1/sync/pull?since=epoch` → JSON blob → browser download

**#104 — Sync Push + Food Favorites**
- [x] Food favorites: star toggle on Foods page; `PATCH /api/v1/foods/{id}/favorite` backend endpoint; V5 migration
- [x] SecurityConfig: added `PATCH` to CORS allowed methods

### Out of scope for Phase 3a (N/A on web or deferred)
- Barcode scanner (camera API too complex for PWA, deprioritised)
- Health Connect steps/calories (Android-only hardware integration)
- Push notifications (iOS PWA limitations until 16.4+; deferred to Phase 4)
- Home-screen widget (Android-only)
- Font scale + on-device AI (Phase 5)

---

## Phase 3b — On-Demand Backup & Restore
> **Goal:** Users can back up all their data to Google Drive (if they have a Google account) or export/import a local JSON file (universal fallback). Completely free — backup files live in the user's own storage.  
> **Depends on:** Phase 1 sync pull response (already the backup format), Phase 3 webapp  
> **Design principle:** Two paths — Drive is the premium seamless path; local file works for everyone regardless of Google account.

| GH Issue | Task | Platform | Status |
|---|---|---|---|
| [#105](https://github.com/Pulin412/mealplan-plus/issues/105) | Google Drive backup — lazy OAuth (on first use), upload JSON to appDataFolder, list + restore | Android + Webapp | ✅ Done |
| [#106](https://github.com/Pulin412/mealplan-plus/issues/106) | Local file backup — Android share sheet export + file picker import; Webapp file upload + parse + sync push | Android + Webapp | ✅ Done |
| [#107](https://github.com/Pulin412/mealplan-plus/issues/107) | Backup & Restore UI — unified screen showing both paths; Drive backup list with date; graceful fallback when no Google account | Android + Webapp | ✅ Done |

### Phase 3b Checklist

**#105 — Google Drive backup**
- [x] Android: `GoogleSignIn` with `DRIVE_APPDATA` scope (lazy — requested only when user taps "Backup to Drive")
- [x] Android: Upload `mealplan_backup_<date>.json.gz` to `appDataFolder`; download + decompress + parse on restore → Room upsert
- [x] Webapp: Google Identity Services OAuth for `drive.appdata` scope (separate from Firebase Auth)
- [x] Webapp: Upload same JSON to `appDataFolder`; download on restore → `POST /api/v1/sync/push`
- [x] Both: Drive token cached; "Connect Google account" prompt shown when not connected

**#106 — Local file backup (universal fallback)**
- [x] Android: "Export" → serialize all Room data → GZIP compress → Android share sheet (Files, email, Dropbox, iCloud, etc.)
- [x] Android: "Import" → file picker → GZIP decompress → parse → upsert into Room
- [x] Webapp: "Export" wired to Backup & Restore screen
- [x] Webapp: "Import" → file upload → parse → `POST /api/v1/sync/push` → reload

**#107 — Backup & Restore UI**
- [x] Android: "Backup & Restore" screen under Settings — Drive section + Local file section
- [x] Webapp: Backup & Restore section in Settings replacing standalone export button
- [x] Both: Drive backup list shows filename + date + size; restore button; delete button

### What was actually delivered
- `DriveHelper.kt` — thin OkHttp wrapper around Drive REST API v3 (list, upload bytes, download bytes, delete)
- `LocalBackupSnapshot` — typed flat snapshot covering all 20 Room tables (foods, meals, diets, plans, planned slots, food logs, health metrics, groceries, exercises, workout templates, workout sessions, planned workouts)
- `BackupRepository` — builds and restores snapshot entirely from/to Room; zero network calls
- GZIP compression before upload / decompression on restore (~85% size reduction)
- `BackupRestoreViewModel` — orchestrates both paths; falls back gracefully when Firebase auth state is stale
- Firebase UID now persisted in `AuthPreferences` at every login so backup never fails on stale sessions
- Human-readable Drive error messages (403 → "enable Drive API in Cloud Console")
- Success toast shows record count: "47 meals, 3 diets, 128 log days, 234 metrics, 67 workouts, 5 lists"
- Branch: `feature/phase3b-backup-restore` (pending merge to main)

### Key Design Notes
- Backup format: `LocalBackupSnapshot` (Kotlin data class) — typed, versioned (`version = 2`), GZIP-compressed JSON
- Old uncompressed backups still restore (auto-detected by trying GZIP, falling back to plain JSON)
- `appDataFolder` scope: file is hidden from Drive UI but counts against user's free 15 GB
- Restore strategy: REPLACE on conflict — safe to run multiple times
- Firebase UID stored in DataStore at login → backup works even when Firebase auth state hasn't refreshed

---

## Phase 4 — AI on Web (Spring AI + RAG)
> **Goal:** Dietary chatbot on the web app, powered by user's actual data via RAG.  
> **Depends on:** Phase 1 (data in Postgres + pgvector enabled), Phase 3 (web app exists)

| GH Issue | Task | Status |
|---|---|---|
| [#95](https://github.com/Pulin412/mealplan-plus/issues/95) | Backend — Spring AI integration + PgVector RAG pipeline | ⬜ Open |
| [#96](https://github.com/Pulin412/mealplan-plus/issues/96) | Web App — AI chatbot UI | ⬜ Open |

### Phase 4 Checklist

**Backend (AI core)**
- [ ] #95 — Add `spring-ai-openai-spring-boot-starter` + `spring-ai-pgvector-store-spring-boot-starter`; `EmbeddingService` (embed on every diet/meal/log write → `entity_embeddings`); `DietContextAdvisor` (RAG context assembly); `POST /api/v1/ai/chat` endpoint; system prompt for IBD dietary role; streaming support (optional v1); API key secrets in Cloud Run; integration test

**Web chatbot UI**
- [ ] #96 — Chat panel (floating button or `/chat` page); streaming response rendering; starter prompts; disclaimer ("not medical advice"); session-only chat history

### Key Design Notes
- RAG flow: embed question → `PgVectorStore` similarity search → inject relevant diet/log/health records → LLM → response
- System prompt must frame the AI as a dietary assistant for IBD patients, not a general chatbot
- Provider-agnostic: `ChatClient` interface means OpenAI → Gemini is a one-line config change
- pgvector was enabled in Phase 1 — by Phase 4 there will be months of embedded user data ready

---

## Phase 5 — AI on Android
> **Goal:** Bring AI to Android with cloud AI for conversations + on-device AI for offline suggestions.  
> **Depends on:** Phase 4 backend AI endpoint live

| GH Issue | Task | Status |
|---|---|---|
| [#97](https://github.com/Pulin412/mealplan-plus/issues/97) | Android — AI dietary assistant (Gemini Nano on-device + cloud hybrid) | ⬜ Open |

### Phase 5 Checklist
- [ ] #97 — Add Google AI Edge SDK; `OnDeviceAIService` (Gemini Nano, offline, quick suggestions); inline suggestions on Daily Log screen; `CloudAIService` calling `/api/v1/ai/chat`; `AIChatScreen` + `AIChatViewModel`; fallback logic (cloud when online → on-device when offline); unit tests for fallback

### Key Design Notes
- Same backend endpoint as Phase 4 web — no new backend work needed
- On-device scope: short suggestions only ("This meal has high sodium for remission diet")
- Cloud scope: full conversation with full user history context via RAG
- Fallback rule: `isOnline() && userPrefersCloud → cloud; else → on-device`

---

## Dependency Graph

```
Foundation (#81, #82, #98 UI redesign)
    └── Phase 1 (Sync API)
            ├── Phase 2 (Workout)          ← parallel with Phase 3 · done ✅
            ├── Phase 3 (Web App scaffold) ← done ✅ · screens, auth, design system
            │       └── Phase 3a (Web App parity) ← done ✅ · #99–#104
            │               └── Phase 3b (Backup & Restore) ← on-demand Drive + local file · #105–#107
            │                       └── Phase 4 (AI Web) ← needs web UI + pgvector data
            │                               └── Phase 5 (AI Android) ← same backend endpoint
            └── (pgvector enabled here)
```

**Critical path:** Foundation → Phase 1 → Phase 3 → Phase 3a → Phase 3b → Phase 4 → Phase 5

### Phase order summary (current state)

| Order | Phase | Status | Notes |
|---|---|---|---|
| 0 | **Foundation** | ✅ Done | Android redesign, DB v35, all 19 screens |
| 1 | **Phase 1** · Backend Sync | ✅ Done | Spring Boot API, delta sync, Cloud Run |
| 2a | **Phase 2** · Workouts Android | ✅ Done | All workout screens, templates, logging, edit mode (#89, #90) |
| 2b | **Phase 2** · Workout Backend sync | ⬜ Open | #91: extend sync push/pull for workouts |
| 2c | **Phase 3** · Web App scaffold | ✅ Done | Next.js, Firebase Auth, all 10 screens |
| 2d | **Phase 3a** · Web Parity | ✅ Done | #99–#104: all 6 issues complete |
| 2e | **Phase 3b** · Backup & Restore | ✅ Done | #105–#107: Drive + local file, Android + Webapp; GZIP; all 20 tables |
| 3 | **Phase 4** · AI Web | ⬜ Open | Needs Phase 3b done + pgvector data accumulating |
| 4 | **Phase 5** · AI Android | ⬜ Open | Needs Phase 4 backend endpoint |

---

## Tech Stack Reference

| Layer | Technology | Notes |
|---|---|---|
| Android | Kotlin, Jetpack Compose, Room v29, Hilt, Retrofit, WorkManager | Self-contained, offline-first |
| Backend | Spring Boot 3.2.5, Kotlin, Spring Data JPA, Flyway, Spring AI | Deployed on Google Cloud Run |
| Database | Neon.tech Postgres + pgvector extension | pgvector enabled in Phase 1 |
| Auth | Firebase Auth (Google + email) | Same project for Android + web |
| Web App | Next.js 14, TypeScript, Tailwind CSS, shadcn/ui, Firebase Auth | PWA for iPhone support |
| AI Backend | Spring AI (provider-agnostic), PgVectorStore | LLM provider swappable via config |
| AI Android | Google AI Edge (Gemini Nano) + cloud fallback | Hybrid offline/online |
| CI/CD | GitHub Actions | Android: build+test; Backend: build+deploy; Web: lint+build |

---

## Progress Tracking

Update the status column in each phase table as work progresses:

| Symbol | Meaning |
|---|---|
| ⬜ Open | Not started |
| 🔄 In Progress | Active work on a branch |
| ✅ Done | Merged to main |
| 🚫 Blocked | Waiting on a dependency |
