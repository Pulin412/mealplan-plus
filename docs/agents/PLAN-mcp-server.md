# PLAN: MCP Connector Server — expose our tools to users' own AI agents

**Status:** Planning · **Date:** 2026-08-18 · **Branch:** `feature/mcp-server` (off `develop`)

> ⚠️ **Not the same "MCP" as ADR-002.** `ADR-002-mcp-server-design.md` uses "MCP server" to mean Spring AI's
> *internal* `@Tool` layer behind `/api/v1/agent/chat` (the in-app assistant, hosted LLM = we pay). **This doc** is
> the **external remote-MCP connector server**: a Streamable-HTTP endpoint that a user's **own** Claude connects to,
> so *their* subscription's tokens do the work. Complementary surfaces, same underlying service layer.

---

## The idea

Stand up a remote MCP endpoint so a user connects **their own Claude** and reads/writes their diets, meals, plans,
and food logs directly from that agent.

**Why it fits us:** it flips the cost model. The in-app assistant makes *us* host the LLM loop; the MCP connector puts
the LLM on the *user's* Claude subscription → near-zero incremental LLM cost, ideal for the zero-billing guardrail. It
**reuses the existing service layer** (`logFoodByName`, `DietService`/`MealService`/`LoggingService`) — a thin protocol
adapter, not a rewrite. We **keep both** surfaces (in-app assistant for casual, MCP for power users).

---

## Decisions locked (2026-08-18)

| Decision | Resolution |
|---|---|
| Build | **A** — MCP inside Spring Boot, isolated `mcp/` package, dependency arrow **inward only** (MCP→services, never reverse) so it's easy to rip out. Separate Gradle module deferred; package boundary is enough. |
| Client | **Claude-first**; ChatGPT read-only fast-follow (individual ChatGPT writes are Biz/Enterprise-gated + web-setup-only → dead end for individuals now). Claude iOS/Android *use* connectors, but you *add* them once on claude.ai (web) → syncs to phone; free tier = 1 connector. |
| Auth | **Phase 1:** stateless **URL-token** `HMAC(uid, serverSecret)` embedded in the connector URL — personal use, no storage/migration; revoke = rotate secret; caveat = no per-user revocation. **Phase 2:** **OAuth 2.1** for multi-user (approval-gated: auth + CORS). **Key insight:** a PAT via the Messages/Responses API is only a *dev* proof — the *consumer* Claude app needs OAuth's add-connector flow, so the URL-token trick is what makes the app usable for **one** person without building OAuth. Trigger for OAuth = "just me → other users." |
| Flags | **DB-backed** feature-flag framework; `mcp_server` = flag #1; runtime toggle from admin screen. |
| Admin | Email **allowlist** = `pulins412@gmail.com` (NB: distinct from the login email); `isAdmin` derived from Firebase JWT email; Android admin screen. |
| Tools | Reads (`listDiets`, `todayDashboard`, `getProfile`, `searchFoods`) + `logFood` (reuse `logFoodByName`) + `createMeal` (new → `MealService`). |
| Product Q | **Keep both** MCP and the in-app assistant — don't drop the multi-provider failover work. |

---

## Phased to-do plan

### Phase 0 — Feature-flag framework + admin plumbing  *(MCP depends on this)*
- [ ] Flyway migration: `feature_flags` (`key, enabled, updated_by, updated_at`) — ⚠️ **needs approval**
- [ ] `FeatureFlagService.isEnabled(key)` + cache; seed `mcp_server` (default **off**)
- [ ] `app.admin-emails` allowlist (`pulins412@gmail.com`); `isAdmin` from JWT email; surface on profile/`/me`
- [ ] Admin API `GET`/`PUT /admin/feature-flags` (admin-gated, non-admin → 403); add to `docs/openapi.yaml`
- [ ] Android `AdminScreen` + VM (list/toggle flags); Settings entry visible only when `isAdmin`
- [ ] Tests: flag service, admin authz (403 path), Android VM

