# MCP Connector — Tool Contract

The **external MCP connector server** exposes a small set of tools so a user can connect their **own**
AI agent (e.g. Claude) and read/write *their own* MealPlan+ data. It is a thin protocol adapter over the
existing core services (`DietService` / `DashboardService` / `UserService` / `FoodService` /
`LoggingService` / `MealService`), isolated in `backend/.../domain/mcp`.

> Not to be confused with `ADR-002-mcp-server-design.md` (the *internal* Spring-AI `@Tool` layer behind
> the in-app assistant). See `PLAN-mcp-server.md` for the full plan and phasing.

## Transport & connection

- **Protocol:** MCP over SSE (Spring AI 1.0.0 MCP server, WebMVC).
- **Endpoints** (under `/mcp/**`, runtime-gated by the `mcp_server` feature flag):
  - `GET /mcp/sse` — the SSE stream (this is the **connector URL**).
  - `POST /mcp/message?sessionId=…` — client→server messages (tool calls).
- **Connector URL:** `<API base>/mcp/sse` (e.g. `https://mealplan-api-rfo22lhanq-ez.a.run.app/mcp/sse`).
- **Auth:** `Authorization: Bearer <connector-token>` on every request. The token is minted by an admin via
  `GET /api/v1/admin/mcp/connector-token?scope=READ|READ_WRITE` (see `openapi.yaml`) and shown/copied in the
  Android admin screen. It's a stateless HMAC of `uid:scope` signed with `MCP_TOKEN_SECRET` — nothing is
  stored, and rotating the secret revokes every token at once. **Phase 2 replaces this with OAuth 2.1** (the
  real phone-connector path; the bearer token proves the tools via MCP Inspector / the Messages-API connector).

## Scopes

| Scope | Authority | Tools |
|-------|-----------|-------|
| `READ` | `SCOPE_MCP_READ` | all read tools |
| `READ_WRITE` | `SCOPE_MCP_READ` + `SCOPE_MCP_WRITE` | read tools **and** `logFood`, `createMeal` |

Every tool is scoped to the caller's `uid` (resolved from the token, propagated to the tool thread) — no
cross-user reach. Write tools called with a read-only token return a refusal string rather than acting.

## Tools

### Reads (any valid token)

| Tool | Params | Returns |
|------|--------|---------|
| `listDiets` | — | The user's diets: `id`, name, favorite flag. |
| `todayDashboard` | `date?` (`YYYY-MM-DD`, omit = today) | Calories consumed vs target, macros, per-slot logged state, streak. |
| `getProfile` | — | Body metrics, goal, activity level, units, daily nutrition targets. |
| `searchFoods` | `query` | Up to 8 matching foods with `id` and per-100g kcal/protein/carbs/fat. |

### Writes (require `READ_WRITE`)

| Tool | Params | Notes |
|------|--------|-------|
| `logFood` | `foodId`, `quantity`, `unit?`, `slot`, `date?` | Logs a food to a meal slot. |
| `createMeal` | `name`, `foods?` (`[{ foodId, quantity, unit? }]`) | Creates a reusable named meal. |

- **`unit`** ∈ `GRAM, ML, PIECE, CUP, TBSP, TSP` (default `GRAM`).
- **`slot`** ∈ `BREAKFAST, LUNCH, DINNER, MORNING_SNACK, EVENING_SNACK`.
- **`foodId`** comes from `searchFoods`.

## Write guardrails (Phase 1)

- **Scope check** — writes require `SCOPE_MCP_WRITE`; otherwise a read-only refusal string.
- **uid ownership** — all reads/writes constrained to the caller's `uid`.
- **Validation** — valid slot/unit; `0 < quantity ≤ 100,000`; food must exist (else "use searchFoods").
- **Caps** — meal name ≤ 100 chars; ≤ 50 foods per meal.
- **Idempotency** — an identical food log (same food/slot/quantity/unit/day) is skipped; a meal with an
  existing name is returned instead of duplicated. Agent retries don't double-write.

## Manual end-to-end check

Prereqs (local): set `MCP_TOKEN_SECRET` and `ADMIN_EMAILS=dev@mealplan.test` in gitignored `backend/.env`,
run `./scripts/local-up.sh`, and enable the `mcp_server` flag (admin screen or
`PUT /api/v1/admin/feature-flags/mcp_server`).

1. Mint a token: `GET /api/v1/admin/mcp/connector-token?scope=READ_WRITE` (as the admin) → copy `token`.
2. Point an MCP client at the connector URL with that bearer token:
   - **MCP Inspector:** `npx @modelcontextprotocol/inspector`, transport = SSE, URL = `http://localhost:8080/mcp/sse`,
     add header `Authorization: Bearer <token>`. Connect → list tools → call `listDiets`, then `logFood`.
   - **Claude (Messages-API connector):** attach the MCP server as a connector with the bearer token and ask
     it to read the dashboard / log a food.
3. Verify a `READ` token refuses `logFood`/`createMeal` and that a write persists (cross-check via the app or
   `GET /api/v1/daily-logs`).

> The consumer Claude **app** (add-connector flow) needs OAuth — that's Phase 2. Phase 1 is validated with
> Inspector / the Messages-API connector only.
