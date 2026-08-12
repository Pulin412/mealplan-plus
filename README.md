# MealPlan+

Offline-first meal planning, nutrition logging, and workout tracking. One backend, two
independent clients — a native Android app and a Next.js PWA (the iPhone experience is the
PWA via Safari → Add to Home Screen).

## Live

| Surface | URL |
|---|---|
| API | https://mealplan-api-rfo22lhanq-ez.a.run.app |
| Health | https://mealplan-api-rfo22lhanq-ez.a.run.app/actuator/health |
| Web app | https://mealplan-plus.vercel.app |
| API docs (Swagger) | `…/swagger-ui.html` — Basic auth on prod |

## Modules

| Dir | Stack | Role |
|---|---|---|
| `backend/` | Spring Boot 3, Kotlin, Postgres (Neon) + pgvector, Firebase JWT | REST API — source of truth, deployed on Cloud Run |
| `android/` | Kotlin, Compose, Room, Hilt | Native client, deployed as a sideloadable APK |
| `webapp/` | Next.js 14, TypeScript, Tailwind | PWA client, deployed on Vercel |

Clients share **no code** — only the API contract `docs/openapi.yaml`, from which each
generates its own typed client. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Quickstart

```bash
# Backend (H2 in-memory dev profile on :8080)
cd backend && ./gradlew bootRun

# Webapp (:3000)
cd webapp && npm install && npm run dev

# Android — build + install debug on a running emulator/device
./gradlew :android:installDebug
```

## Build & test

| Module | Command |
|---|---|
| Backend | `cd backend && ./gradlew build test` |
| Android | `./gradlew :android:testDebugUnitTest` |
| Webapp | `cd webapp && npm run build && npm run lint` |
| Regen API types (webapp) | `cd webapp && npm run gen:api` |

## Docs

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — how the tiers fit, data/sync model, auth, deployment, zero-billing.
- [docs/FEATURES.md](docs/FEATURES.md) — what's built and what's planned.
- [docs/RUNBOOK.md](docs/RUNBOOK.md) — ops: rollback, where to look when it breaks, monitoring setup.
- `docs/openapi.yaml` — the API contract (build input; keep in sync with every API change).
- `docs/agents/` — planning for the future AI-agent feature.
