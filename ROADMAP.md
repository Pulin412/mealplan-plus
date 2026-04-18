# MealPlan+ — Product Roadmap

> Last updated: April 15, 2026  
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
| [#89](https://github.com/Pulin412/mealplan-plus/issues/89) | Android — Workout domain model and Room entities (v33 migration) | ⬜ Open |
| [#90](https://github.com/Pulin412/mealplan-plus/issues/90) | Android — Workout screens (Log, History, Exercise catalogue) | ⬜ Open |
| [#91](https://github.com/Pulin412/mealplan-plus/issues/91) | Backend — Workout JPA entities + sync extension | ⬜ Open |

### Phase 2 Checklist

**Android**
- [ ] #89 — `Exercise`, `WorkoutSession`, `WorkoutSet` entities; DAOs; Room migration v32; `WorkoutRepository`; `exercises.json` asset + `ExerciseSeeder` with version guard
- [ ] #90 — `WorkoutLogScreen`, `WorkoutHistoryScreen`, `ExerciseCatalogueScreen`; add "Log Workout" to Quick Add FAB; register routes in NavHost; ViewModel unit tests

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
| [#92](https://github.com/Pulin412/mealplan-plus/issues/92) | Web App — Project scaffold (Next.js + TypeScript + PWA + Firebase Auth) | ⬜ Open |
| [#93](https://github.com/Pulin412/mealplan-plus/issues/93) | Web App — Core screens (Dashboard, Diets, Meals, Daily Log, Calendar) | ⬜ Open |
| [#94](https://github.com/Pulin412/mealplan-plus/issues/94) | Web App — Health Metrics, Grocery, Settings + data export | ⬜ Open |

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
            ├── Phase 2 (Workout)        ← parallel with Phase 3 · screens spec'd in design-future.html
            ├── Phase 3 (Web App)        ← same design language as Android (#98) · spec in design-future.html
            │       └── Phase 4 (AI Web) ← needs web UI + pgvector data
            │               └── Phase 5 (AI Android) ← same backend endpoint · AI overlay already designed
            └── (pgvector enabled here)
```

**Critical path:** Foundation → Phase 1 → Phase 3 → Phase 4 → Phase 5  
Phase 2 (Workout) can run in parallel with Phase 3 once Phase 1 is done.

### Phase order summary (with redesign)

| Order | Phase | Key dependency | Design work |
|---|---|---|---|
| 0 | **Foundation** | — | #98: implement `design-future.html` on Android |
| 1 | **Phase 1** · Backend Sync | Foundation stable | Backup/Sync/Restore screens (already designed) |
| 2a | **Phase 2** · Workouts | Phase 1 API live | 3 workout screens already spec'd |
| 2b | **Phase 3** · Web App | Phase 1 API live | Re-use same design tokens from #98 |
| 3 | **Phase 4** · AI Web | Phase 1 + Phase 3 | AI overlay already designed |
| 4 | **Phase 5** · AI Android | Phase 4 backend live | AI overlay already in Android design |

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
