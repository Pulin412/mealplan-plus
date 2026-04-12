# MealPlan+ — Android Room Database Schema

> **Current version:** 30  
> Schema files: `android/schemas/com.mealplanplus.data.local.AppDatabase/`  
> Identity hash (v29 = v30, data-only migration): `fd9f0f2df75f4bb4379c5f68e6b66c40`

---

## Migration history

| Version | Type | Change |
|---|---|---|
| 1–9 | Schema | Initial tables and early iterations |
| 10–26 | Schema | Progressive feature additions (logs, plans, health, grocery, tags) |
| 27 | Schema | Added `diet_meals` join table; added `slotType` to `planned_slots` |
| 28 | **Data** | Dedup diets + meals — seeder idempotency guard added |
| 29 | **Data** | Dedup `meal_food_items` (compound-PK tables); fix `getExistingDietNames()` |
| 30 | **Data** | Wipe corrupted `meal_food_items` / `diet_slots` / `meals`; `MealSlotReseeder` re-populates from `seed_data.json` |

> **Rule:** Never use destructive fallback (`fallbackToDestructiveMigration`). Always add an explicit `Migration(x, y)` in `DatabaseModule.kt`. Schema files must be exported and committed.

---

## Tables (18)

### `users`
Primary store for the authenticated user profile.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `email` | TEXT | NOT NULL · UNIQUE |
| `passwordHash` | TEXT | NOT NULL |
| `displayName` | TEXT | |
| `photoUrl` | TEXT | |
| `age` | INTEGER | |
| `contact` | TEXT | |
| `weightKg` | REAL | |
| `heightCm` | REAL | |
| `gender` | TEXT | |
| `activityLevel` | TEXT | |
| `targetCalories` | INTEGER | |
| `goalType` | TEXT | |
| `createdAt` | INTEGER | NOT NULL |
| `updatedAt` | INTEGER | NOT NULL |

---

### `food_items`
Master catalogue of all foods. Seeded from `seed_data.json` + user-added.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `name` | TEXT | NOT NULL |
| `brand` | TEXT | |
| `barcode` | TEXT | |
| `caloriesPer100` | REAL | NOT NULL |
| `proteinPer100` | REAL | NOT NULL |
| `carbsPer100` | REAL | NOT NULL |
| `fatPer100` | REAL | NOT NULL |
| `gramsPerPiece` | REAL | |
| `gramsPerCup` | REAL | |
| `gramsPerTbsp` | REAL | |
| `gramsPerTsp` | REAL | |
| `glycemicIndex` | INTEGER | |
| `preferredUnit` | TEXT | |
| `isFavorite` | INTEGER | NOT NULL (0/1) |
| `lastUsed` | INTEGER | |
| `createdAt` | INTEGER | NOT NULL |
| `isSystemFood` | INTEGER | NOT NULL (0/1) |
| `serverId` | TEXT | sync: backend UUID |
| `updatedAt` | INTEGER | NOT NULL |
| `syncedAt` | INTEGER | |

---

### `meals`
Named collections of food items. Each meal belongs to one or more diet slots.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `name` | TEXT | NOT NULL |
| `description` | TEXT | |
| `isSystem` | INTEGER | NOT NULL (0/1) |
| `serverId` | TEXT | |
| `createdAt` | INTEGER | NOT NULL |
| `updatedAt` | INTEGER | NOT NULL |
| `syncedAt` | INTEGER | |

---

### `meal_food_items`
Join table: which foods (with quantity) make up a meal.

| Column | Type | Constraints |
|---|---|---|
| `mealId` | INTEGER | NOT NULL · FK → `meals.id` |
| `foodId` | INTEGER | NOT NULL · FK → `food_items.id` |
| `quantity` | REAL | NOT NULL |
| `unit` | TEXT | NOT NULL |
| `notes` | TEXT | |

**Composite PK:** `(mealId, foodId)`

---

