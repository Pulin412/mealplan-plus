# MealPlan+

Offline-first meal planning, nutrition logging, and workout tracking app.  
Android app + Spring Boot backend + Next.js PWA (iPhone via Add to Home Screen).

> **Roadmap:** See [ROADMAP.md](ROADMAP.md) for the full phase plan and GitHub issue tracking.  
> **Design spec:** Open `design-future.html` in a browser — interactive mockups for all screens.

---

## Platform Support

| Platform | Stack | Status |
|---|---|---|
| Android | Kotlin, Jetpack Compose, Room, Hilt | ✅ Production |
| iPhone / iPad | Next.js PWA (Safari → Add to Home Screen) | 🚧 Phase 3c — deploying |
| Web (desktop / Chrome) | Next.js 14, Tailwind, shadcn/ui | ✅ Built, deploying |
| Backend | Spring Boot 3.2.5, Kotlin, JPA, Flyway | ✅ Built, deploying |

---

## Features

- **Food Database** — Manual entry, barcode scan, USDA / OpenFoodFacts search
- **Meal Templates** — Reusable meals with quantities and units
- **Diet Templates** — Full-day plans across Breakfast, Lunch, Dinner slots
- **Calendar Planning** — Assign diets to dates, plan the week ahead
- **Daily Logging** — Log what you ate, compare against the plan, streak tracking
- **Workout Logging** — Exercise catalogue, workout templates, session history, sets/reps/weight
- **Health Metrics** — Weight, blood glucose, HbA1c, custom metrics with trend charts
- **Grocery Lists** — Auto-generated from planned diets for any date range
- **Backup & Restore** — Google Drive (appDataFolder) + local file export/import, GZIP compressed
- **Health Connect** — Steps, calories burned, weight from Garmin/Fitbit/Samsung (Android)
- **Home Screen Widgets** — Today's plan, diet summary, mini calendar (Android, Glance)
- **AI Assistant** — Dietary chatbot via Spring AI + RAG (Phase 4, upcoming)

---

## Architecture

```
Android (Room) ←──── SyncWorker ────→ Spring Boot API ←──── Next.js PWA
                                            │
                                       Neon.tech Postgres
                                       + pgvector (AI Phase 4)
```

- **Android** — fully offline-first; Room is source of truth; syncs to backend via WorkManager
- **Backend** — Firebase JWT auth; delta sync push/pull with last-write-wins conflict resolution
- **Webapp** — same Firebase project; same backend API; works as PWA on iPhone Safari
- **No KMP / no shared module** — Android and web are independent codebases; backend is the shared layer

---

## Tech Stack

| Layer | Technology |
|---|---|
| Android | Kotlin 1.9, Jetpack Compose, Room v29, Hilt, Retrofit, WorkManager, Glance |
| Backend | Spring Boot 3.2.5, Kotlin, Spring Data JPA, Flyway, Spring AI (Phase 4) |
| Database | Neon.tech Postgres 16 + pgvector extension |
| Auth | Firebase Authentication (Google + email/password) |
| Webapp | Next.js 14, TypeScript, Tailwind CSS, shadcn/ui, serwist (PWA) |
| CI/CD | GitHub Actions — build+test on PRs; deploy to Cloud Run on develop→main merge |
| Hosting | Google Cloud Run (backend) · Vercel (webapp) |

---

## Repository Layout

```
mealplan-plus/
├── android/          ← Kotlin, Compose, Room, Hilt — fully self-contained
├── backend/          ← Spring Boot 3 — REST API, source of truth
├── webapp/           ← Next.js 14 PWA — web + iPhone via Add to Home Screen
├── docs/             ← Architecture docs, DB schema, branching strategy
├── scripts/          ← One-time setup scripts (GCP, etc.)
├── shared/           ← DISCONNECTED — do not add code here
├── ios/              ← SUPERSEDED by PWA — no new work
├── backup/           ← Seed data (one-time import, historical)
├── CLAUDE.md         ← AI assistant context (always keep up to date)
└── ROADMAP.md        ← Phase plan and GitHub issue tracking
```

---

## Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog or later |
| JDK | 17 (Android) · 21 (Backend) |
| Android SDK | API 26–35 |
| Node.js | 18+ |
| Firebase project | Required (see below) |

### 1. Clone

```bash
git clone https://github.com/Pulin412/mealplan-plus.git
cd mealplan-plus
```

### 2. Firebase setup

The app uses Firebase Auth only (free tier — no Firestore, no Storage, no Cloud Functions).

1. Go to [Firebase Console](https://console.firebase.google.com) → project `mealplan-plus`
2. Add an **Android app** → package name `com.mealplanplus` → download `google-services.json` → place in `android/`
3. Add a **Web app** → copy the config into `webapp/.env.local` (see `webapp/.env.local.example`)
4. Enable **Authentication → Sign-in methods → Email/Password** and **Google**

Full details: [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

### 3. Android

```bash
# Build debug APK
./gradlew :android:assembleDebug

# Run unit tests
./gradlew :android:testDebugUnitTest

# Install on connected device
adb install android/build/outputs/apk/debug/android-debug.apk
```

### 4. Backend (local)

```bash
cd backend
./gradlew bootRun
# Runs on http://localhost:8080 with H2 in-memory DB — no Postgres needed locally
```

### 5. Webapp (local)

```bash
cd webapp
cp .env.local.example .env.local   # fill in Firebase config
npm install
npm run dev
# Runs on http://localhost:3000
```

---

## Branching Strategy

See [docs/BRANCHING.md](docs/BRANCHING.md) for full details.

```
feature/* → PR → develop   (CI: build + test gate)
develop   → PR → main      (CI: build + test gate → deploy on merge)
```

| Branch | Purpose |
|---|---|
| `main` | Production — only updated via PR merge from `develop` |
| `develop` | Integration — all feature branches merge here first |
| `feature/*` | Individual features — one branch per GitHub issue |

Never push directly to `main`. Every production change goes through a PR.

---

## CI / CD

| Workflow | Triggers | What it does |
|---|---|---|
| `ci.yml` | Push to `main`/`develop`; any PR | Build + unit tests for changed modules (path-filtered) |
| `backend-deploy.yml` | PR merged `develop` → `main` | JAR → Docker → Artifact Registry → Cloud Run → health check |

**Deployed services:**
- **Backend:** Google Cloud Run · `mealplan-api` · `europe-west4` (Netherlands)
- **Webapp:** Vercel · auto-deploy on push to `main`

---

## Key Docs

| File | Purpose |
|---|---|
| `CLAUDE.md` | Full project context for AI assistant |
| `ROADMAP.md` | Phase plan, GitHub issues, current status |
| `docs/DATABASE_SCHEMA.md` | Full Room schema, ER diagram, migration history |
| `docs/BRANCHING.md` | Branching strategy and PR workflow |
| `design-future.html` | Interactive design mockups — open in browser |
| `docs/openapi.yaml` | Backend OpenAPI spec — source of truth for API types |

---

## Files That Must Never Be Committed

- `android/google-services.json` — Firebase Android credentials
- `webapp/.env.local` — Firebase web config + API keys
- `local.properties` — local SDK paths

All listed in `.gitignore`.

---

## License

Private repository. All rights reserved.
