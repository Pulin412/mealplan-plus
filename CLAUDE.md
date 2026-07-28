# MealPlan+ — Root (monorepo)

Offline-first meal planning & food logging. Log meals by slot (BREAKFAST/LUNCH/DINNER),
track health metrics, browse/create diets, get shopping lists, smart "you haven't logged yet" notifications.

**Two independent clients, one backend.** `android/`, `webapp/`, `backend/`. No shared client code.
Module-specific rules live in each module's own `CLAUDE.md` — this file holds only what applies to every module.
The old `android/` + `webapp/` (pre-redesign) apps were removed — their reference docs live in `docs/legacy/`.

> Deep docs (do NOT paste here, link only): `docs/V2_PLAN.md`, `docs/V2_ARCHITECTURE.md`, `docs/DEPLOYMENT.md`, `docs/BRANCHING.md`, `docs/openapi.yaml`. Old-app docs archived under `docs/legacy/`.

## Commands
| Module | Build / Test |
|--------|--------------|
| Android | `./gradlew :android:testDebugUnitTest` (unit) · instrumented tests in `android/src/androidTest/` |
| Backend | `cd backend && ./gradlew build test` |
| Webapp  | `cd webapp && npm run build && npm run lint` · regen API types: `npm run gen:api` |

## Module map
| Module | Role |
|--------|------|
| `android/` | Kotlin, Compose, Room, Hilt — self-contained production app (redesign) |
| `backend/` | Spring Boot 3.2.5 REST API; Firebase JWT auth, Neon.tech Postgres + pgvector — deployed on Cloud Run |
| `webapp/`  | Next.js 14 + TypeScript PWA — deployed on Vercel |
| `docs/`    | V2 plan/architecture, deployment, branching, OpenAPI (`docs/legacy/` = old-app archive) |
| `scripts/` | One-off setup scripts |

Live: API `https://mealplan-api-rfo22lhanq-ez.a.run.app` · Web `https://mealplan-plus.vercel.app` · health `…/actuator/health`.
Backend is the source of truth and shared layer for both clients.

## Hard rules — never break (cross-cutting)
1. **Zero-billing guardrail.** IMPORTANT: never import Firestore, Cloud Functions, Firebase Storage, or Realtime DB. Firebase is Auth + Crashlytics + Remote Config + Analytics, free-tier only. A CI task fails the build if banned SDKs appear.
2. **`shared/` KMP module is dead.** Do not re-add it; `:shared` and the old `:android` are gone from `settings.gradle.kts`.
3. **iOS is gone.** The iPhone client is the Next.js PWA (`webapp/`) on Safari. Do not re-add an `ios/` module.
4. **YOU MUST confirm with the user before any commit or push.** No exceptions.
5. Small, focused commits — one logical change each. Never a 40-file monster commit.
6. `backend/**` is in the path filter of ALL client CI workflows — any API change rebuilds Android + Webapp. Keep the API contract (`docs/openapi.yaml`) in sync.

## Git workflow — `develop` is the primary branch
IMPORTANT: `develop` is the base/integration branch — treat it as primary, not `main`.
1. **ALWAYS cut a `feature/*` (or `fix/*`) branch off `develop`.** Never commit or push directly to `develop` or `main` — they take PRs only.
   `git checkout develop && git pull && git checkout -b feature/<issue-or-topic>`
2. **Every change lands via a PR into `develop`** (squash-and-merge). No exceptions — even a one-line fix gets a branch + PR.
3. Branch off `develop`, never off `main`. Name after the issue/phase: `feature/…`, `fix/…`.
4. `develop → main` PRs are release-only and deploy on merge — open one only when the user explicitly asks to release.
5. Full model: `docs/BRANCHING.md`.

## Human approval required (stop and ask)
- Any commit, push, branch, or PR.
- Room schema migrations (see `android/CLAUDE.md`) and Flyway migrations (`backend/`).
- Changing the deployed API contract, auth, or CORS.
- Anything touching billing posture (Firebase/GCP services, Cloud Run config).

## CI
⚠️ **Pipeline pending review** — the workflows in `.github/` (`android.yml`, `webapp.yml`, `webapp-prune.yml`) were written for the old module layout and still reference the now-renamed paths; they need rewiring to the current `android/` + `webapp/` (formerly `android-v2`/`webapp-v2`). `backend.yml`, `backend-deploy.yml` (Cloud Run on merge to `main`) and Vercel deploy still apply. Keep `backend/**` in every client workflow's path filter so API changes rebuild the clients.

## Workflow
- Make minimal changes; do not refactor unrelated code.
- TDD for new features: write the failing test first, then implement.
- Run the relevant module's build + tests before marking a task done.
- When unsure between two approaches, present both and let the user choose.

<!-- TEMPORARY: app + PWA are being redesigned; a second repo may be added later.
     When that happens, move machine-wide personal rules to ~/.claude/CLAUDE.md (already set up)
     and keep each new repo's root file thin like this one. -->
