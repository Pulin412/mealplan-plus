#!/usr/bin/env python3
"""
Seeds the local PostgreSQL DB with foods (from SQLite backup) and
diets/meals (from seed_data.json). Run once after starting the local stack.
"""
import sqlite3, json, uuid
from datetime import datetime, timezone
import psycopg2

FIREBASE_UID = "iUMTqIa65jV0Om25TAae8yJdrVz1"
SQLITE_DB    = "/Users/pulin@backbase.com/personal/mealplan-plus/backup/mealplan_database_backup.db"
SEED_JSON    = "/Users/pulin@backbase.com/personal/mealplan-plus/data/seed_data.json"

PG = dict(host="localhost", port=5434, dbname="mealplanplus",
          user="mealplan", password="mealplan_dev")

now = datetime.now(timezone.utc)

def gen_uuid():
    return str(uuid.uuid4())

# ── Connect ───────────────────────────────────────────────────────────────────
sqlite_conn = sqlite3.connect(SQLITE_DB)
sqlite_conn.row_factory = sqlite3.Row
pg = psycopg2.connect(**PG)
cur = pg.cursor()

# ── 1. Clear existing user data (idempotent re-runs) ──────────────────────────
print("Clearing existing data for this UID...")
cur.execute("DELETE FROM diet_meals WHERE diet_id IN (SELECT id FROM diets WHERE firebase_uid=%s)", (FIREBASE_UID,))
cur.execute("DELETE FROM meal_food_items WHERE meal_id IN (SELECT id FROM meals WHERE firebase_uid=%s)", (FIREBASE_UID,))
cur.execute("DELETE FROM diets WHERE firebase_uid=%s", (FIREBASE_UID,))
cur.execute("DELETE FROM meals WHERE firebase_uid=%s", (FIREBASE_UID,))
cur.execute("DELETE FROM foods WHERE firebase_uid=%s OR (firebase_uid IS NULL AND is_system_food=TRUE)", (FIREBASE_UID,))

# ── 2. Insert foods from SQLite ───────────────────────────────────────────────
print("Inserting foods...")
rows = sqlite_conn.execute("""
    SELECT name, brand, barcode, caloriesPer100, proteinPer100, carbsPer100,
           fatPer100, gramsPerPiece, gramsPerCup, gramsPerTbsp, gramsPerTsp,
           glycemicIndex, isSystemFood, isFavorite
    FROM food_items
""").fetchall()

food_name_to_id = {}
for r in rows:
    uid = FIREBASE_UID if not r["isSystemFood"] else None
    cur.execute("""
        INSERT INTO foods (firebase_uid, name, brand, barcode,
            calories_per100, protein_per100, carbs_per100, fat_per100,
            grams_per_piece, grams_per_cup, grams_per_tbsp, grams_per_tsp,
            glycemic_index, is_system_food, created_at, updated_at, server_id)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
        RETURNING id
    """, (uid, r["name"], r["brand"], r["barcode"],
          r["caloriesPer100"], r["proteinPer100"], r["carbsPer100"], r["fatPer100"],
          r["gramsPerPiece"], r["gramsPerCup"], r["gramsPerTbsp"], r["gramsPerTsp"],
          r["glycemicIndex"], bool(r["isSystemFood"]),
          now, now, gen_uuid()))
    food_name_to_id[r["name"]] = cur.fetchone()[0]

print(f"  Inserted {len(food_name_to_id)} foods")

# ── 3. Insert meals + diets from seed_data.json ───────────────────────────────
with open(SEED_JSON) as f:
    seed = json.load(f)

SLOT_MAP = {
    "BREAKFAST": "Breakfast", "LUNCH": "Lunch", "DINNER": "Dinner",
    "NOON": "Snack", "SNACK": "Snack", "PRE_WORKOUT": "Pre-Workout",
    "POST_WORKOUT": "Post-Workout",
}
UNIT_MAP = {"g": "GRAM", "piece": "PIECE", "cup": "CUP",
            "tbsp": "TBSP", "tsp": "TSP", "ml": "ML"}

print("Inserting diets and meals...")
diets_inserted = 0
meals_inserted = 0

for diet_data in seed.get("diets", []):
    diet_meal_links = []

    for slot_key, meal_data in diet_data.get("meals", {}).items():
        slot = SLOT_MAP.get(slot_key, slot_key.capitalize())
        meal_name = meal_data.get("name", f"{diet_data['name']} {slot}")

        cur.execute("""
            INSERT INTO meals (firebase_uid, name, slot, created_at, updated_at, server_id)
            VALUES (%s,%s,%s,%s,%s,%s) RETURNING id
        """, (FIREBASE_UID, meal_name, slot, now, now, gen_uuid()))
        meal_id = cur.fetchone()[0]
        meals_inserted += 1

        for item in meal_data.get("items", []):
            food_id = food_name_to_id.get(item["food"])
            if food_id is None:
                print(f"  WARN: food not found: {item['food']}")
                continue
            unit = UNIT_MAP.get(item.get("unit", "g"), "GRAM")
            cur.execute("""
                INSERT INTO meal_food_items (meal_id, food_id, quantity, unit)
                VALUES (%s,%s,%s,%s)
            """, (meal_id, food_id, item["quantity"], unit))

        diet_meal_links.append((meal_id, slot))

    cur.execute("""
        INSERT INTO diets (firebase_uid, name, description, created_at, updated_at, server_id)
        VALUES (%s,%s,%s,%s,%s,%s) RETURNING id
    """, (FIREBASE_UID, diet_data["name"], diet_data.get("description"), now, now, gen_uuid()))
    diet_id = cur.fetchone()[0]
    diets_inserted += 1

    for idx, (meal_id, slot) in enumerate(diet_meal_links):
        cur.execute("""
            INSERT INTO diet_meals (diet_id, meal_id, day_of_week, slot)
            VALUES (%s,%s,%s,%s)
        """, (diet_id, meal_id, idx % 7, slot))

print(f"  Inserted {diets_inserted} diets, {meals_inserted} meals")

pg.commit()
cur.close()
pg.close()
sqlite_conn.close()
print("Done! Refresh the webapp to see the data.")
