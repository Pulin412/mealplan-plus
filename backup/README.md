# MealPlan+ Database Backup

**Taken:** 2026-04-11  
**DB Version:** 26 (before schema redesign)  
**Branch:** `feature/db-optimisation`

## Contents

| File | Size | Purpose |
|------|------|---------|
| `mealplan_database_backup.db` | 560K | Raw SQLite binary — can be restored directly via ADB |
| `mealplan_dump.sql` | 429K | Full SQL text dump — human-readable, every CREATE TABLE + INSERT |
| `mealplan_data_export.json` | 941K | All tables as JSON — schema-agnostic, for mapping to new structure |

## Row Counts at Backup Time

| Table | Rows |
|-------|------|
| users | 3 |
| food_items | 108 |
| meals | 652 |
| meal_food_items | 3,066 |
| diets | 114 |
| diet_meals | 651 |
| plans | 48 |
| daily_logs | 42 |
| logged_foods | 947 |
| health_metrics | 14 |
| grocery_lists | 1 |
| grocery_items | 38 |
| tags | 12 |
| diet_tags | 141 |
| custom_meal_slots | 0 |
| custom_metric_types | 0 |

## How to Restore (if needed)

### Option A — Restore raw SQLite file (complete restore)
```bash
# Push backup to device tmp
adb push mealplan_database_backup.db /data/local/tmp/restore.db

# Copy into app's data directory (app must be installed)
adb shell "run-as com.mealplanplus cp /data/local/tmp/restore.db /data/data/com.mealplanplus/databases/mealplan_database"
```

### Option B — Use the JSON export (for mapping to new schema)
The `mealplan_data_export.json` file contains every table as a named array.  
Each row is a flat object with column names as keys.  
Dates stored as `Long` (epoch milliseconds); divide by 86400000 to get epoch days.

### Option C — Replay the SQL dump
```bash
sqlite3 new_database.db < mealplan_dump.sql
```

## Key Relationships (v26 schema)

```
users → food_items (userId, nullable — system foods have userId of a user)
users → meals (userId)
users → diets (userId)
users → plans (userId + date composite PK)
users → daily_logs (userId + date + slotType composite)
users → health_metrics (userId)
users → grocery_lists (userId)
meals → meal_food_items → food_items
diets → diet_meals → meals (per slotType)
plans → diets (dietId, SET_NULL on delete)
daily_logs → logged_foods → food_items
grocery_lists → grocery_items
diets ↔ tags (via diet_tags)
food_items ↔ tags (via food_tag_cross_refs)
```

## Delete this folder after

Once the new schema (v27+) is live, tested, and data is verified correct, this folder and its contents can be safely deleted.