### Phase 1 — MCP server  *(behind `mcp_server` flag)*
> **Auth pivot (2026-08-18):** header **bearer** token, not URL-token. Spring AI 1.0.0 MCP server = SSE
> (two endpoints); a URL token isn't reattached to the message POSTs, and consumer Claude's add-connector
> allows no custom headers — so URL-token can't drive the phone app anyway. Header token proves the tools
> now (via MCP Inspector / Messages-API connector); **OAuth (Phase 2) is the real phone-connector path.**
- [x] Spring AI MCP-server dep (`spring-ai-starter-mcp-server-webmvc:1.0.0`, SSE) in isolated `domain/mcp/`, flag-gated
- [x] Bearer-token auth filter → resolve `uid` + scope (stateless HMAC token; read vs read-write) + Reactor context propagation
- [x] Read tools: `listDiets`, `todayDashboard`, `getProfile`, `searchFoods`
- [x] Write tools: `logFood` + `createMeal` (call `LoggingService`/`MealService` directly)
- [x] Write guardrails: write-scope check, slot/unit/quantity validation, size caps, food-existence, idempotency
- [x] Tests: `McpTokenServiceTest` + `McpServerIntegrationTest` (real MCP client over SSE: connect→auth→list→read→write→read-only-refusal)
- [x] **1d:** admin "Connect Claude" section — mint + show/copy the bearer connector token, scope select
      (`GET /api/v1/admin/mcp/connector-token` → `McpConnectorTokenResponse`; Android admin screen)
- [x] **1d:** document the MCP tool contract → `docs/agents/MCP-TOOLS.md`
- [x] **1d:** manual E2E against a real MCP client — verified via MCP Inspector (SSE + bearer token → list/read/write tools OK)
- [ ] (Phase 3) structured tool-call logging + minimal write audit

**Gotchas found (saved to memory):** Spring AI runs tools OFF the servlet thread → thread-local
SecurityContext lost → fixed via micrometer `ThreadLocalAccessor` + `Hooks.enableAutomaticContextPropagation()`.
Kotlin has nested block comments → a literal `/`+`*` in a KDoc (e.g. writing `/mcp` + `/**`) breaks compilation.

### Phase 2 — OAuth 2.1 (public)  *— ⚠️ approval-gated (auth + CORS), deferred until multi-user*
- [ ] PRM `/.well-known/oauth-protected-resource` + AS metadata; `/authorize`+PKCE, `/token`, DCR `/register`, JWKS
- [ ] Firebase-bridge login at the authorize step; uid-scoped, audience-bound tokens + scopes
- [ ] Resource-server validation + `401 WWW-Authenticate`
- [ ] Decide then: Spring Authorization Server (self-host) vs managed vendor (must clear zero-billing free-tier check)

### Phase 3 — Observability + evals  *(keep in mind, later)*
- [ ] Tool-call telemetry (name, uid, latency, outcome) + Micrometer via actuator; write audit table; per-session correlation id
- [ ] Eval harness: golden cases for tool correctness (reuse the in-app Phase-1.5 eval work); behavior regression
- [ ] Re-evaluate ChatGPT (read-only)

---

## Cross-cutting write guardrails  *(applied from Phase 1)*
- **uid ownership scoping** on every tool — queries always constrained to the caller's `uid`, no cross-user reach
- **read vs read-write scope** enforced from the token
- **input validation + caps** — required fields, type/range checks (qty > 0, max foods/meal, string-length limits), reject absurd values
- **rate limiting per user** — stop a runaway agent loop
- **idempotency / dedup** on writes — agent retries don't double-log
- **MCP tool annotations** (`destructiveHint` / `idempotentHint` / `readOnlyHint`) → client asks for confirmation on writes
- **Neon-friendly** — handlers short & connectionless (don't re-pin compute; keep `minimum-idle: 0` intact)

---

## Approval gates (repo hard rules)
- Phase 0 **Flyway migration** (`feature_flags` table)
- Phase 2 **OAuth 2.1 / CORS** change
- Every commit / push / PR

## Related
- `ADR-002-mcp-server-design.md` — internal Spring-AI tool layer (in-app assistant); the tools this connector reuses.
- `PLAN-bring-your-own-ai.md` (on `feature/bring-your-own-ai`) — the in-app assistant track this complements.