### `diets`
A named day-plan composed of meal slots.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `name` | TEXT | NOT NULL |
| `description` | TEXT | |
| `createdAt` | INTEGER | NOT NULL |
| `isSystem` | INTEGER | NOT NULL (0/1) |
| `serverId` | TEXT | |
| `updatedAt` | INTEGER | NOT NULL |
| `syncedAt` | INTEGER | |
| `isFavourite` | INTEGER | NOT NULL (0/1) |

---

### `diet_slots`
Which meal fills each slot (BREAKFAST/NOON/LUNCH/EVENING/DINNER) in a diet.

| Column | Type | Constraints |
|---|---|---|
| `dietId` | INTEGER | NOT NULL · FK → `diets.id` |
| `slotType` | TEXT | NOT NULL |
| `mealId` | INTEGER | FK → `meals.id` (nullable) |
| `instructions` | TEXT | |

**Composite PK:** `(dietId, slotType)`

---

### `daily_logs`
One row per user per day — the anchor for all logged food on that date.

| Column | Type | Constraints |
|---|---|---|
| `userId` | INTEGER | NOT NULL · FK → `users.id` |
| `date` | INTEGER | NOT NULL (epoch ms) |
| `plannedDietId` | INTEGER | FK → `diets.id` |
| `notes` | TEXT | |
| `createdAt` | INTEGER | NOT NULL |

**Composite PK:** `(userId, date)`

---

### `logged_foods`
Individual food items logged by the user within a day+slot.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `userId` | INTEGER | NOT NULL · FK → `daily_logs(userId, logDate)` |
| `logDate` | INTEGER | NOT NULL |
| `foodId` | INTEGER | NOT NULL · FK → `food_items.id` |
| `quantity` | REAL | NOT NULL |
| `unit` | TEXT | NOT NULL |
| `slotType` | TEXT | NOT NULL |
| `timestamp` | INTEGER | |
| `notes` | TEXT | |

---

### `plans`
Calendar: which diet is assigned to which day.

| Column | Type | Constraints |
|---|---|---|
| `userId` | INTEGER | NOT NULL · FK → `users.id` |
| `date` | INTEGER | NOT NULL |
| `dietId` | INTEGER | FK → `diets.id` |
| `notes` | TEXT | |
| `isCompleted` | INTEGER | NOT NULL (0/1) — day completion flag |

**Composite PK:** `(userId, date)`

---

