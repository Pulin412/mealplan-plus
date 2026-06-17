# Spring AI Learning Guide — MealPlan+ Agent

A personal reference for how the AI agent in this project works, built alongside
the implementation. Each section links to the actual source files.

---

## 1. What is Spring AI?

Spring AI is the Spring Framework's answer to building AI-powered applications in Java/Kotlin.
It provides a **single abstraction layer** over different LLM providers (Anthropic, OpenAI,
Ollama, Gemini, etc.) so that your application code doesn't know or care which model is running.

The key abstraction chain is:

```
Your code
   │
   ▼
ChatClient          ← the high-level fluent API you call in your controllers
   │
   ▼
ChatModel           ← provider-specific implementation (OllamaChatModel, AnthropicChatModel, …)
   │
   ▼
HTTP / local        ← actual call to Ollama on localhost, or Anthropic's API, etc.
```

> Official docs: https://docs.spring.io/spring-ai/reference/

---

## 2. The files we built

```
backend/src/main/kotlin/com/mealplanplus/api/domain/agent/
├── AgentConfig.kt          ← wires the right ChatModel at startup
├── AgentController.kt      ← REST endpoint  POST /api/v1/agent/chat
├── AgentDto.kt             ← request + response data classes
└── MealPlanToolService.kt  ← the tools the agent can call (search, log)
```

---

## 3. Phase 1 walkthrough — what each file does

### 3.1 AgentDto.kt
[`backend/.../agent/AgentDto.kt`](../backend/src/main/kotlin/com/mealplanplus/api/domain/agent/AgentDto.kt)

Plain data classes. Nothing Spring AI-specific here.

```kotlin
data class AgentChatRequest(
    val message: String,      // "I just had 100g of chicken breast"
    val date: String? = null, // optional: "2026-06-17"
    val slot: String? = null  // optional hint: "LUNCH"
)

data class AgentChatResponse(
    val reply: String,
    val actionsPerformed: List<ToolAction> = emptyList()
)
```

**Why `date` and `slot` as optional hints?**
The agent figures these out from the message text, but having them explicitly avoids
ambiguity (e.g. "I had this yesterday" when the device time zone is off).

---

### 3.2 MealPlanToolService.kt
[`backend/.../agent/MealPlanToolService.kt`](../backend/src/main/kotlin/com/mealplanplus/api/domain/agent/MealPlanToolService.kt)

This is where the **tools** live. A tool is a method the LLM can decide to call during
a conversation turn. Spring AI discovers them via the `@Tool` annotation.

```kotlin
@Service
class MealPlanToolService(...) {

    @Tool(description = "Search the food database by name. ...")
    fun searchFoods(query: String): String { ... }

    @Tool(description = "Get a summary of all foods already logged for a given date. ...")
    fun getTodayLog(date: String): String { ... }

    @Tool(description = "Log a food item for the user. ...")
    @Transactional
    fun logFood(foodId: Long, quantity: Double, unit: String, slot: String, date: String): String { ... }
}
```

**What `@Tool` actually does at runtime:**

1. When Spring AI builds the request to the LLM, it serialises each `@Tool` method
   into a JSON schema (parameter names, types, description) and sends it in the request.
2. The LLM reads the schemas and, if it decides a tool is needed, returns a *tool call*
   in its response instead of plain text.
3. Spring AI intercepts that response, invokes the matching method on the service bean,
   captures the return value, and sends it back to the LLM in a follow-up request.
4. The LLM then produces a final human-readable reply.

This loop can happen multiple times in one request — e.g. the agent calls `searchFoods`
first, then `logFood` with the ID it found.

**`uid` comes from `SecurityContextHolder`** — the Firebase JWT filter has already
validated the token and stored the user's Firebase UID as the principal name before
this bean is ever called.

---

### 3.3 AgentController.kt
[`backend/.../agent/AgentController.kt`](../backend/src/main/kotlin/com/mealplanplus/api/domain/agent/AgentController.kt)

```kotlin
@RestController
@RequestMapping("/api/v1/agent")
class AgentController(
    private val chatClientBuilder: ChatClient.Builder,  // injected from AgentConfig
    private val tools: MealPlanToolService
) {
    private val chatClient: ChatClient by lazy { chatClientBuilder.build() }

    @PostMapping("/chat")
    fun chat(@RequestBody request: AgentChatRequest, auth: Authentication): AgentChatResponse {
        val reply = chatClient.prompt()
            .system(systemPrompt)   // tells the LLM its role + context
            .user(request.message)  // the user's actual message
            .tools(tools)           // makes @Tool methods available
            .call()
            .content() ?: "Sorry, I couldn't process that request."

        return AgentChatResponse(reply = reply)
    }
}
```

