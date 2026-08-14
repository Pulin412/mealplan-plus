# PLAN — Bring Your Own AI (nutrition assistant + multi-provider failover)

**Branch:** `feature/bring-your-own-ai` (off `develop`)
**Status:** planning → Phase 1 not started
**Owner:** Pulin · **Scope for now:** personal use, Android + backend only (webapp + multi-user rollout deferred)

This is the living source of truth. Update the checkboxes and the "Current status" line as we go so any
context reset can resume from here.

---

## Goal

A chat assistant that answers questions about the user's own data (profile / meals / diets / metrics)
and (later) generates + saves diet/meal combinations — with the **LLM provider swappable and
auto-failing-over** so UX is never affected when a free-tier limit is hit.

## Confirmed decisions (locked)

- **Backend-mediated** LLM calls (Android → `POST /api/v1/agent/chat` → Spring AI → provider). Reuses the
  existing agent + tool-calling + DB access. Not client-direct.
- **Provider failover chain**, not a single provider. Default order: **Groq → Gemini → Mistral → Anthropic**
  (Anthropic = paid backstop). Order/enablement must be **changeable in settings** (server-side config,
  single-user for now; per-user later).
- **One OpenAI-compatible adapter covers Groq / OpenRouter / Mistral / Gemini** (all speak the OpenAI wire
  format; Gemini via its OpenAI-compat endpoint). Anthropic keeps its dedicated Spring AI client (already
  wired in `AgentConfig`). Adding a provider = a config row, not code.
- **Reactive-only failover** for v1: on `429`/quota/`5xx`/timeout → next provider; on `401/403` (bad key)
  → skip + log, don't hammer; all exhausted → friendly "assistant unavailable" message. Proactive
  per-provider daily counters = later.
- **Non-streaming** per attempt (failover-safe). Streaming = later.
- **Tool-calling-capable models only** in the chain — every provider must support OpenAI-style function
  calling so the agent's tools work consistently across a failover.
- **No pgvector/RAG in v1** — inject a profile/diets/meals context summary or rely on tool-calls. Add
  vector RAG only if context grows too big.
- ADR-001 is reference, not gospel — we diverged (chain+failover instead of single Gemini default; no RAG
  in v1). Its pricing/model IDs are stale (~2026-06); re-verify before quoting.

## Existing assets (already built — reuse)

- `backend/.../domain/agent/AgentController.kt` — `POST /api/v1/agent/chat` (currently a food-logging assistant).
- `backend/.../domain/agent/MealPlanToolService.kt` — `@Tool` functions: `searchFoods`, `getTodayLog`, `logFood`.
- `backend/.../domain/agent/AgentConfig.kt` — provider chosen via `AGENT_PROVIDER` env (ollama / anthropic).
- `backend/.../domain/agent/AgentDto.kt` — `AgentChatRequest` / `AgentChatResponse`.
- pgvector on Neon is provisioned (unused for now).

---

## Phases & to-dos

### Phase 1 — Multi-provider failover + grounded Q&A (backend, personal)
- [x] OpenAI-compatible provider factory: build a Spring AI `ChatModel`/`ChatClient` from `{name, baseUrl, apiKey(env), model}`.
      → `ResilientAgentService.buildModel()` (per-provider `OpenAiApi`+`OpenAiChatModel`; `completionsPath` override for Gemini).
      Added `spring-ai-openai` (non-starter) dep. Anthropic via autowired optional `AnthropicChatModel`.
- [x] Provider-chain config surface (server-side, single-user): ordered list + enabled flag; seeded
      Groq → Gemini → Mistral → Anthropic. Keys via env (`GROQ_API_KEY`/`GEMINI_API_KEY`/`MISTRAL_API_KEY`/`ANTHROPIC_API_KEY`).
      → `agent.providers` in `application.yml` bound by `AgentProperties`/`ProviderConfig`. **No DB table → no Flyway** (env-only for now).
- [x] `ResilientAgentService`: iterate the chain; catch `429`/quota/`5xx`/timeout → next; `401/403` → skip+log;
      all-exhausted → friendly error. Reactive-only. Distinguishes quota (429) from auth (401/403) via `httpStatusOf()`.
- [x] Generalize the agent system prompt: food-logging → nutrition assistant (Q&A + logging).
      → Externalized to a model-agnostic **playbook** `resources/agent/playbook.md` (classpath-loaded, `{{TODAY}}`/`{{SLOT_HINT}}` tokens); routing decision table + few-shot so behaviour is identical across model swaps.
- [x] Add read tools mirroring existing ones: `getDiets`, `getMeals`, `getMetrics`, `getRecentLogs` (in `MealPlanToolService`).
- [x] Robustness tools: **`logFoodByName`** (search+match+log in one hop — avoids the multi-step flow weak models malform) + **`getProfile`** (ground suggestions). Tool descriptions trimmed to mechanics (routing moved to the playbook to save tokens). `logFood` kept as id-based fallback.
- [x] `GET /api/v1/agent/providers` — read-only chain status (name/type/model/enabled/ready, **no keys**) for the settings screen.
- [x] Verify the Groq model tool-calls at runtime — **PASSED** (2026-08-13). `openai/gpt-oss-120b` ran
      `logFoodByName` in one hop and persisted (confirmed via non-LLM `GET /daily-logs`). Notes: `moonshotai/
      kimi-k2-instruct` is NOT on the account (404); `llama-3.3-70b-versatile` malforms tool calls. Set the
      yml `GROQ_MODEL` default to `openai/gpt-oss-120b`. Gemini/Mistral tool-calling still unverified (no keys).
