# Database Schema

**Database:** `mealplan_database` (Room / SQLite)
**Current version:** 21
**Location on device:** `/data/data/com.mealplanplus/databases/`

---

## Entity Relationship Diagram

```
users
 └──< health_metrics
 └──< daily_logs
       └──< logged_foods
 └──< plans
 └──< meals
       └──< meal_food_items >── food_items
 └──< diets
       └──< diet_meals >── meals
       └──< diet_tags >── tags
 └──< grocery_lists
       └──< grocery_items >── food_items
 └──< custom_meal_slots
 └──< custom_metric_types
 └──< food_tag_cross_refs >── tags
```

---

## Entities

### `users`
User profile and authentication record.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | Auto-generated |
| `email` | TEXT UNIQUE | Lowercase, trimmed |
| `passwordHash` | TEXT | Empty for Firebase accounts |
| `displayName` | TEXT? | |
| `bio` | TEXT? | |
| `age` | INT? | |
| `weight` | FLOAT? | kg |
| `height` | FLOAT? | cm |
| `activityLevel` | TEXT? | Enum: SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE |
| `goal` | TEXT? | Enum: LOSE, MAINTAIN, GAIN |
| `photoUrl` | TEXT? | Google profile photo URL |
| `createdAt` | LONG | Unix ms |
| `updatedAt` | LONG | Unix ms |

---

### `food_items`
Nutrition database. All macros stored per 100g.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `name` | TEXT | |
| `calories` | FLOAT | per 100g |
| `protein` | FLOAT | g per 100g |
| `carbs` | FLOAT | g per 100g |
| `fat` | FLOAT | g per 100g |
| `fiber` | FLOAT? | g per 100g |
| `sugar` | FLOAT? | g per 100g |
| `servingSize` | FLOAT? | Default serving amount |
| `servingUnit` | TEXT? | g/ml/piece/cup/tbsp/tsp/slice/scoop |
| `category` | TEXT? | For grocery grouping |
| `brand` | TEXT? | |
| `barcode` | TEXT? | EAN/UPC |
| `isFavorite` | BOOL | |
| `isCustom` | BOOL | false = seeded/imported |
| `lastUsedAt` | LONG? | For recent foods |
| `serverId` | TEXT? | Cloud sync ID |
| `syncedAt` | LONG? | Cloud sync timestamp |
| `createdAt` | LONG | |

---

### `meals`
Reusable meal templates.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `name` | TEXT | |
| `description` | TEXT? | |
| `serverId` | TEXT? | |
| `syncedAt` | LONG? | |
| `createdAt` | LONG | |
| `updatedAt` | LONG | |

---

### `meal_food_items`
Junction: meal ↔ food with quantity and unit.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `mealId` | LONG FK | → meals |
| `foodItemId` | LONG FK | → food_items |
| `quantity` | FLOAT | Amount in `unit` |
| `unit` | TEXT | g/ml/piece/cup/… |
| `order` | INT | Display order |

---

### `diets`
Full-day meal plan templates.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `name` | TEXT | |
| `description` | TEXT? | |
| `isSystem` | BOOL | System diets are read-only |
| `serverId` | TEXT? | |
| `syncedAt` | LONG? | |
| `createdAt` | LONG | |
| `updatedAt` | LONG | |

---

### `diet_meals`
Junction: diet ↔ meal, assigned to a slot.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `dietId` | LONG FK | → diets |
| `mealId` | LONG FK | → meals |
| `slotType` | TEXT | BREAKFAST/MORNING_SNACK/LUNCH/EVENING_SNACK/DINNER |
| `instructions` | TEXT? | Prep notes |
| `order` | INT | |

---

### `tags`
Coloured labels for diets and foods.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `name` | TEXT | UNIQUE per user |
| `color` | TEXT? | Hex color |
| `createdAt` | LONG | |

---

### `diet_tags` / `food_tag_cross_refs`
Many-to-many junction tables.

| Column | Type |
|--------|------|
| `dietId` / `foodId` | LONG FK |
| `tagId` | LONG FK |

---

### `daily_logs`
One record per user per date.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `date` | TEXT | ISO-8601 (yyyy-MM-dd) |
| `plannedDietId` | LONG? | Diet assigned for this day |
| `notes` | TEXT? | |
| `createdAt` | LONG | |

