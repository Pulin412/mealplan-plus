# ADR-002: MCP Server Design and Agent API Layer

**Status:** Proposed  
**Date:** 2026-06-17  
**Author:** Pulin  
**Depends on:** ADR-001 (AI Agent Strategy)

---

## Context

ADR-001 decided that the AI agent lives in the Spring Boot backend and is called via a single REST endpoint by both Android and the webapp. Before building the agent, we need to define:

1. How the agent manipulates app data (foods, meals, diets, logs, plans)
2. How a single agent endpoint serves both clients without duplicating logic
3. What is missing from the current API layer to make this work
4. How Android's local-first / sync architecture fits into an agent that writes to the backend

### Current state of backend APIs

All CRUD endpoints already exist and are live:

| Domain | Backend endpoint | Webapp uses it | Android uses it |
|--------|-----------------|---------------|----------------|
| Foods | `GET/POST/DELETE /api/v1/foods` | Yes (direct) | **No** — sync only |
| Meals | `GET/POST/PUT/DELETE /api/v1/meals` | Yes (direct) | **No** — sync only |
| Diets | `GET/POST/PUT/DELETE /api/v1/diets` | Yes (direct) | **No** — sync only |
| Daily logs | `GET/POST/PUT/DELETE /api/v1/daily-logs` | Yes (direct) | **No** — sync only |
| Plans | `GET/PUT/DELETE /api/v1/plans/{date}` | Yes (direct) | **No** — sync only |
| Health metrics | `GET/POST/DELETE /api/v1/health-metrics` | Yes (direct) | **No** — sync only |
| Sync | `POST /api/v1/sync/push` + `GET /api/v1/sync/pull` | No | **Only interface** |

**Android does not call individual CRUD endpoints.** It writes to Room locally, then `SyncWorker` pushes the delta to the backend and pulls remote changes every 15 minutes.

### What does not exist yet

- A `/api/v1/agent/**` endpoint family on the backend
- Any Spring AI `@Tool`-annotated service methods (MCP tool layer)
- An agent endpoint registered in Android's `MealPlanApi.kt`
- A webapp hook for the agent endpoint

---

## The MCP Server Pattern

### What MCP is in this context

Model Context Protocol (MCP) defines a standard way for an AI model to call structured tools with typed inputs/outputs. In Spring AI, tools are plain Kotlin/Java methods annotated with `@Tool` on a `@Service` bean. The `ChatClient` receives a user message, decides which tools to call (and how many times), executes them, and incorporates the results into the final response.

The MCP server is **not a separate process** — it is co-located inside the Spring Boot application as a set of tool beans. The AI model calls these tools through Spring AI's function-calling layer, not over the network.

```
Android / Webapp
      |
      | POST /api/v1/agent/chat  { message, context }
      ↓
  AgentController (Spring Boot)
      |
      ↓
  ChatClient (Spring AI)  ←——  LLM (Gemini / Claude / Groq)
      |                              |
      | calls @Tool methods          | decides which tools to call
      ↓                              |
  MealPlanToolService  ←————————————┘
  (wraps existing Service layer)
      |
      ↓
  FoodService / MealService / DietService / LogService / PlanService
      |
      ↓
  Backend DB (Neon.tech Postgres)
      |
      ↓ (next sync pull, ~15 min or on-demand)
  Android Room DB
```

---

## Proposed Tool Inventory

Each tool maps 1-to-1 to an existing service method. No new business logic.

### Food tools
| Tool name | Underlying endpoint | Description |
|-----------|-------------------|-------------|
| `search_foods` | `GET /api/v1/foods/search` | Find foods by name — used before logging or building a meal |
| `create_food` | `POST /api/v1/foods` | Add a new food item with macros |
| `list_foods` | `GET /api/v1/foods` | List all user foods — used to give agent full context |

### Meal tools
| Tool name | Underlying endpoint | Description |
|-----------|-------------------|-------------|
| `list_meals` | `GET /api/v1/meals` | List all meals (agent needs this to avoid duplicating) |
| `create_meal` | `POST /api/v1/meals` | Create a named meal with food items |
| `update_meal` | `PUT /api/v1/meals/{id}` | Add/remove foods from an existing meal |

### Diet tools
| Tool name | Underlying endpoint | Description |
|-----------|-------------------|-------------|
| `list_diets` | `GET /api/v1/diets` | List all diets |
| `create_diet` | `POST /api/v1/diets` | Create a diet and assign meals to slots |
| `update_diet` | `PUT /api/v1/diets/{id}` | Modify a diet's meals or metadata |

### Log tools
| Tool name | Underlying endpoint | Description |
|-----------|-------------------|-------------|
| `get_today_log` | `GET /api/v1/daily-logs` (filtered by date) | Read what has been logged today — core context for suggestions |
| `log_food` | `POST /api/v1/daily-logs` | Log a food item for a specific slot and date |

### Plan tools
| Tool name | Underlying endpoint | Description |
|-----------|-------------------|-------------|
| `get_day_plan` | `GET /api/v1/plans/{date}` | Read planned meals for a date |
| `set_day_plan` | `PUT /api/v1/plans/{date}` | Assign a diet to a specific date |

### Health tools (read-only for agent — never write autonomously)
| Tool name | Underlying endpoint | Description |
|-----------|-------------------|-------------|
| `get_health_metrics` | `GET /api/v1/health-metrics` | Read weight, glucose, etc. — context for personalised suggestions |

---

