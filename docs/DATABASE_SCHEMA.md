# MealPlan+ — Database Schema

> **Target version:** 30  
> **Previous version:** 26 (backed up in `backup/` before the v27 redesign)  
> **Last updated:** 2026-04-17

---

## Design Principles

1. **Foods → Meals → Diets → Day Plans** — strict hierarchy from atomic unit upward
2. **`slotType` belongs to context, not meals** — a meal is a reusable food collection; which slot it fills is determined by `diet_slots` or `planned_slots`, not by the meal itself
3. **Planning vs Logging are separate** — `day_plans`/`planned_slots` = intent; `daily_logs`/`logged_foods` = reality (streak uses logs only)
4. **Minimal user scoping** — `user_id` only where genuinely needed for isolation (`day_plans`, `daily_logs`, `health_metrics`, `grocery_lists`). Meals and diets are app-wide; `is_system` flag distinguishes built-ins from user-created content
5. **No destructive migrations** — every schema change adds an explicit numbered `MIGRATION_X_Y`; no `fallbackToDestructiveMigration` ever

---

## Entity Relationship Diagram

```
food_items
  │
  ├─── meal_foods ──────────── meals
  │                              │
  │                         diet_slots ──────── diets
  │                              │
  │                        planned_slots ────── day_plans ─── users
  │                              │                               │
  ├─── planned_slot_foods ───────┘                          daily_logs
  │                                                              │
  └─── logged_foods ──────────────────────────────────────────── ┘
  │
  └─── grocery_items ──── grocery_lists ─── users

health_metrics ──── users
tags ──── diet_tags ──── diets
```

---

## Tables

### `users`
One row per Firebase account.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `firebase_uid` | TEXT | NOT NULL UNIQUE |
| `email` | TEXT | |
| `display_name` | TEXT | |
| `created_at` | INTEGER | NOT NULL (epoch ms) |

---

### `food_items`
Atomic nutritional unit. System foods (`is_system_food = 1`) are shared across all users; user-created foods have `user_id` set.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `user_id` | INTEGER | NULLABLE → `users(id)` CASCADE (NULL = system food) |
| `name` | TEXT | NOT NULL |
| `calories_per_100g` | REAL | NOT NULL |
| `protein_per_100g` | REAL | NOT NULL |
| `carbs_per_100g` | REAL | NOT NULL |
| `fat_per_100g` | REAL | NOT NULL |
| `fiber_per_100g` | REAL | |
| `sugar_per_100g` | REAL | |
| `sodium_per_100g` | REAL | |
| `glycemic_index` | INTEGER | NULLABLE (0–100) |
| `serving_size` | REAL | NOT NULL DEFAULT 100 |
| `serving_unit` | TEXT | NOT NULL DEFAULT 'GRAM' |
| `is_system_food` | INTEGER | NOT NULL DEFAULT 0 |
| `barcode` | TEXT | NULLABLE |
| `server_id` | TEXT | NULLABLE |
| `updated_at` | INTEGER | NOT NULL (epoch ms) |
| `synced_at` | INTEGER | NULLABLE (epoch ms) |

**Indexes:** `(user_id)`, `(barcode)`, `(name)` WHERE `is_system_food = 1` UNIQUE

---

### `meals`
Named collection of foods. **Does not carry `slot_type`** — that belongs to `diet_slots` or `planned_slots`.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `name` | TEXT | NOT NULL |
| `description` | TEXT | NULLABLE |
| `is_system` | INTEGER | NOT NULL DEFAULT 0 |
| `server_id` | TEXT | NULLABLE |
| `created_at` | INTEGER | NOT NULL (epoch ms) |
| `updated_at` | INTEGER | NOT NULL (epoch ms) |
| `synced_at` | INTEGER | NULLABLE (epoch ms) |

> **Change from v26:** `user_id`, `slot_type`, and `custom_slot_id` removed. Meals are now app-global; slot assignment happens at diet/plan level.

---

### `meal_foods`
Junction: which foods (and in what quantity) make up a meal.

| Column | Type | Constraints |
|--------|------|-------------|
| `meal_id` | INTEGER | NOT NULL → `meals(id)` CASCADE |
| `food_id` | INTEGER | NOT NULL → `food_items(id)` CASCADE |
| `quantity` | REAL | NOT NULL |
| `unit` | TEXT | NOT NULL DEFAULT 'GRAM' |
| `notes` | TEXT | NULLABLE |

**PK:** `(meal_id, food_id)`  
**Indexes:** `(food_id)`

---

### `diets`
Named day template — a reusable collection of meals assigned to slots.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `name` | TEXT | NOT NULL |
| `description` | TEXT | NULLABLE |
| `is_system` | INTEGER | NOT NULL DEFAULT 0 |
| `is_favourite` | INTEGER | NOT NULL DEFAULT 0 |
| `created_at` | INTEGER | NOT NULL (epoch ms) |
| `server_id` | TEXT | NULLABLE |
| `updated_at` | INTEGER | NOT NULL (epoch ms) |
| `synced_at` | INTEGER | NULLABLE (epoch ms) |

