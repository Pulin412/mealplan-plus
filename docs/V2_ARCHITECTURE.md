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
 backend/            android               webapp
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
- **android** and **webapp** each **generate their own** typed HTTP client + DTO
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
| **android** | Retrofit `*Api`, DTO models (`build/generated/…`) | Room entity + DAO, Repository (DTO↔entity + cache/sync), ViewModel, Compose UI |
| **webapp** | `types.generated.ts` (DTO types) | `lib/api/*.ts` typed calls, hooks, React UI |

Regeneration commands:

| Tier | Command |
|------|---------|
| backend | `cd backend && ./gradlew openApiGenerate` (runs as part of `build`) |
| android | `./gradlew :android:openApiGenerate` (runs as part of build) |
| webapp | `cd webapp && npm run gen:api` |

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

---

# Offline-first data layer & sync

> **Status: TARGET design (agreed, not yet built).** The current v2 `FoodRepository`
> does per-operation *online write-through* (`api.createFood()` inline, throws when
> offline) — that's the wrong strategy and will be refactored. **Foods is the reference
> implementation** for this pattern; every later screen follows it.
>
> Applies to **android**. The **webapp PWA stays online-first** for now (reads Neon
> live, no local store) — it still sees Android's changes because they land on the server.
> Aligned with Android's official guidance:
> <https://developer.android.com/topic/architecture/data-layer/offline-first>

## The core principle

> **Room is the single source of truth for the UI. The UI never touches the network.
> A background SyncWorker is the *only* thing that talks to the server.**

```
UI ──read/write──▶ Room (local truth) ◀──sync──▶ Neon ◀──read── PWA
       instant, works offline            background        (online-first)
```

Clean layering (stricter than current code — repositories currently call the API inline):

- **Repository = local-only.** Exposes Room `Flow`s for reads; writes go to Room and mark
  the row dirty. It does **not** call Retrofit.
- **SyncWorker = the sole network boundary.** Pushes local changes up, pulls server
  changes down. Nothing else calls the sync API.

## 1. Identity — one client-generated UUID

The single most important decision; it removes a whole class of bugs (the v1/current-v2
"three IDs" duplication).

- The **client generates a UUID** when a record is created (offline, no server needed).
  Server-seeded rows (e.g. system foods) get their UUID from the server.
- That UUID is the **Room primary key** *and* the server identity (`serverId`). Sync is an
  **idempotent upsert-by-UUID**. The server's internal numeric `id` is irrelevant to clients.
- Because the UUID is identical on client and server *from creation*, a record created
  offline and later pulled back **matches its own local row** → updates, never duplicates.

> This is exactly why duplication is impossible: **UUID = Room primary key + upsert on
> pull.** Pull the whole dataset any number of times → still one row per record. (The v1
> bug duplicated because it deduped on the local auto-increment id, which never matched.)

## 2. Reads — always local, reactive

- UI/ViewModel observe a Room `Flow` (`getFoods(): Flow<List<Food>>`), never the API.
- On a fresh device/login, Room is empty → the **first pull** populates it → thereafter
  reads are local.
- Use the **LCE** (Loading / Content / Error) state pattern; protect readers with `catch`.

## 3. Writes — strategy *per operation*, not one global choice

| Operation | Strategy | Behaviour |
|-----------|----------|-----------|
| Create/edit food, **log meal**, favorite, health metric, day plan | **Lazy / optimistic** | Write Room now (with client UUID) + mark **dirty**; SyncWorker pushes later. Works offline. |
| Delete (any data) | **Lazy + tombstone** | Mark `deletedAt` (soft delete) + dirty; sync propagates the delete. Never hard-delete locally first. |
| Login / register | **Online-only** | Can't authenticate offline; fail loudly. Firebase, not Room. |
| Analytics / Crashlytics | **Queued (best-effort)** | Fire-and-forget; already handled by Firebase. |
| Browse / list | **Read** (local) | Never hits the network directly. |

## 4. Dirty tracking & tombstones (the two gears that make sync work)

- **Dirty flag** (or an outbox): every local write marks the row `PENDING`. The SyncWorker
  pushes only dirty rows and clears the flag on success. Without this, sync can't know what
  changed. *(Dirty flag is enough at our scale; an outbox table is the heavier option.)*
- **Tombstones (soft delete):** a delete sets `deletedAt`; the row is only physically
  removed after the delete is confirmed synced. The backend already returns `tombstones[]`
  on pull.

## 5. SyncWorker — the *when*

One **WorkManager** worker for **all** entity types (not per-screen, not a manual button):

- **Bidirectional:** push dirty rows/tombstones (`/api/v1/sync/push`) + pull changes since a
  stored cursor (`/api/v1/sync/pull?since=…`).
- **Triggers:** app foreground/launch, connectivity regained, periodic safety net (~15 min),
  debounced after a local write. Constraint: `NetworkType.CONNECTED`.