Key points:

- `ChatClient` is **stateless per call** — no session memory between requests.
  Every call starts fresh. (Multi-turn memory is out of scope for now — see PLAN.)
- `.tools(tools)` — Spring AI inspects the `MealPlanToolService` bean, finds all
  `@Tool` methods, and registers them for this call only.
- `.call().content()` — blocking call. Spring AI also supports `.stream()` for
  streaming responses, but we use blocking for simplicity.
- The `system(...)` prompt is the hidden instruction that sets the agent's persona
  and tells it the date, meal slots, and what to do when a food isn't found.

---

### 3.4 AgentConfig.kt
[`backend/.../agent/AgentConfig.kt`](../backend/src/main/kotlin/com/mealplanplus/api/domain/agent/AgentConfig.kt)

This is the **provider selector**. Both the Anthropic and Ollama starters are on the
classpath. Without this config, Spring would try to auto-configure both and throw a
`NoUniqueBeanDefinitionException` when injecting `ChatClient.Builder`.

```kotlin
@Configuration
class AgentConfig {

    @Bean
    fun chatClientBuilder(
        @Value("\${agent.provider:ollama}") provider: String,
        @Autowired(required = false) ollamaModel: OllamaChatModel?,
        @Autowired(required = false) anthropicModel: AnthropicChatModel?
    ): ChatClient.Builder {
        val model: ChatModel = when (provider) {
            "anthropic" -> anthropicModel ?: error("Set ANTHROPIC_API_KEY")
            else        -> ollamaModel    ?: error("Run: ollama serve")
        }
        return ChatClient.builder(model)
    }
}
```

By defining our own `ChatClient.Builder` bean, we replace the Spring AI auto-configured
one (`@ConditionalOnMissingBean` means ours wins). The controller just injects
`ChatClient.Builder` and never knows which provider is behind it.

**Provider selection logic:**

| `AGENT_PROVIDER` env var | Model used | When |
|---|---|---|
| `ollama` (default) | `OllamaChatModel` (localhost:11434) | Local dev |
| `anthropic` | `AnthropicChatModel` (api.anthropic.com) | Production (Cloud Run) |

---

## 4. Configuration — application.yml

[`backend/src/main/resources/application.yml`](../backend/src/main/resources/application.yml)

```yaml
agent:
  provider: ${AGENT_PROVIDER:ollama}   # override with AGENT_PROVIDER=anthropic in prod

spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:}   # empty → Anthropic beans not created locally
      chat:
        options:
          model: ${AI_MODEL:claude-haiku-4-5-20251001}
          max-tokens: 1024
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        options:
          model: ${OLLAMA_MODEL:qwen2.5:7b}
```

All values are env-var driven with sensible local defaults. Nothing to change in code
when deploying — just set the right env vars in Cloud Run.

---

## 5. Dependency setup — build.gradle.kts

[`backend/build.gradle.kts`](../backend/build.gradle.kts)

```kotlin
repositories {
    mavenCentral()  // Spring AI 1.0.0 GA is on Maven Central
}

dependencies {
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic:1.0.0")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama:1.0.0")
    // ...
}
```

**Why no BOM?**
We tried the Spring AI BOM (`spring-ai-bom`) with the `io.spring.dependency-management`
plugin, but the version resolved as empty string at Gradle resolution time.
Specifying the version explicitly on each artifact is more explicit and avoids this.

**Artifact name change in 1.0.0 GA:**
The Anthropic starter was renamed between milestone and GA:
- Milestones (up to M6): `spring-ai-anthropic-spring-boot-starter`
- GA (1.0.0): `spring-ai-starter-model-anthropic`

Same pattern for all providers: `spring-ai-starter-model-<provider>`.

---

## 6. Ollama — local setup

Ollama runs LLMs entirely on your machine. No API key, no internet, no cost.

### Install

```bash
# macOS
brew install ollama

# or download from https://ollama.com
```

### Pull a model

```bash
# qwen2.5:7b — good tool-calling accuracy, ~4 GB
ollama pull qwen2.5:7b

# lighter option (~2 GB, faster on CPU)
ollama pull llama3.2
```

### Start the server

```bash
ollama serve
# runs at http://localhost:11434 by default
```

Verify it's running:
```bash
curl http://localhost:11434/api/tags
# should return a JSON list of your downloaded models
```