Unique constraint: `(userId, date)`

---

### `logged_foods`
Individual food entries within a daily log.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `logId` | LONG FK | → daily_logs |
| `foodItemId` | LONG FK | → food_items |
| `quantity` | FLOAT | |
| `unit` | TEXT | |
| `slotType` | TEXT | BREAKFAST/… or custom slot name |
| `loggedAt` | LONG | Unix ms timestamp |

---

### `plans`
Diet plan assignment for a specific date.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `date` | TEXT | yyyy-MM-dd |
| `dietId` | LONG FK | → diets |
| `isCompleted` | BOOL | |
| `createdAt` | LONG | |

Unique constraint: `(userId, date)`

---

### `health_metrics`
Timestamped health measurements.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `metricTypeId` | LONG? FK | → custom_metric_types (null = built-in) |
| `type` | TEXT | WEIGHT/BLOOD_GLUCOSE/BLOOD_PRESSURE/CUSTOM |
| `value` | FLOAT | Primary measurement |
| `value2` | FLOAT? | Diastolic (blood pressure) |
| `subType` | TEXT? | FASTING/POST_MEAL/RANDOM (glucose) |
| `unit` | TEXT | kg/mmol/mgdl/mmHg/% |
| `recordedAt` | LONG | Unix ms |
| `notes` | TEXT? | |

---

### `custom_metric_types`
User-defined health metric types.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `name` | TEXT | |
| `unit` | TEXT | |
| `minValue` | FLOAT? | Normal range lower bound |
| `maxValue` | FLOAT? | Normal range upper bound |
| `createdAt` | LONG | |

---

### `grocery_lists`

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `name` | TEXT | |
| `startDate` | TEXT? | yyyy-MM-dd |
| `endDate` | TEXT? | yyyy-MM-dd |
| `serverId` | TEXT? | |
| `syncedAt` | LONG? | |
| `createdAt` | LONG | |

---

### `grocery_items`

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `listId` | LONG FK | → grocery_lists |
| `foodItemId` | LONG? FK | → food_items (null = manual item) |
| `name` | TEXT | |
| `quantity` | FLOAT | |
| `unit` | TEXT | |
| `category` | TEXT | For grouping |
| `isChecked` | BOOL | Shopping cart status |
| `order` | INT | |

---

### `custom_meal_slots`
Per-date user-defined meal slots.

| Column | Type | Notes |
|--------|------|-------|
| `id` | LONG PK | |
| `userId` | LONG FK | |
| `date` | TEXT | yyyy-MM-dd |
| `name` | TEXT | |
| `order` | INT | Display order |

---

## DAOs

| DAO | Key operations |
|-----|---------------|
| `FoodDao` | CRUD, search by name, barcode lookup, favorites, recent |
| `MealDao` | CRUD for meals and meal-food items, `MealWithFoods` query |
| `DietDao` | CRUD, `DietFullSummary` (aggregate macros), batch food/slot queries |
| `DailyLogDao` | CRUD, `getLogWithFoods(date)` Flow for reactive log screen |
| `PlanDao` | Assign/remove diet to date, `PlanWithDietName` for calendar |
| `HealthMetricDao` | CRUD, date-range queries, custom types management |
| `UserDao` | CRUD, `getUserByEmail`, `getUserByIdSync` |
| `TagDao` | CRUD, `getTagsForDiets(List<Long>)` batch query |
| `GroceryDao` | CRUD for lists and items, `GroceryListWithItems` |
| `CustomMealSlotDao` | CRUD per date |

---

## Migrations

The database has 21 schema migrations (v1 → v21). Key milestones:

| Version | Change |
|---------|--------|
| v1–v9 | Initial schema, food/meal/diet/log tables |
| v10 | Added `userId` column to all tables for multi-user support |
| v11 | Tag system: `tags`, `diet_tags` tables |
| v15 | Health metrics with custom types |
| v17 | Grocery lists and items |
| v19 | Custom meal slots |
| v20 | `food_tag_cross_refs` table |
| v21 | Sync columns (`serverId`, `syncedAt`) on food, meal, diet, grocery |

**Important:** Destructive migration fallback is disabled. If a migration fails, the app crashes rather than silently wiping user data.