- **Dedup:** `enqueueUniqueWork("sync_<entity>_<uuid>", ExistingWorkPolicy.REPLACE, …)`.
- **Retry:** `Result.retry()` → automatic exponential backoff.
- An optional **"Sync now"** in settings is fine as a convenience/debug affordance — never
  the primary path.

## 6. Conflict resolution — last-write-wins

- LWW on `updatedAt`; the backend already does this (remote wins only when
  `remoteUpdatedAt > localUpdatedAt`).
- Prefer **server-set timestamps** — device clocks drift, and skewed clocks are the known
  weak spot of LWW. Acceptable for a single-user/multi-device app; don't over-engineer
  (no field-merge/CRDTs).
- **During pull, don't clobber a dirty local row** — push it first (or let LWW/timestamp
  decide). This is the one place pull and local writes interact.

## 7. Sync direction: pull vs push

- **Pull-based** (refetch-on-open) — easy, but doesn't scale for relational data.
- **Push-based delta sync** (`/sync/push|pull` + tombstones) — scales for relational data
  (foods → meals → diets → logs → plans), works offline indefinitely. **This is the
  destination.** Foods may start pull-simple, but the target is push-based delta sync.
- The backend `/sync` API is **already the right shape** — reuse it; rebuild only the client.

## 8. Known client-side concern — serialization adapters

The generated models use `java.time` types; the backend's JSON wire format doesn't match
Gson's defaults, so Retrofit needs adapters. Centralised in
`android/.../data/remote/ApiSerialization.kt` (`apiGson()`):

- ✅ `Instant` ⇄ epoch millis (done — was causing every Foods response to fail to deserialize).
- ⬜ `LocalDate` (backend serializes as a JSON array `[y,m,d]`) and enum types — add when
  those screens are built. **Register all such adapters in `apiGson()`, one place.**

## Complete mental model

```
Create/Edit (data):  UI → Room now (client UUID) + mark DIRTY → SyncWorker pushes ↑ later
Delete (data):       UI → Room row DELETED (tombstone) + DIRTY → SyncWorker syncs delete ↑
Read (data):         UI ← Room always (populated by pull ↓)
Auth:                online-only (Firebase, not Room)
SyncWorker (bg, event-triggered, all entities):
    push DIRTY ↑  +  pull since-cursor ↓  →  LWW + upsert-by-UUID (no dupes)
Neon = hub → PWA reads it live and sees everything
```

---

# Design system & component library

> Source of truth: **`design_v2/`** — `MealPlan+ Build Spec` (§3 tokens + component
> vocabulary, data model, screens) and `MealPlan Home` (the interactive prototype = the
> look & interaction spec). When they disagree: prototype wins on visuals, spec wins on data.

## The rule
> **Screens compose from a per-platform component library; they do not hand-roll styling.
> The shared contract across platforms is the *token set* (§3 values), not code — Compose
> and React each implement it once.**

Clients can't share code (Kotlin vs TS), so there are two libraries implementing one design:

| | Android (Compose) | Webapp (React) |
|---|---|---|
| Tokens | `ui/theme/` (`Color.kt`, `Type.kt`) | `globals.css` vars + a tokens module |
| Components | `ui/components/` | `src/components/ui/` |

## Tokens (`ui/theme/Color.kt`) — from Build Spec §3
Core (teal/ink/bg/success/danger), the **muted ramp** (`#5b666e → #8a949b → #9aa4aa →
#a2abb1`), the **border/surface ramp** (`#eaeef0 … #f2f4f5`), **macro colours** (P/C/F),
and the **favourite gold** (`oklch(0.72 0.13 75)`). DM Mono for all numerals/data
(`tabular-nums`).

## Component library (`ui/components/`) — extracted from Foods, first consumer
- `MacroText` — `P12 · C21 · F21` (terse, DM Mono, no units — spec format)
- `CalorieValue` — SemiBold number + small `kcal`
- `FavoriteStar` — **gold** when active, `#c4ccd1` when empty
- `VerifiedBadge` — `✓ Verified` / `Custom`
- `AppCard` — white, 1px border, 12dp radius
- `SegmentedControl` — grey track, white active pill (list/compact, and reusable)

Meals/Diets/Home ("same shell" per the spec) compose from these instead of duplicating.

## Non-negotiable design rules (spec §9 / §13 — WCAG 2.1 AA)
- **≥44px tap targets** on all icon buttons/steppers (a launch blocker, not polish).
- Body text ≥12px; `tabular-nums` globally; labels on icon buttons for TalkBack.
- Every screen implements empty / loading / error / offline states — not just the happy path.

## Open decision (logged)
- **Macros: per-100g vs per-serving.** The design models `Food` macros **per serving** with a
  free-text `serving` label ("170 g"); our backend/openapi store **per 100g**. We kept
  per-100g for now (UI shows a subtle "per 100g"); revisit before Meals/Diets, since Meals
  sum food servings.
