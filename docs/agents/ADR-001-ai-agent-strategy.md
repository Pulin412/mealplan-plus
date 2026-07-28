# ADR-001: AI Agent Strategy for MealPlan+

**Status:** Proposed  
**Date:** 2026-06-10  
**Author:** Pulin  

---

## Context

MealPlan+ is a meal planning and food logging app with an Android client, a Next.js PWA, and a Spring Boot backend deployed on Cloud Run. We want to add an AI agent layer that can:

1. **Meal suggestions** — recommend meals or foods based on diet, logged history, and health goals
2. **Chatbot / Q&A** — answer user questions about nutrition, workouts, and their own logged data
3. **Smart food logging** — parse natural-language descriptions ("had a bowl of oats with banana") into structured log entries

### Constraints

- **Zero new infrastructure cost** during development and early production (free tiers are acceptable)
- **Personal dev/testing** can use the developer's existing Anthropic Claude subscription API key
- **Cloud API calls are required** — purely on-device is acceptable as a supplement but not sufficient for webapp parity
- **Both Android and webapp** must have access to the same capabilities; this means agent logic lives in the backend and is exposed via REST API

### Existing assets we can leverage

- **Spring AI** is already a dependency in the Spring Boot backend
- **PgVectorStore** (pgvector on Neon.tech Postgres) is already provisioned — ideal for RAG
- **Daily logs, food items, workouts, health metrics** are all in the DB — rich context for personalisation
- The backend is already deployed and authenticated (Firebase JWT)

---

## Options

### Option A — Google Gemini API (Free Tier → Paid Scale)

**Model:** Gemini 2.0 Flash (free tier) / Gemini 1.5 Pro (paid)

**Free tier limits (as of 2026):**
- Gemini 2.0 Flash: 15 RPM · 1,500 RPD · 1M TPM — free forever
- Gemini 1.5 Flash: same limits

**Spring AI integration:** `spring-ai-vertex-ai-gemini-spring-boot-starter` or direct Google AI SDK. Spring AI has first-class Gemini support.

**Cost if you exceed free tier:** ~$0.075 / 1M input tokens (Flash) — cheapest commercial LLM per token.

| Pros | Cons |
|------|------|
| Best free tier among commercial LLMs | Google data-processing terms |
| Multimodal — can accept food photos | Spring AI Gemini integration less mature than OpenAI |
| Scales cheaply once free tier is exhausted | Google dependency (vendor lock-in risk) |
| 1M token context window | Flash model is less capable than Pro on complex reasoning |
| Fast response times | Rate limits hit quickly for concurrent users |

**Verdict for this project:** Best default choice for free + cheap-scale. Multimodal is a bonus for future food-photo logging.

---

### Option B — Groq API (Free Tier, Open-Source Models)

**Models:** Llama 3.3 70B, Llama 3.1 8B, Gemma 2 9B, Mixtral 8x7B

**Free tier limits (as of 2026):**
- Llama 3.3 70B: 30 RPM · 14,400 RPD · 131,072 context
- Llama 3.1 8B: 30 RPM · 14,400 RPD · much higher throughput

**Spring AI integration:** Groq exposes an OpenAI-compatible API. Use `spring-ai-openai-spring-boot-starter` with `spring.ai.openai.base-url=https://api.groq.com/openai`.

**Cost if free tier is exhausted:** ~$0.59 / 1M input tokens (Llama 3.3 70B) — comparable to Gemini Flash.

| Pros | Cons |
|------|------|
| Extremely fast inference (custom ASIC hardware) | No multimodal (text only) |
| Generous free tier | Another third-party vendor dependency |
| Open-source models (no proprietary lock-in on model side) | Food/nutrition domain knowledge weaker than Gemini/Claude |
| OpenAI-compatible — zero code change if switching | Free tier pauses for inactivity; warm-up latency |
| Works with Spring AI OpenAI adapter today | RPD limit (14,400) can be hit by one active user with heavy use |

**Verdict for this project:** Good fallback or secondary option. Very fast but text-only is limiting for future photo logging.

