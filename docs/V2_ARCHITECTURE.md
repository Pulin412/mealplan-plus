# MealPlan+ v2 — Architecture

How the three tiers relate, and the rules that keep them from duplicating each other.
Companion to `V2_PLAN.md` (what to build) — this doc is **how** they fit together.

## Three tiers, one contract

There is **no shared client code** (`shared/` KMP is dead; the webapp is TypeScript).
The only thing shared across tiers is the **API contract**: `docs/openapi.yaml`.

```
                 docs/openapi.yaml          ← the ONE contract (neutral, repo root)
                        │  generate
     ┌──────────────────┼───────────────────────┐
 backend/            android-v2               webapp-v2
 (the BRAIN)         (native client)          (PWA client)
 Spring interfaces   Retrofit client + DTOs   TS types
 + model DTOs        (generated)              (generated)
   │                     │                        │
 controllers         Repository + Room        service + hooks + local store
 implement them      (DTO↔entity, sync)       (DTO↔state)
   │                     │                        │
 HTTP service        ViewModel → Compose      hook → React
 (Cloud Run)         UI                       UI
```

- **backend/** implements the contract and owns everything that can be centralised:
  persistence, auth, sync/merge rules, validation, and server-only compute
  (AI agent, pgvector search, the shared food database).
- **android-v2** and **webapp-v2** each **generate their own** typed HTTP client + DTO
  models from the same spec, then write a thin platform-specific layer on top.

The backend is the **de-duplication engine**, not a source of duplication: anything on
the server is written once and both clients consume it over the wire.

## The rule (this is the whole point)

> **The API contract and DTO shapes are declared once, in the spec, and *generated* into
> every tier. No client ever hand-writes a DTO or an endpoint path. No business rule lives
> in a client unless it *must* run offline — everything else is computed server-side.**

Two model types per entity is **expected, not duplication**:

| Type | Source | Purpose |
|------|--------|---------|
| `FoodDto` | 🔧 generated from spec | the wire contract — never hand-edited |
| Room `Food` / local model | ✍️ you write | offline cache; has local-only concerns (autoincrement id, epoch `updatedAt`, `servingLabel`) |

A small mapper (`DtoDto.toEntity()`) bridges them. Fighting this costs more than it saves.

## What is generated vs. what you write

| Tier | 🔧 Generated (never hand-edit) | ✍️ You write |
|------|-------------------------------|--------------|
| **backend** | `*Api` interfaces, DTO models (`build/generated/…`) | Controller (`: FoodsApi`), Service, JPA entity + entity→DTO mapping |
| **android-v2** | Retrofit `*Api`, DTO models (`build/generated/…`) | Room entity + DAO, Repository (DTO↔entity + cache/sync), ViewModel, Compose UI |
| **webapp-v2** | `types.generated.ts` (DTO types) | `lib/api/*.ts` typed calls, hooks, React UI |

Regeneration commands:

| Tier | Command |
|------|---------|
| backend | `cd backend && ./gradlew openApiGenerate` (runs as part of `build`) |
| android-v2 | `./gradlew :android-v2:openApiGenerate` (runs as part of build) |
| webapp-v2 | `cd webapp-v2 && npm run gen:api` |

All three read `docs/openapi.yaml`. Editing the contract → regenerate all three → the
compiler/tsc flags any tier that has drifted.

## Canonical example — `GET /api/v1/foods?favorites={bool}` → `List<FoodDto>`

### Spec (`docs/openapi.yaml`)
```yaml
/api/v1/foods:
  get:
    operationId: listFoods
    parameters:
      - { name: favorites, in: query, schema: { type: boolean, default: false } }
    responses:
      '200': { content: { application/json: { schema:
        { type: array, items: { $ref: '#/components/schemas/FoodDto' } } } } }
```

### Backend
```kotlin
// 🔧 generated: FoodsApi.kt
interface FoodsApi {
    fun listFoods(favorites: Boolean): ResponseEntity<List<FoodDto>>
}

// ✍️ FoodController.kt — implements the generated interface (compiler enforces conformance)
@RestController
class FoodController(private val service: FoodService) : FoodsApi {
    override fun listFoods(favorites: Boolean) =
        ResponseEntity.ok(service.list(currentUid(), favoritesOnly = favorites))
}

// ✍️ FoodService.kt — the logic + entity→DTO mapping lives here, once
fun list(firebaseUid: String, favoritesOnly: Boolean = false): List<FoodDto> { /* … */ }
```

### Android
```kotlin
// 🔧 generated: Retrofit FoodsApi + FoodDto
interface FoodsApi {
    @GET("api/v1/foods")
    suspend fun listFoods(@Query("favorites") favorites: Boolean? = false): Response<List<FoodDto>>
}

// ✍️ FoodRepository.kt — the service layer: generated API → Room cache
class FoodRepository @Inject constructor(private val dao: FoodDao, private val api: FoodsApi) {
    fun getFoods(): Flow<List<Food>> = dao.getAllFoods()             // UI reads local cache
    suspend fun refresh() {
        val dtos = api.listFoods(favorites = false).body() ?: return
        dao.upsertAll(dtos.map { it.toEntity() })                    // DTO → Room entity
    }
}
```
UI observes `repo.getFoods()` (local `Food`) and never sees a `FoodDto`.

### Webapp
```typescript
// 🔧 generated: types.generated.ts (via npm run gen:api)

// ✍️ lib/api/foods.ts — re-export the generated DTO, thin typed call
import type { components } from "@/lib/api/types.generated";
import { apiFetch } from "./client";

export type FoodDto = components["schemas"]["FoodDto"];   // from spec, NOT hand-written

export const listFoods = (favorites = false): Promise<FoodDto[]> =>
  apiFetch<FoodDto[]>(`/api/v1/foods?favorites=${favorites}`);

// ✍️ hooks/useFoods.ts — consumes the service; UI state lives here
```

DTO types come from `foods.ts` (which derives them from the spec). Only genuinely
UI-only types (`FoodSort`, `FoodViewMode`, `ManualFoodForm`) are hand-written, in
`src/types/food.ts`.

## Do / don't

- ✅ Change an endpoint or field → edit `docs/openapi.yaml`, regenerate all three, fix
  what the compiler flags. (Contract change needs human approval — see root `CLAUDE.md`.)
- ✅ Put shared business logic on the server.
- ✅ Keep client code to: generated API client, local cache/model, repository, VM/hooks, UI.
- ❌ Hand-write a `*Dto` type or an endpoint path string that duplicates the spec.
- ❌ Import a hand-written DTO where a generated one exists (this is how drift starts).
- ❌ Reimplement a server business rule in a client unless it must run offline.
