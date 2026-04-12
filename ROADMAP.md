# MealPlan+ — Product Roadmap

> Last updated: April 2026  
> Track progress via [GitHub Issues](https://github.com/Pulin412/mealplan-plus/issues)

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

## Foundation — Android Stability
> **Goal:** Clean, stable Android app as the v1.0 baseline before sync work begins.

| GH Issue | Task | Status |
|---|---|---|
| [#81](https://github.com/Pulin412/mealplan-plus/issues/81) | Remove `shared/` KMP module dependency from Android | ⬜ Open |
| [#82](https://github.com/Pulin412/mealplan-plus/issues/82) | Stabilise Room schema — document v29 as clean baseline | ⬜ Open |
| [#83](https://github.com/Pulin412/mealplan-plus/issues/83) | Merge `feature/quick-add-fab` → `main`, tag as v1.0 | ⬜ Open |

### Foundation Checklist
- [ ] #81 — Audit `shared/` usage in Android; copy anything needed; remove `:shared` dependency
- [ ] #82 — Verify schema files up to v29 exist; add `DB_SCHEMA.md`; write idempotency test
- [ ] #83 — Verify no duplicate data on device; merge branch; tag `v1.0`; push tag

### What was fixed to get here
- DB v27→v28: deduplicated diets + meals (seeder ran multiple times due to missing guard)
- DB v28→v29: deduplicated food_items (compound-PK tables handled carefully)
- `UserDataSeeder`: added `getDietCount() > 0` idempotency guard
- `JsonDataImporter`: fixed `getExistingDietNames()` — was always returning empty set
- Quick-Add FAB: central `+` button with slide-up sheet (Add Food, New Meal, New Diet, Log Today)
- Bottom nav: visible on all authenticated screens; Settings tab added for symmetry
- Meal filter chips (Breakfast/Lunch/Dinner): now actually filter meals by diet slot assignment

---

## Phase 1 — Backend Sync API
> **Goal:** Backend becomes the source of truth. Android syncs data. Backup/restore is a free side-effect.  
> **Unblocks:** Phase 3 (web app needs API), Phase 4 (AI needs data in Postgres)

| GH Issue | Task | Status |
|---|---|---|
| [#84](https://github.com/Pulin412/mealplan-plus/issues/84) | Design OpenAPI spec for all core domain endpoints | ⬜ Open |
| [#85](https://github.com/Pulin412/mealplan-plus/issues/85) | Backend — implement all domain JPA entities + CRUD endpoints | ⬜ Open |
| [#86](https://github.com/Pulin412/mealplan-plus/issues/86) | Backend — implement delta sync push/pull + enable pgvector | ⬜ Open |
| [#87](https://github.com/Pulin412/mealplan-plus/issues/87) | Android — SyncWorker calling backend push/pull | ⬜ Open |
| [#88](https://github.com/Pulin412/mealplan-plus/issues/88) | Backend — Cloud Run deployment + CI/CD pipeline | ⬜ Open |

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

| GH Issue | Task | Status |
|---|---|---|
| [#89](https://github.com/Pulin412/mealplan-plus/issues/89) | Android — Workout domain model and Room entities (v30 migration) | ⬜ Open |
| [#90](https://github.com/Pulin412/mealplan-plus/issues/90) | Android — Workout screens (Log, History, Exercise catalogue) | ⬜ Open |
| [#91](https://github.com/Pulin412/mealplan-plus/issues/91) | Backend — Workout JPA entities + sync extension | ⬜ Open |

### Phase 2 Checklist

**Android**
- [ ] #89 — `Exercise`, `WorkoutSession`, `WorkoutSet` entities; DAOs; Room migration v30; `WorkoutRepository`; `exercises.json` asset + `ExerciseSeeder` with version guard
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
Foundation
    └── Phase 1 (Sync API)
            ├── Phase 2 (Workout)        ← same sync protocol, new domain
            ├── Phase 3 (Web App)        ← needs backend API + OpenAPI spec
            │       └── Phase 4 (AI Web) ← needs web UI + pgvector data
            │               └── Phase 5 (AI Android) ← same backend endpoint
            └── (pgvector enabled here)
```

**Critical path:** Foundation → Phase 1 → Phase 3 → Phase 4 → Phase 5  
Phase 2 (Workout) can run in parallel with Phase 3 once Phase 1 is done.

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