> **Change from v26:** `user_id` removed. Diets are app-global.

---

### `diet_slots`
Junction: which meal fills which slot in a diet template. `slot_type` is the canonical slot name (BREAKFAST, LUNCH, DINNER, NOON, EVENING, or any custom string).

| Column | Type | Constraints |
|--------|------|-------------|
| `diet_id` | INTEGER | NOT NULL → `diets(id)` CASCADE |
| `slot_type` | TEXT | NOT NULL |
| `meal_id` | INTEGER | NULLABLE → `meals(id)` SET_NULL |
| `instructions` | TEXT | NULLABLE |

**PK:** `(diet_id, slot_type)`  
**Indexes:** `(meal_id)`

> **Renamed from `diet_meals`** for clarity. Semantically identical; `slot_type` column unchanged.

---

### `day_plans`
A user's plan for a specific date. Replaces `plans`.

| Column | Type | Constraints |
|--------|------|-------------|
| `user_id` | INTEGER | NOT NULL → `users(id)` CASCADE |
| `date` | INTEGER | NOT NULL (epoch ms, midnight UTC) |
| `is_completed` | INTEGER | NOT NULL DEFAULT 0 |
| `notes` | TEXT | NULLABLE |
| `template_diet_id` | INTEGER | NULLABLE → `diets(id)` SET_NULL |

**PK:** `(user_id, date)`  
**Indexes:** `(user_id)`, `(template_diet_id)`

> `template_diet_id` is informational only — it records which diet was used as a template when the day was planned. Actual slot content comes from `planned_slots`.

---

### `planned_slots`  ⭐ NEW
The source of truth for what is planned for each slot on each day. One row per slot per day.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `user_id` | INTEGER | NOT NULL |
| `date` | INTEGER | NOT NULL (epoch ms) |
| `slot_type` | TEXT | NOT NULL |
| `meal_id` | INTEGER | NULLABLE → `meals(id)` SET_NULL (NULL = ad-hoc foods-only slot) |
| `source_diet_id` | INTEGER | NULLABLE (informational: which diet this meal came from; no FK) |
| `instructions` | TEXT | NULLABLE |

**PK:** `id`  
**UNIQUE:** `(user_id, date, slot_type)`  
**FK:** `(user_id, date)` → `day_plans(user_id, date)` CASCADE  
**Indexes:** `(user_id, date)`, `(meal_id)`

---

### `planned_slot_foods`  ⭐ NEW
Individual foods added directly to a planned slot — either on top of a meal, or as the entire slot content when `meal_id` is NULL.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `planned_slot_id` | INTEGER | NOT NULL → `planned_slots(id)` CASCADE |
| `food_id` | INTEGER | NOT NULL → `food_items(id)` CASCADE |
| `quantity` | REAL | NOT NULL |
| `unit` | TEXT | NOT NULL DEFAULT 'GRAM' |
| `notes` | TEXT | NULLABLE |

**Indexes:** `(planned_slot_id)`, `(food_id)`

---

### `daily_logs`
Records whether a slot was logged on a given day. **Drives the streak — do not modify this table without extreme care.**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `user_id` | INTEGER | NOT NULL → `users(id)` CASCADE |
| `date` | INTEGER | NOT NULL (epoch ms) |
| `slot_type` | TEXT | NOT NULL |
| `is_logged` | INTEGER | NOT NULL DEFAULT 0 |
| `notes` | TEXT | NULLABLE |
| `logged_at` | INTEGER | NULLABLE (epoch ms) |

**UNIQUE:** `(user_id, date, slot_type)`  
**Indexes:** `(user_id, date)`

---

### `logged_foods`
Actual foods consumed in a logged slot (may differ from what was planned).

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `log_id` | INTEGER | NOT NULL → `daily_logs(id)` CASCADE |
| `food_id` | INTEGER | NULLABLE → `food_items(id)` SET_NULL (SET_NULL preserves history if food deleted) |
| `quantity` | REAL | NOT NULL |
| `unit` | TEXT | NOT NULL |
| `notes` | TEXT | NULLABLE |

**Indexes:** `(log_id)`, `(food_id)`

---

### `health_metrics`
Generic type/value store for weight, glucose, blood pressure, steps, calories burned.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `user_id` | INTEGER | NOT NULL → `users(id)` CASCADE |
| `date` | INTEGER | NOT NULL (epoch ms) |
| `type` | TEXT | NOT NULL — 'WEIGHT' \| 'BLOOD_GLUCOSE' \| 'BP_SYSTOLIC' \| 'BP_DIASTOLIC' \| 'STEPS' \| 'CALORIES_BURNED' |
| `value` | REAL | NOT NULL |
| `unit` | TEXT | NOT NULL |
| `notes` | TEXT | NULLABLE |