- [x] Expose in `docs/openapi.yaml`: `/agent/chat` + `/agent/providers` (Assistant tag) + `AgentChatRequest`/
      `AgentChatResponse`/`ToolAction`/`ProviderStatus` schemas. Backend (`kotlin-spring`, interfaceOnly) + Android
      (`kotlin`) both regenerate clean; `AssistantApi` generated for the app. ⚠️ contract change (clients rebuilt).
- [x] `cd backend && ./gradlew build test` — green.

### Phase 2 — Android chat UI + provider settings
- [x] Android chat screen → `POST /agent/chat` (non-streaming). `AgentChatScreen` + `AgentChatViewModel`
      (single StateFlow, `_state.update{}`), `AgentRepository` over generated `AssistantApi`. Message list,
      input, thinking dots, suggestion chips, "via <provider>" chip. Entry = ✨ button next to Settings on Home.
- [x] Settings screen: **read-only** provider status (`GET /agent/providers`) — `AiProvidersScreen` +
      `AiProvidersViewModel`, linked from `SettingsScreen`. Reorder/enable-disable **deferred** (needs a
      backend `PUT /agent/providers` + storage — Phase 4). Shows name/model/ready/enabled, no keys.
- [x] `:android:compileDebugKotlin` + `:android:testDebugUnitTest` green.
- [ ] On-device test (user drives build+install+launch; add temp LAN entry, revert before commit).

### Phase 1.5 — Eval harness (gate for qualifying providers)
- [ ] Small fixture set (~10–30 cases) run through the agent **against each provider in the chain**.
- [ ] Priority: (1) tool-calling correctness (right tool + args, e.g. "log oats" → searchFoods→logFood);
      (2) grounded Q&A (answers from fixture data, no hallucination); (3) later: createDiet action safety.
- [ ] Grading: structural asserts for tool calls; keyword/contains or LLM-as-judge (strong model) for free text.
- [ ] Manually-run backend harness, **excluded from normal CI** (costs money, hits external providers) — tag it.
- [ ] Purpose: regression gate whenever a provider/model is added or reordered in the chain.

### Phase 3 — Generative planning (actions)
- [ ] `createMeal` / `createDiet` tools → agent composes AND saves, reusing the meal/diet domain.
- [ ] Test create-a-diet-from-a-prompt end to end.

### Phase 4 — Rollout (DEFERRED — needs approval; touches secrets/auth)
- [ ] Per-user BYO provider + key. Decision pending: **client-held key** (no server secret) vs
      **encrypted-at-rest** in Postgres (needs KMS/app secret). A user's BYO key = their personal 1-entry chain.
- [ ] Free-tier quota metering + limits UI + proactive per-provider daily counters (reset per quota window).
- [ ] Webapp mirror (chat + settings).
- [ ] pgvector RAG if injected context gets too large.
- [ ] Streaming responses (+ safe pre-first-token failover).

---

## MCP — decided NO for v1
Tools are in-process Spring AI `@Tool` functions in the backend that owns the DB — direct repo calls, no
protocol hop. MCP would only add cost, and the failover chain argues against it (MCP-over-tool-calling
support varies by provider; in-process tools are defined once and Spring AI adapts them per provider).
Revisit MCP only to expose these tools to *external* agents (Claude Desktop, third-party), or to consume a
*third-party* MCP — a Phase 4+ product decision, not plumbing for this feature. (See existing ADR-002.)

## Open questions / notes
- Provider-config storage: a small JSON/settings row server-side now; per-user table at rollout.
- Anthropic backstop uses a paid key (yours) — fine for personal; the free tiers (Groq/Gemini/Mistral) sit ahead of it.
- Zero-billing guardrail: only the Anthropic backstop costs money; keep free-tier keys ahead of it. No Firestore/Functions/etc.
- Approvals still required per repo rules: any Flyway migration, contract/auth change, and every commit/push/PR.

## Current status
**Phase 1 backend core built & green** (`./gradlew build test`). Provider factory + `agent.providers` chain
config + `ResilientAgentService` failover + generalized prompt + 4 read tools + `GET /agent/providers` all landed
on `feature/bring-your-own-ai` (not committed yet). Old `AgentConfig` chatClientBuilder + `AGENT_PROVIDER`
switch removed (ollama dep still present, unused). **Smoke-tested live** with a real Groq free key (2026-08-13):
- ✅ Failover works — observed a genuine groq `429` (TPM 12k limit) fall through the chain.
- ✅ Grounded Q&A works — groq answered `getDiets` / `getMetrics` correctly over seeded dev data.
- ❌ **Blocker:** groq `llama-3.3-70b-versatile` malforms tool calls on multi-step/write flows
  (`<function=searchFoods {"query":"oats"}>` → Groq `400 tool_use_failed: ... not in request.tools`).
  Single read tool-calls are fine; `searchFoods→logFood` and `getTodayLog` fail. Not our code — model quirk.

**Next action (start of next session):**
1. Swap groq's model to a reliable tool-calling one — try `moonshotai/kimi-k2-instruct` or `qwen/qwen3-32b`
   (avoid llama-3.3-70b for function calling). Re-run the log smoke test until `searchFoods→logFood` persists.
2. Add a 2nd real free-provider key (Gemini or Mistral) so failover has a working landing spot.
3. Minor polish: gate `/providers` anthropic `ready` on `ANTHROPIC_API_KEY` (bean autoconfigures without it);
   widen `httpStatusOf()` to classify the anthropic 401 ("cannot retry due to server authentication").
4. Then Phase 2 (Android chat + settings) + OpenAPI typing.
Test harness lives in scratchpad (`smoke.sh` / `log_test.sh`): signs in dev@mealplan.test via Firebase,
hits `/agent/providers` + `/agent/chat`. Backend: `GROQ_API_KEY=… ./scripts/local-up.sh` (H2, seeded).
