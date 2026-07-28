# Architecture

## Three tiers, one contract

```
                 docs/openapi.yaml         ← the ONE shared contract
                        │ generate
       ┌────────────────┼─────────────────┐
   backend/          android/           webapp/
   Spring (brain)    Retrofit + Room    TS + React
   controllers       repo → VM → UI     service → hooks → UI
        │
   Cloud Run ──▶ Postgres (Neon) + pgvector
```

There is **no shared client code**. The only thing shared across tiers is the API
contract, `docs/openapi.yaml`. Each tier **generates** its own typed HTTP client + DTOs
from it — nobody hand-writes a DTO or an endpoint path.

Regenerate after editing the contract (contract changes need human approval):

| Tier | Command |
|---|---|
| backend | `./gradlew :backend:openApiGenerate` (runs in `build`) |
| android | `./gradlew :android:openApiGenerate` (runs in `build`) |
| webapp | `cd webapp && npm run gen:api` |

**The rule:** business logic lives on the server and is consumed over the wire; a rule
only lives in a client if it *must* run offline. The backend is the de-duplication engine.

## Data & sync

- **Backend owns persistence** (Neon Postgres, Flyway migrations, forward-only) and all
  server-only compute (pgvector similarity search, the shared food database, future AI agent).
- **Delta sync API:** `POST /api/v1/sync/push` + `GET /api/v1/sync/pull?since=<ISO>`,
  last-write-wins on `updatedAt`, soft deletes via `tombstones[]`.
- **Android** caches Foods/Meals/Diets in Room (offline-first; a background sync worker is
  the target design for the full write path). **Webapp** is online-first — reads Neon live,
  so it sees Android's changes as soon as they land on the server.
- Records use a **client-generated UUID** as both the Room primary key and the server
  identity → sync is an idempotent upsert-by-UUID, so a record created offline never
  duplicates when pulled back.

## Auth

Firebase issues a JWT → client sends `Authorization: Bearer <token>` → backend validates
via Firebase's JWKS endpoint → data is scoped to `firebaseUid`. No server-side session.

## Zero-billing guardrail (hard rule)

Every Google/Firebase service in use is **free-tier only**, and the build fails if a
billable SDK is imported.

- **Allowed Firebase:** Auth, Crashlytics, Remote Config, Analytics.
- **Banned (never import):** Firestore, Cloud Functions, Firebase Storage, Realtime DB.
  A CI task (`verifyNoBillableFirebaseFeatures`) enforces this on Android.
- Cloud Run scales to zero; Neon free tier; Vercel Hobby; Secret Manager (few secrets).
  Expected cost at personal scale: **$0/month**.

## Design system

Two component libraries (Compose + React) implement **one token set** (colours, DM Mono for
all numerals, ≥44px tap targets, WCAG 2.1 AA). Screens compose from the library rather than
hand-rolling styling. The design source lives in `design_v2/` (local, gitignored).

---

## Deployment

### Branch → deploy flow

```
feature/*  ──PR──▶  develop  ──PR──▶  main  ──merge──▶  deploy 🚀
           CI gate           CI gate
```

- **`develop`** is the integration branch; **`main`** mirrors production. Both take PRs
  only — never commit directly. Always cut `feature/*` (or `fix/*`) off `develop`.
- PRs into `develop` and `main` run the CI gates (Android build, Backend build+test,
  Webapp type-check+lint+build), path-filtered — `backend/**` is in every client filter,
  so an API change rebuilds the clients.
- **Merging `develop → main` deploys**: backend → Cloud Run, webapp → Vercel.

### Backend — Cloud Run (`backend-deploy.yml`, on merge to `main` when `backend/**` changed)

Build+test → Docker image → push to Artifact Registry → `gcloud run deploy` → health check
→ prune old images. Runs under `SPRING_PROFILES_ACTIVE=prod` (`application-prod.yml`):
hardened logging, Hikari pool, `ddl-auto=validate`, Flyway `clean` disabled, CORS locked to
the prod webapp origin, **Swagger behind Basic auth**.

| Property | Value |
|---|---|
| Service / region | `mealplan-api` · `europe-west4` |
| GCP project | `mealplan-plus` |
| Deployer SA | `mealplan-deployer@mealplan-plus.iam.gserviceaccount.com` |
| Image | `europe-west4-docker.pkg.dev/mealplan-plus/mealplan/mealplan-api` |

**GitHub secrets:** `GCP_PROJECT_ID`, `GCP_SA_KEY`, `FIREBASE_PROJECT_ID`.
**Secret Manager:** `mealplan-db-{url,user,password}`, `mealplan-swagger-{user,password}`.

### Database — Neon Postgres

Flyway migrations in `backend/src/main/resources/db/migration/` (`V1__baseline.sql` is a
squashed baseline; migrations are forward-only and need approval). pgvector is created by
the baseline. To rebuild from empty (clean slate): back up first (Neon branch/PITR), then
`DROP SCHEMA public CASCADE; CREATE SCHEMA public;` + re-grant — Flyway rebuilds on next boot.

### Webapp — Vercel

Auto-deploys on `main` (Root Directory `webapp/`). Env: `NEXT_PUBLIC_FIREBASE_*`,
`NEXT_PUBLIC_API_BASE_URL` = the prod API. Firebase authorized domains must include the
Vercel domain.

### Android — manual APK

`android-release.yml` is `workflow_dispatch` only (inputs: `versionName`, `versionCode`).
It builds a **debug-signed** APK pointed at prod (`applicationId com.mealplanplus`, sideload
variant `com.mealplanplus.dev`) and publishes it to a GitHub Release. No Play account yet —
signed-AAB + Play upload is a placeholder in the workflow.

### Rollback

- **Backend:** `gcloud run services update-traffic mealplan-api --region=europe-west4 --to-revisions=<rev>=100`.
- **Webapp:** Vercel → Deployments → promote a previous deployment.
- **Database:** forward-only; restore from a Neon branch/PITR.