### Model recommendations for tool calling

| Model | Size | Tool calling | Notes |
|---|---|---|---|
| `qwen2.5:7b` | 4 GB | Excellent | Best balance for agentic tasks |
| `mistral:7b` | 4 GB | Good | Reliable, well-tested |
| `llama3.2:3b` | 2 GB | Moderate | Fast on CPU, less reliable for multi-step tools |
| `llama3.1:8b` | 5 GB | Good | Stronger reasoning than 3.2 |

> Tool calling requires Ollama ≥ 0.2.8. Check: `ollama --version`

---

## 7. How to run and test

### Step 1 — Start Ollama

```bash
# Install (one-time)
brew install ollama

# Pull a model (one-time, ~4 GB download)
ollama pull qwen2.5:7b

# Start the server (keep this terminal open)
ollama serve
```

Verify it's ready:
```bash
curl http://localhost:11434/api/tags
# → {"models":[{"name":"qwen2.5:7b", ...}]}
```

If you get "connection refused", Ollama isn't running. Run `ollama serve` first.

---

### Step 2 — Open the agent endpoint for local testing

The endpoint requires a Firebase JWT by default. For a quick smoke test, temporarily
add it to the permit-all list in
[`SecurityConfig.kt`](../backend/src/main/kotlin/com/mealplanplus/api/config/SecurityConfig.kt):

```kotlin
.requestMatchers(
    "/actuator/health",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/h2-console/**",
    "/api/v1/agent/**"   // ← add this line for local smoke testing only
).permitAll()
```

> **Remember to remove this before committing.** It's only for local dev.

---

### Step 3 — Start the backend

```bash
# From the repo root (AGENT_PROVIDER defaults to 'ollama')
./gradlew :backend:bootRun
```

You should see in the logs:
```
Started MealPlanApiApplication in X.XXX seconds
```

If you see `Ollama not available — run: ollama serve`, Ollama isn't running (go back to Step 1).

---

### Step 4 — Send a test message

```bash
curl -s -X POST http://localhost:8080/api/v1/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I just had 100g of chicken breast for lunch",
    "date": "2026-06-17",
    "slot": "LUNCH"
  }' | python3 -m json.tool
```

Expected response:
```json
{
  "reply": "Logged 100g of chicken breast (~165 kcal) to your LUNCH on 2026-06-17.",
  "actionsPerformed": []
}
```

Try a few more messages to exercise the tools:

```bash
# Should search and log
"I had a banana and 200ml of whole milk for breakfast"

# Should say it can't find the food and suggest adding manually
"I had a bowl of my grandma's kheer"

# Should read the existing log before logging
"What did I eat today so far? I want to add a protein shake"
```

---

### Step 5 — Verify the tool calls happened (logs)

Watch the backend terminal. When the agent calls a tool, Spring AI logs it at DEBUG
level. You should see lines like:

```
[agent] Calling tool: searchFoods(query=chicken breast)
[agent] Tool result: id=42 | Chicken Breast | 165 kcal ...
[agent] Calling tool: logFood(foodId=42, quantity=100.0, ...)
```

If you only see the final reply but no tool call logs, set the log level for Spring AI
temporarily in `application.yml`:

```yaml
logging:
  level:
    org.springframework.ai: DEBUG
```

---

### Step 6 — Verify the food was actually logged (H2 console)

The H2 in-memory database console is available at:
`http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:mealplandb`
- Username: `sa`
- Password: *(leave blank)*

Run this query to see logged foods:
```sql
SELECT lf.*, dl.date, dl.firebase_uid
FROM logged_foods lf
JOIN daily_logs dl ON dl.id = lf.daily_log_id
ORDER BY lf.id DESC
LIMIT 10;
```

If a row appears with your food ID, slot, and today's date — the full tool calling loop
worked end to end.

---

### Testing with a real Firebase token

Once you remove the permit-all shortcut above, use a real token:

```bash
# Get your token from the Android app's Logcat — search for "idToken"
# Or generate one via the Firebase Auth REST API emulator

curl -s -X POST http://localhost:8080/api/v1/agent/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <firebase-id-token>" \
  -d '{"message": "I just had 2 scrambled eggs", "date": "2026-06-17"}'
```

---

### Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `connection refused` on port 8080 | Backend not started | `./gradlew :backend:bootRun` |
| `connection refused` on port 11434 | Ollama not running | `ollama serve` |
| `Ollama not available` error at startup | Ollama started after backend | Restart backend |
| Agent replies but no food logged | Tool call failed silently | Check DEBUG logs, verify food exists in DB |
| `model not found` from Ollama | Model not pulled | `ollama pull qwen2.5:7b` |
| `401 Unauthorized` | Forgot to add permit-all | Add `/api/v1/agent/**` to SecurityConfig (Step 2) |
| Empty or garbled reply | Model too small for tool calling | Switch to `qwen2.5:7b` or `mistral:7b` |

---

## 8. Swapping the provider

To switch from Ollama to Anthropic (or any other provider), there is **zero code change**.
It is entirely config/env driven.

### Local → Anthropic

```bash
export ANTHROPIC_API_KEY=sk-ant-...
export AGENT_PROVIDER=anthropic
./gradlew :backend:bootRun
```

### Production (Cloud Run)

Set these env vars in Cloud Run:
```
ANTHROPIC_API_KEY = <secret from Secret Manager>
AGENT_PROVIDER    = anthropic
```

### Why this works

`AgentConfig.kt` reads `agent.provider` (which maps to `AGENT_PROVIDER` env var)
and returns a `ChatClient.Builder` backed by the matching `ChatModel` bean.
`AgentController` only ever sees `ChatClient` — it has no import of any provider class.

---

## 9. How tool calling works — step by step

When you send "I had 100g of chicken breast":

```
1. AgentController calls chatClient.prompt()...call()

2. Spring AI builds the HTTP request:
   - Includes the system prompt (role, date, slot info)
   - Includes the user message
   - Includes JSON schemas for all 3 @Tool methods

3. LLM (Ollama/Anthropic) responds:
   {
     "tool_call": { "name": "searchFoods", "arguments": {"query": "chicken breast"} }
   }

4. Spring AI invokes MealPlanToolService.searchFoods("chicken breast")
   → returns "id=42 | Chicken Breast | 165 kcal | 31g protein | 0g carbs | 3.6g fat (per 100g)"

5. Spring AI sends the tool result back to the LLM as a follow-up message

6. LLM responds again:
   {
     "tool_call": { "name": "logFood", "arguments": {"foodId": 42, "quantity": 100.0, "unit": "GRAM", ...} }
   }

7. Spring AI invokes MealPlanToolService.logFood(42, 100.0, "GRAM", "LUNCH", "2026-06-17")
   → returns "Logged Chicken Breast — 100gram (~165 kcal) to LUNCH on 2026-06-17."

8. LLM produces the final human reply:
   "Done! Logged 100g of chicken breast (~165 kcal) to your lunch. 💪"

9. AgentController returns AgentChatResponse(reply = "Done! ...")
```

The `@Tool` description text is what the LLM reads to decide **when** to call each tool.
Write descriptions as if you're explaining the function to a person who needs to decide
whether to use it.

---

## 10. What is NOT implemented yet (from the plan)

See [`PLAN-android-ai-agent.md`](./PLAN-android-ai-agent.md) for the full todo list.

- **1.2** — `ANTHROPIC_API_KEY` in Cloud Run Secret Manager (needed before prod deploy)
- **1.7** — Integration test: assert log entry created in DB after a full agent call
- **Phase 2** — Android `MealPlanApi.kt` method + `AgentRepository` + `AgentViewModel`
- **Phase 3** — Chat FAB + bottom sheet UI on Home screen

---

## 11. Key Spring AI concepts — quick reference

| Term | What it is |
|---|---|
| `ChatModel` | Provider-specific implementation (one per provider) |
| `ChatClient` | Fluent API wrapping a `ChatModel`; what you use in controllers |
| `ChatClient.Builder` | Factory for `ChatClient`; you can set defaults (system prompt, tools, advisors) |
| `@Tool` | Marks a method as callable by the LLM |
| `Advisor` | Middleware in the call chain (e.g. `MessageChatMemoryAdvisor` for history) |
| `PromptTemplate` | Parameterised prompt strings (not used yet in this project) |
| `VectorStore` | Semantic search index for RAG (future phase) |

---

## 12. Useful links

- [Spring AI Reference Docs](https://docs.spring.io/spring-ai/reference/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
- [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Ollama model library](https://ollama.com/library)
- [Ollama tool calling support](https://ollama.com/blog/tool-support)
- [ADR-001 — AI provider strategy](./ADR-001-ai-agent-strategy.md)
- [ADR-002 — MCP server design](./ADR-002-mcp-server-design.md)
- [Implementation plan](./PLAN-android-ai-agent.md)
