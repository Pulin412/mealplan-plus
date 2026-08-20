# EatMyPlan

Offline-first meal planning, nutrition logging, and workout tracking. One backend, two
independent clients — a native Android app and a Next.js PWA (the iPhone experience is the
PWA via Safari → Add to Home Screen).

> **Brand vs. identifiers.** The product is **EatMyPlan** (rebranded from *MealPlan+*, Aug 2026).
> Internal identifiers are intentionally unchanged: the git repo is `mealplan-plus`, the Android/
> backend package is `com.mealplanplus`, the GCP/Firebase project is `mealplan-plus`, and the
> Cloud Run service is `mealplan-api`. Renaming those would mean a new Play listing + Auth-user
> migration for zero user benefit, so only user-visible strings were changed.

## Live

| Surface | URL |
|---|---|
| Web app | https://eatmyplan.com (also `www.eatmyplan.com`) |
| API | https://api.eatmyplan.com — custom domain; the Cloud Run default `https://mealplan-api-rfo22lhanq-ez.a.run.app` still works in parallel |
| Health | https://api.eatmyplan.com/actuator/health |
| MCP connector | https://api.eatmyplan.com/mcp (Streamable HTTP; OAuth 2.1 via Stytch) — connect your own Claude |
| API docs (Swagger) | `…/swagger-ui.html` — Basic auth on prod |
| Android | Sideloadable APK on [GitHub Releases](https://github.com/Pulin412/mealplan-plus/releases) (`android-v*`) |

All `eatmyplan.com` hosts are free: Vercel for the web app, a free Cloud Run **domain mapping** for
`api.` (no load balancer), and a Stytch **custom auth domain** for `login.`. See
[docs/RUNBOOK.md](docs/RUNBOOK.md#domains--external-mcp-connector) for the full domain map.

## Modules

| Dir | Stack | Role |
|---|---|---|
| `backend/` | Spring Boot 3, Kotlin, Postgres (Neon) + pgvector, Firebase JWT | REST API — source of truth, deployed on Cloud Run |
| `android/` | Kotlin, Compose, Room, Hilt | Native client, deployed as a sideloadable APK |
| `webapp/` | Next.js 14, TypeScript, Tailwind | PWA client, deployed on Vercel |

Clients share **no code** — only the API contract `docs/openapi.yaml`, from which each
generates its own typed client. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Connect your own Claude (MCP)

EatMyPlan exposes its tools over the [Model Context Protocol](https://modelcontextprotocol.io) so a
user can connect **their own Claude** and read/write their diets, meals, plans, and logs — the LLM
cost sits on the user's own subscription (zero-billing-friendly).

- **Connector URL:** `https://api.eatmyplan.com/mcp` (add it in claude.ai → Settings → Connectors)
- **Auth:** OAuth 2.1 (PKCE + DCR) via **Stytch Connected Apps**, bridged to the app's Firebase
  session — `login.eatmyplan.com` is the authorization server, `eatmyplan.com/authorize` is the
  consent page. The access token carries `firebase_uid`, so MCP tools resolve the same user as the
  REST API.
- **Gated** by the DB-backed `mcp_server` feature flag (admin-only; see `AdminController`).
- Full design + ops: [docs/agents/PLAN-mcp-server.md](docs/agents/PLAN-mcp-server.md) and
  [docs/RUNBOOK.md](docs/RUNBOOK.md#domains--external-mcp-connector).

## Quickstart

```bash
# Backend (H2 in-memory dev profile on :8080)
cd backend && ./gradlew bootRun

# Webapp (:3000) — point it at the local backend
cd webapp && npm install && NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 npm run dev

# Android — build + install debug on a running emulator/device
./gradlew :android:installDebug
# ...or against a physical device on the LAN:
./gradlew :android:installDebug -PapiBaseUrl=http://<your-mac-LAN-ip>:8080

# Or one command: start backend (H2) + seed a full demo dataset
./scripts/local-up.sh   # sign in with dev@mealplan.test / mealplan123
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
- [docs/RUNBOOK.md](docs/RUNBOOK.md) — ops: rollback, domain map, MCP connector, monitoring setup.
- `docs/openapi.yaml` — the API contract (build input; keep in sync with every API change).
- `docs/agents/` — the AI-agent + MCP-connector design docs.