**Indexes:** `(user_id, date)`, `(user_id, type)`

---

### `grocery_lists`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `user_id` | INTEGER | NOT NULL → `users(id)` CASCADE |
| `name` | TEXT | NOT NULL |
| `start_date` | INTEGER | NULLABLE (epoch ms) |
| `end_date` | INTEGER | NULLABLE (epoch ms) |
| `created_at` | INTEGER | NOT NULL (epoch ms) |

---

### `grocery_items`
Persisted items in a grocery list. `name` is denormalized so history survives food deletion.

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `list_id` | INTEGER | NOT NULL → `grocery_lists(id)` CASCADE |
| `food_id` | INTEGER | NULLABLE → `food_items(id)` SET_NULL |
| `name` | TEXT | NOT NULL (denormalized) |
| `quantity` | REAL | NOT NULL |
| `unit` | TEXT | NOT NULL |
| `is_bought` | INTEGER | NOT NULL DEFAULT 0 |
| `notes` | TEXT | NULLABLE |

**Indexes:** `(list_id)`

---

### `tags`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INTEGER | PK AUTOINCREMENT |
| `name` | TEXT | NOT NULL UNIQUE |

### `diet_tags`  (junction)

| Column | Type | Constraints |
|--------|------|-------------|
| `diet_id` | INTEGER | NOT NULL → `diets(id)` CASCADE |
| `tag_id` | INTEGER | NOT NULL → `tags(id)` CASCADE |

**PK:** `(diet_id, tag_id)`

---

## Slot Type Convention

`slot_type` is a free-form TEXT field. Standard values:

| Value | Display |
|-------|---------|
| `BREAKFAST` | Breakfast |
| `LUNCH` | Lunch |
| `DINNER` | Dinner |
| `NOON` | Noon snack |
| `EVENING` | Evening snack |
| `SNACK` | Snack |

Custom slots are stored as plain strings (e.g. `"Pre-workout"`). No separate table needed.

---

## Slot Resolution Algorithm

When loading what to display for a given `(user_id, date, slot_type)`:

```
1. Query planned_slots WHERE (user_id, date, slot_type)
   ├─ Found, meal_id set     → load meal's foods from meal_foods
   ├─ Found, meal_id = NULL  → slot is ad-hoc only
   └─ Not found              → fall back to day_plans.template_diet_id's diet_slots

2. In all cases: also load planned_slot_foods for this planned_slot (ad-hoc additions)

3. Combined content = meal foods + ad-hoc foods
```

---

## Tables Removed vs v26

| Removed | Reason |
|---------|--------|
| `custom_meal_slots` | Slots are now free-text strings; no separate table needed |
| `meals.user_id` | Meals are app-global; `is_system` flag is sufficient |
| `meals.slot_type` / `meals.custom_slot_id` | Slot assignment moved to `diet_slots` / `planned_slots` |
| `diets.user_id` | Diets are app-global; `is_system` flag is sufficient |

---

## Migration History

| Version | Description |
|---------|-------------|
| 1–17 | Initial schema iterations |
| 18 | Health metrics |
| 19 | Sync columns (server_id, updatedAt, syncedAt) |
| 20 | Health Connect integration |
| 21 | Tags + diet_tags |
| 22 | Grocery items |
| 23 | Favourites (isFavourite on diets) |
| 24–25 | Glycemic index data + fix |
| 26 | String → Long dates across all date columns |
| **27** | **New schema: planned_slots, planned_slot_foods, diet_slots rename, remove user_id from meals/diets, drop custom_meal_slots** |
| 28 | Data-only: deduplicate meals + diets by name (keep min id) |
| 29 | Data-only: deduplicate food_items by name (keep min id), re-point FK refs |
| **30** | **Data-only: wipe all meals + meal_food_items + diet_slots (corrupted by v28 dedup); MealSlotReseeder re-seeds from seed_data.json on next launch** |

### v30 is the stable baseline

All schema files in `android/schemas/com.mealplanplus.data.local.AppDatabase/30.json` reflect the current entity definitions. No further destructive data migrations are planned; v31+ will be additive only (new tables, new columns).

---

## Idempotency Guarantee

All seeders that populate initial data (`UserDataSeeder`, `MealSlotReseeder`, `DatabaseSeeder`) are guarded by a count check before inserting:

```kotlin
if (dao.getDietCount() > 0) return   // already seeded
```

This ensures re-running the seeder (e.g. after an app restart) never creates duplicate rows. The seeder uses `INSERT OR IGNORE` / `OnConflictStrategy.IGNORE` for food items and tags.