---

### Option C — Anthropic Claude API (Personal Development / Cheapest Paid)

**Note:** A Claude.ai subscription (claude.ai/pro) does **not** include API access. API billing is separate at [console.anthropic.com](https://console.anthropic.com). However, the developer can add a small credit ($5–10) and use it for personal dev/testing at minimal cost.

**Cheapest model:** `claude-haiku-4-5` — $0.80 / 1M input · $4 / 1M output tokens

**No free tier** — every token is billed.

**Spring AI integration:** `spring-ai-anthropic-spring-boot-starter` — first-class support.

| Pros | Cons |
|------|------|
| Best reasoning quality among all options | No free tier |
| Excellent tool use / function calling (critical for smart food logging) | Most expensive per-token of the three options |
| Safest output (lowest hallucination rate) | Claude.ai subscription ≠ API key — needs separate billing |
| First-class Spring AI support | Anthropic terms prohibit storing Claude outputs for training |
| Claude Code familiarity (same API) | Overkill for simple meal suggestions |

**Cheapest path:** Use `claude-haiku-4-5` — it is cheaper than GPT-4o-mini and better at following structured output/tool use instructions. For 1,000 food-log parses / day at ~300 tokens each: **~$0.24/day**.

**Verdict for this project:** Best for personal dev and testing. Not the right default for a free-tier-first production app, but consider as an opt-in premium feature later.

---

### Option D — Gemini Nano (On-Device, Android Only)

**Cost:** Free, always. No network required.

**Already planned** in CLAUDE.md under "AI: Gemini Nano on-device for Android offline use".

**Integration:** Android `com.google.ai.edge.aicore:aicore` SDK. Requires Android 14+ (API 34) and a Gemini Nano–capable device (Pixel 8+, some Samsung flagships).

| Pros | Cons |
|------|------|
| Completely free, offline, private | Android 14+ and specific hardware only |
| Zero latency (no network round-trip) | Model is much smaller — weaker reasoning |
| No rate limits | No webapp support — Android-only |
| Works when backend is down | Limited context window (~1K–4K tokens) |

**Verdict for this project:** Ideal as a **supplement** for offline quick suggestions and food-name autocomplete on Android. Not suitable as the primary agent because it excludes webapp users entirely and has limited capability.

---

### Option E — Self-Hosted Ollama on Cloud Run (Free Compute)

**Models:** Any open-source model — Llama 3.2 3B, Phi-3 mini, Gemma 2 2B (small models only — Cloud Run has no GPU)

**Cost:** Cloud Run CPU pricing. Small models (2–4B params) on 2 vCPU / 4 GB RAM can generate ~3–10 tokens/sec — very slow for chat. A minimum instance to avoid cold starts costs ~$15–30/month.

**Spring AI integration:** `spring-ai-ollama-spring-boot-starter` with `spring.ai.ollama.base-url=http://localhost:11434`.

| Pros | Cons |
|------|------|
| No per-token cost | CPU inference is extremely slow (3–10 tok/sec vs 100+ tok/sec on Groq) |
| Full data privacy — nothing leaves your infra | Minimum-instance cost to avoid cold starts (~$15–30/mo) |
| Spring AI native support | Small models only — weaker reasoning than Gemini/Claude |
| Model is replaceable | High ops overhead (model updates, resource tuning) |

**Verdict for this project:** Not recommended as primary. The combination of slow inference and minimum-instance cost makes it worse than the free tiers of Gemini or Groq for this app's scale.

---

## Comparison Summary

| | Gemini (A) | Groq (B) | Claude (C) | Gemini Nano (D) | Ollama (E) |
|---|---|---|---|---|---|
| **Free tier** | Yes (best) | Yes (generous) | No | Yes (always) | Yes (compute) |
| **Webapp support** | Yes | Yes | Yes | No | Yes |
| **Android support** | Via backend | Via backend | Via backend | Native | Via backend |
| **Multimodal** | Yes | No | No | Limited | No |
| **Cheapest paid scale** | $0.075/1M | $0.59/1M | $0.80/1M | n/a | ~$0.02/1M equiv |
| **Reasoning quality** | Good | Good (70B) | Best | Weak | Weak–Good |
| **Inference speed** | Fast | Fastest | Fast | Fast (device) | Very slow |
| **Spring AI support** | First-class | Via OpenAI adapter | First-class | N/A (Android SDK) | First-class |

---

## Use Case × Option Mapping

### Meal suggestions
Context needed: user's current diet, recent logs, health goals, food preferences.  
Approach: RAG — embed the user's diet plan and log history into PgVectorStore; retrieve relevant context per query; pass to LLM.  
**Best fit:** Gemini Flash (free tier, handles 1M token context for large history retrieval)

### Chatbot / Q&A
Context needed: same as meal suggestions + general nutrition knowledge.  
Approach: RAG with vector search over user data + optionally a pre-embedded nutrition knowledge base.  
**Best fit:** Gemini Flash (free) or Groq Llama 70B (free, faster response)

### Smart food logging
Context needed: food database (USDA / local) + LLM to parse input → structured FoodItem log.  
Approach: Tool use / function calling — LLM calls a `searchFood(name)` tool that queries the food DB, then returns structured JSON.  
**Best fit:** Claude Haiku (best tool use accuracy) for personal use; Gemini Flash (free tier) for production

---

## Proposed Decision

Implement a provider-agnostic agent layer in the Spring Boot backend using **Spring AI's abstraction**. Configure the LLM provider via environment variable so it can be swapped without code changes.

### Phase 1 — Free tier baseline (now)
- **Provider:** Gemini 2.0 Flash via Google AI Studio (free API key)
- **Developer testing:** Anthropic Claude Haiku via Anthropic API key (personal, separate billing)
- **On-device:** Gemini Nano for Android quick suggestions (offline, supplement only)
- **Vector store:** PgVectorStore already provisioned — use it for RAG context

### Phase 2 — Production scale (when free tier limits are hit)
- Upgrade to Gemini paid tier (cheapest commercial option at $0.075/1M tokens)
- Consider Groq as a speed-optimised alternative for chat endpoints

### Phase 3 — Premium feature (optional)
- Offer Claude Haiku as an opt-in "premium accuracy" mode for smart food logging, funded by a future in-app subscription tier

### Configuration approach (Spring AI)

```yaml
# application.yml — swap provider without code change
spring:
  ai:
    # Default: Gemini free tier
    google:
      gemini:
        api-key: ${GEMINI_API_KEY}
        model: gemini-2.0-flash
    # Opt-in: Anthropic (dev/testing)
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:}
      model: claude-haiku-4-5-20251001
```

```java
// Single interface — provider is injected by Spring AI
@Service
public class MealPlanAiService {
    private final ChatClient chatClient; // Spring AI abstraction

    // same code regardless of whether Gemini, Groq, or Claude is wired
}
```

---

## Consequences

- Backend gains a `/api/v1/agent/*` endpoint family; both Android and webapp call the same API
- PgVectorStore (already provisioned) is used for user-context RAG — no new infra needed
- Switching LLM provider requires only a config change, not a code change
- Free tier limits: Gemini 2.0 Flash gives 1,500 requests/day free — sufficient for a single-user personal app; needs monitoring once shared with other users
- Android Gemini Nano requires an explicit availability check; fallback to backend API when unavailable or offline check fails
- Smart food logging via tool use requires the food search API to be exposed as a Spring AI `FunctionCallback` — design work needed before implementation

---

## Open Questions

1. Should the agent have memory across sessions (conversation history persisted to DB) or be stateless per request?
2. Should user data embedded in PgVectorStore be per-user isolated (separate namespace/filter) or shared? (Nutrition knowledge base can be shared; personal logs must be isolated.)
3. Is food-photo logging (multimodal) in scope for Phase 1, or deferred?
4. What is the acceptable latency target for the chat endpoint? (Streaming responses vs. full response)
