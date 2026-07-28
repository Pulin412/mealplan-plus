# backend/ — MealPlan+ API

Spring Boot 3.2.5 · Kotlin · Neon.tech Postgres + pgvector · Firebase JWT (JWKS) auth · Flyway.
Source of truth for both clients. Deployed to Cloud Run (`europe-west4`):
`https://mealplan-api-rfo22lhanq-ez.a.run.app`.

## Commands
- Build + test: `./gradlew build test`
- Local stack: `docker-compose up` (from repo root)

## API surface
- 7 domain CRUD endpoints: User, Food, Meal, Diet, Grocery, HealthMetric, DailyLog.
- Delta sync: `POST /api/v1/sync/push` + `GET /api/v1/sync/pull?since=<ISO>`, last-write-wins (remote wins only when `remoteUpdatedAt > localUpdatedAt`).
- Soft deletes via **tombstones**; pull response includes `tombstones[]`.
- Contract is `docs/openapi.yaml` — clients generate from it. Keep it in sync with every endpoint change.

## Hard rules — never break
1. **Flyway migrations are forward-only and need human approval.** Add a new `V<n>__*.sql`; never edit an applied migration (V1–V6 exist). Migrations run on the docker/prod profile only.
2. **Auth is Firebase JWT via JWKS.** `SecurityConfig` secures all endpoints. CORS allows localhost + `*.vercel.app` only — changing auth or CORS needs approval.
3. **Any change here rebuilds all clients** (CI path filter). If you change a DTO or route, update `docs/openapi.yaml` and expect Android + Webapp CI to run.
4. No paid GCP/Firebase services (root zero-billing rule).

## Deploy
`backend-deploy.yml` deploys to Cloud Run on PR merge to `main` when `backend/**` changes. Rollback + credentials: `docs/ARCHITECTURE.md`.

## Out of scope / do not touch
- Applied Flyway migrations `V1`–`V6`.
- Cloud Run service config / secrets — managed outside the repo (see `docs/ARCHITECTURE.md`).