## Agent Endpoint Design

A single endpoint serves both Android and webapp:

```
POST /api/v1/agent/chat
Authorization: Bearer <Firebase JWT>

{
  "message": "Create a high-protein breakfast meal with eggs and oats",
  "context": {
    "date": "2026-06-17",
    "slot": "BREAKFAST"          // optional hint from the UI
  }
}

→ 200 OK
{
  "reply": "I created a breakfast meal called 'Protein Breakfast' with 2 eggs (130 kcal) and 80g oats (300 kcal). Total: 430 kcal, 32g protein. Want me to assign it to today's plan?",
  "actionsPerformed": [
    { "tool": "create_food", "result": "created food: Egg (id=42)" },
    { "tool": "create_meal", "result": "created meal: Protein Breakfast (id=17)" }
  ]
}
```

The `actionsPerformed` array lets both clients know what changed so they can refresh the right screens, rather than doing a full reload.

---

## How Both Clients Use This

### Webapp
- Add a `useAgent` hook that calls `POST /api/v1/agent/chat`
- On response, use `actionsPerformed` to selectively invalidate the relevant SWR/state caches
- No new API endpoints needed — the agent writes to the same backend DB the webapp already reads

### Android
1. Add `agentChat` to `MealPlanApi.kt`:
   ```kotlin
   @POST("api/v1/agent/chat")
   suspend fun agentChat(@Body request: AgentChatRequest): AgentChatResponse
   ```
2. On agent response, trigger a **foreground sync pull** immediately (don't wait 15 minutes) so the user sees the agent's changes right away
3. The agent writes to the backend DB; the sync pull brings those rows to Room; the existing Compose UI reacts to Room `Flow<>` updates automatically

---

## Data Consistency: Android Local-First vs Agent Backend-First

This is the key tension. Android is local-first (Room is the source of truth on device). The agent is backend-first (it only sees data in the backend DB).

### Gap: unsynced Android data is invisible to the agent

If the user creates a food on Android that hasn't synced yet, the agent cannot see it. If the user asks "log my usual oats meal", the agent won't know "usual oats meal" exists if it was created offline.

### Mitigation strategy

| Scenario | Mitigation |
|----------|-----------|
| User invokes agent | Trigger a sync push first, then call the agent. Ensures the agent sees the latest local data. |
| Agent creates data | On agent response, trigger a sync pull. Ensures Android Room reflects agent changes within seconds, not 15 minutes. |
| Conflict (agent + user edit same item offline) | Existing last-write-wins conflict resolution applies. Agent writes have a server timestamp; local writes carry `updatedAt`. Whichever is newer wins on next sync. |

### Implementation note for Android

Before calling `/api/v1/agent/chat`, the `AgentRepository` should call `SyncWorker`'s sync logic directly (a one-shot foreground sync, not the periodic 15-minute job):

```kotlin
// AgentRepository.kt
suspend fun chat(message: String, context: AgentContext): AgentChatResponse {
    syncRepository.pushNow()           // upload any local changes first
    val response = api.agentChat(...)  // call agent
    syncRepository.pullNow()           // pull agent's changes immediately
    return response
}
```

---

## What Needs to Be Built (in order)

| # | What | Where | Notes |
|---|------|-------|-------|
| 1 | `MealPlanToolService` with `@Tool` methods | `backend/` | Thin wrappers over existing services — no new business logic |
| 2 | `AgentController` (`POST /api/v1/agent/chat`) | `backend/` | Spring AI `ChatClient` + tool beans + RAG context injection |
| 3 | `AgentChatRequest` / `AgentChatResponse` DTOs | `backend/` | Shared contract for both clients |
| 4 | Add `agentChat` to `MealPlanApi.kt` | `android/` | One new Retrofit method |
| 5 | `AgentRepository` with push-before / pull-after | `android/` | Wraps sync + API call |
| 6 | `useAgent` hook | `webapp/` | Calls agent endpoint, invalidates relevant caches |
| 7 | Agent UI (chat sheet / input) | `android/` + `webapp/` | Chat interface wired to the above |

Steps 1–3 are backend-only and can be built and tested independently before touching either client.

---

## What Does NOT Need to Change

- Android's existing sync architecture — it stays local-first for all non-agent interactions
- Webapp's existing direct-API calls for manual CRUD — those don't go through the agent
- The backend CRUD endpoints themselves — the agent's tool layer calls the service layer directly, not the HTTP endpoints (no internal HTTP loopback)
- Room schema / migrations — agent changes arrive via sync, not a new code path

---

## Open Questions

1. **Streaming vs full response:** Should the agent endpoint stream tokens (`text/event-stream`) for a chat feel, or return a full response? Streaming is better UX but more complex on Android (no native SSE client in Retrofit; would need OkHttp). Recommendation: start with full response, add streaming in a follow-up.

2. **Conversation history:** Should multi-turn chat context (previous messages) be stored per user in the backend DB, or should the client send the full conversation history on each call? Storing on backend is cleaner but requires a new table. For MVP, client-sent history is simpler.

3. **Sync trigger on Android:** The push-before / pull-after pattern requires exposing a one-shot sync method from `SyncWorker`. Currently only the periodic job is defined. A `suspend fun syncNow()` function needs to be extracted.

4. **Tool call transparency:** Should the `actionsPerformed` list be shown to the user ("I created 2 foods and 1 meal"), or kept internal? Showing it builds trust; hiding it is cleaner. Both clients should at least log it.
