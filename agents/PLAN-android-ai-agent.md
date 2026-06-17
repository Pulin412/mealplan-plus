# Android AI Agent — Implementation Plan

**Goal:** Smart food logging via natural language on Android.  
**First use case:** User types "I had 2 eggs and 80g oats" → agent logs it automatically.  
**AI provider (dev):** Anthropic Claude Haiku (best tool use accuracy, use personal API key).  
**AI provider (prod):** Gemini 2.0 Flash free tier (swap via config, no code change).

See ADR-001 for provider comparison, ADR-002 for architecture.

---

## Sequencing

```
Phase 1 (backend) → Test 1 (curl/Postman) → Deploy to Cloud Run
        → Phase 2 (Android wiring) → Test 2 (Logcat debug button)
        → Phase 3 (Android UI) → Test 3 (full end-to-end demo)
```

---

## Phase 1 — Backend foundation
> No Android or app needed. Work entirely in `backend/`.

- [ ] **1.1** Add Spring AI BOM + Anthropic starter to `backend/build.gradle.kts`
- [ ] **1.2** Add `ANTHROPIC_API_KEY` to local `.env` and Cloud Run secret manager
- [ ] **1.3** Create `AgentChatRequest` + `AgentChatResponse` DTOs
- [ ] **1.4** Create `MealPlanToolService` with three `@Tool` methods:
  - `searchFoods(query)` — find matching foods for the user
  - `getTodayLog(date)` — read what is already logged today (agent context)
  - `logFood(foodId, quantity, unit, slot, date)` — write the log entry
- [ ] **1.5** Create `AgentController` — `POST /api/v1/agent/chat`
- [ ] **1.6** Add the new endpoint to `SecurityConfig` (authenticated like all `/api/v1/**`)
- [ ] **1.7** Integration test: "I had 2 eggs" → assert log entry created in DB

### 🧪 Test point 1 — curl, no Android needed
```bash
curl -X POST http://localhost:8080/api/v1/agent/chat \
  -H "Authorization: Bearer <firebase-token>" \
  -H "Content-Type: application/json" \
  -d '{"message": "I just had 2 scrambled eggs", "date": "2026-06-17"}'
```
Expected: agent reply in JSON + food log row created in DB.

---

## Phase 2 — Android wiring (no UI)
> Requires Phase 1 deployed to Cloud Run.

- [ ] **2.1** Deploy updated backend to Cloud Run
- [ ] **2.2** Add `AgentChatRequest` / `AgentChatResponse` data classes to Android
- [ ] **2.3** Add `agentChat` suspend fun to `MealPlanApi.kt`
- [ ] **2.4** Create `AgentRepository` — push → call agent → pull
- [ ] **2.5** Create `AgentViewModel` — message input, loading, reply, error state

### 🧪 Test point 2 — debug button in Settings screen
Add a temporary hardcoded call in `SettingsScreen.kt` that fires the agent and prints
the reply to Logcat. Confirm the food log appears on the Home screen after the pull.
Remove before Phase 3.

---

## Phase 3 — Android UI
> Requires Phase 2 passing.

- [ ] **3.1** Add chat FAB to Home screen
- [ ] **3.2** Build chat bottom sheet — input field, send button, reply bubble, loading state
- [ ] **3.3** Wire bottom sheet to `AgentViewModel`
- [ ] **3.4** Show confirmation in sheet: "Logged: 2 eggs · 140 kcal · Breakfast"
- [ ] **3.5** Remove Phase 2 debug button

### 🧪 Test point 3 — full demo
Type "I just had a bowl of oats with a banana" → food log updates on Home screen in real time.

---

## What is NOT in scope (yet)
- Meal suggestions and general Q&A (Phase 1 of ADR-001 use cases deferred)
- Webapp agent UI (deferred until Android is stable)
- Streaming responses (full response first, streaming as follow-up)
- Conversation history / multi-turn memory (stateless per request for now)
- Gemini Nano on-device (supplement, deferred)