### `planned_slots`
Per-day, per-slot overrides (when the diet's default meal is swapped for a specific day).

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `userId` | INTEGER | NOT NULL · FK → `users.id` |
| `date` | INTEGER | NOT NULL |
| `slotType` | TEXT | NOT NULL |
| `mealId` | INTEGER | FK → `meals.id` |
| `sourceDietId` | INTEGER | |
| `instructions` | TEXT | |

---

### `planned_slot_foods`
Individual foods added to a planned slot (used when logging food outside of a meal).

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `plannedSlotId` | INTEGER | NOT NULL · FK → `planned_slots.id` |
| `foodId` | INTEGER | NOT NULL · FK → `food_items.id` |
| `quantity` | REAL | NOT NULL |
| `unit` | TEXT | NOT NULL |
| `notes` | TEXT | |

---

### `health_metrics`
Weight, GI score, energy level, sleep, and custom metrics.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `userId` | INTEGER | NOT NULL · FK → `users.id` |
| `date` | INTEGER | NOT NULL |
| `timestamp` | INTEGER | NOT NULL |
| `metricType` | TEXT | (WEIGHT, GI_SCORE, ENERGY, SLEEP, CUSTOM) |
| `customTypeId` | INTEGER | FK → `custom_metric_types.id` |
| `value` | REAL | NOT NULL |
| `secondaryValue` | REAL | |
| `subType` | TEXT | |
| `notes` | TEXT | |
| `serverId` | TEXT | |
| `updatedAt` | INTEGER | NOT NULL |
| `syncedAt` | INTEGER | |

---

### `custom_metric_types`
User-defined health metric definitions.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `userId` | INTEGER | NOT NULL · FK → `users.id` |
| `name` | TEXT | NOT NULL |
| `unit` | TEXT | NOT NULL |
| `minValue` | REAL | |
| `maxValue` | REAL | |
| `isActive` | INTEGER | NOT NULL (0/1) |

---

### `tags`
Colour-coded labels that can be applied to diets or foods.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `userId` | INTEGER | NOT NULL · FK → `users.id` |
| `name` | TEXT | NOT NULL |
| `color` | TEXT | |
| `createdAt` | INTEGER | NOT NULL |

**Index:** `UNIQUE(userId, name)`

---

### `diet_tags`
Many-to-many: diets ↔ tags.

| Column | Type | Constraints |
|---|---|---|
| `dietId` | INTEGER | NOT NULL · FK → `diets.id` |
| `tagId` | INTEGER | NOT NULL · FK → `tags.id` |

**Composite PK:** `(dietId, tagId)`

---

### `food_tag_cross_refs`
Many-to-many: foods ↔ tags.

| Column | Type | Constraints |
|---|---|---|
| `foodId` | INTEGER | NOT NULL · FK → `food_items.id` |
| `tagId` | INTEGER | NOT NULL · FK → `tags.id` |

**Composite PK:** `(foodId, tagId)`

---

### `grocery_lists`
Named grocery lists, optionally tied to a date range.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `userId` | INTEGER | NOT NULL · FK → `users.id` |
| `name` | TEXT | NOT NULL |
| `startDate` | INTEGER | |
| `endDate` | INTEGER | |
| `createdAt` | INTEGER | NOT NULL |
| `updatedAt` | INTEGER | NOT NULL |
| `serverId` | TEXT | |
| `syncedAt` | INTEGER | |

---

### `grocery_items`
Line items in a grocery list.

| Column | Type | Constraints |
|---|---|---|
| `id` | INTEGER | PK NOT NULL |
| `listId` | INTEGER | NOT NULL · FK → `grocery_lists.id` |
| `foodId` | INTEGER | FK → `food_items.id` (null for custom items) |
| `customName` | TEXT | |
| `quantity` | REAL | NOT NULL |
| `unit` | TEXT | NOT NULL |
| `isChecked` | INTEGER | NOT NULL (0/1) |
| `sortOrder` | INTEGER | NOT NULL |
| `category` | TEXT | |

---

## Entity relationship overview

```
users
 ├── daily_logs (userId+date PK)
 │     └── logged_foods (userId+logDate FK)
 ├── plans (userId+date PK)
 │     └── planned_slots
 │           └── planned_slot_foods
 ├── health_metrics
 ├── custom_metric_types
 ├── tags
 │     ├── diet_tags  ──→ diets
 │     └── food_tag_cross_refs ──→ food_items
 └── grocery_lists
       └── grocery_items ──→ food_items

diets
 └── diet_slots ──→ meals
                      └── meal_food_items ──→ food_items
```

---

## Sync metadata columns

All syncable entities carry three columns for Phase 1 backend sync:

| Column | Meaning |
|---|---|
| `serverId` | UUID assigned by the backend on first push |
| `updatedAt` | Last local write timestamp (epoch ms) |
| `syncedAt` | Timestamp of last successful sync to backend |

**Conflict resolution:** if `updatedAt (client) > updatedAt (server)` → client wins, else server wins.

Tables with sync columns: `food_items`, `meals`, `diets`, `health_metrics`, `grocery_lists`.

---

## Adding a new migration

1. Increment `version` in `AppDatabase.kt`
2. Add `MIGRATION_X_Y` in `DatabaseModule.kt`
3. Run `./gradlew :android:kspDebugKotlin` to export the new schema JSON
4. Commit the new `android/schemas/.../Y.json` alongside the migration code
5. Update the migration history table above
