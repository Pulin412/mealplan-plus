#!/usr/bin/env python3
"""
Seed the LOCAL Docker postgres from an Android Room SQLite database.
ONLY targets localhost:5434 — never touches Neon.tech (production).

Usage:
    python3 scripts/seed_local_db.py <sqlite_db_path> <firebase_uid>

Getting the SQLite file (choose one):
    ADB (USB debugging on):
        adb exec-out run-as com.mealplanplus cat databases/mealplan_database > /tmp/mealplan.db
    Android Studio:
        View > Tool Windows > Device Explorer
        data/data/com.mealplanplus/databases/mealplan_database

Getting your Firebase UID:
    1. Open https://mealplan-plus.vercel.app in Chrome and sign in
    2. DevTools > Application > IndexedDB > firebaseLocalStorageDb > firebaseLocalStorage
    3. Find the entry — the "uid" field is your Firebase UID

Prerequisites:
    pip install psycopg2-binary
    Docker stack must be running: docker compose up -d
"""
import sys, sqlite3, uuid, argparse
from datetime import datetime, timezone

try:
    import psycopg2
except ImportError:
    sys.exit("Missing dependency: pip install psycopg2-binary")

PG = dict(host="localhost", port=5434, dbname="mealplanplus",
          user="mealplan", password="mealplan_dev")

SLOT_MAP = {
    "BREAKFAST": "BREAKFAST", "LUNCH": "LUNCH", "DINNER": "DINNER",
    "SNACK": "SNACK", "PRE_WORKOUT": "PRE_WORKOUT", "POST_WORKOUT": "POST_WORKOUT",
    "NOON": "SNACK",
}


def epoch_ms_to_ts(ms):
    return datetime.fromtimestamp((ms or 0) / 1000.0, tz=timezone.utc)


def main():
    parser = argparse.ArgumentParser(description="Seed local postgres from Android SQLite DB")
    parser.add_argument("sqlite_db", help="Path to mealplan_database SQLite file")
    parser.add_argument("firebase_uid", help="Your Firebase UID")
    args = parser.parse_args()

    uid = args.firebase_uid
    now = datetime.now(timezone.utc)

    sc = sqlite3.connect(args.sqlite_db)
    sc.row_factory = sqlite3.Row

    print(f"Connecting to local postgres at localhost:{PG['port']}...")
    pg = psycopg2.connect(**PG)
    cur = pg.cursor()

    # Confirm we're on local — Neon hostnames contain ".neon.tech"
    cur.execute("SELECT inet_server_addr(), current_database()")
    addr, db = cur.fetchone()
    print(f"Connected: {addr or 'local'}/{db}")

    # ── Clear all user data (FK-safe order) ───────────────────────────────────
    print("Clearing existing user data from local postgres...")
    cur.execute("DELETE FROM diet_meals WHERE diet_id IN (SELECT id FROM diets WHERE firebase_uid=%s)", (uid,))
    cur.execute("DELETE FROM meal_food_items WHERE meal_id IN (SELECT id FROM meals WHERE firebase_uid=%s)", (uid,))
    cur.execute("DELETE FROM food_user_prefs WHERE firebase_uid=%s", (uid,))
    cur.execute("DELETE FROM logged_foods WHERE daily_log_id IN (SELECT id FROM daily_logs WHERE firebase_uid=%s)", (uid,))
    cur.execute("DELETE FROM daily_logs WHERE firebase_uid=%s", (uid,))
    cur.execute("DELETE FROM health_metrics WHERE firebase_uid=%s", (uid,))
    cur.execute("DELETE FROM grocery_items WHERE grocery_list_id IN "
                "(SELECT id FROM grocery_lists WHERE firebase_uid=%s)", (uid,))
    cur.execute("DELETE FROM grocery_lists WHERE firebase_uid=%s", (uid,))
    cur.execute("DELETE FROM diets WHERE firebase_uid=%s", (uid,))
    cur.execute("DELETE FROM meals WHERE firebase_uid=%s", (uid,))
    cur.execute("DELETE FROM foods WHERE firebase_uid=%s", (uid,))

    # ── Foods ─────────────────────────────────────────────────────────────────
    # sqlite_food_id_map: SQLite local int ID → postgres bigint ID
    sqlite_food_id_map = {}

    all_food_rows = sc.execute("""
        SELECT id, name, brand, barcode, caloriesPer100, proteinPer100, carbsPer100,
               fatPer100, gramsPerPiece, gramsPerCup, gramsPerTbsp, gramsPerTsp,
               glycemicIndex, isFavorite, isSystemFood, serverId, updatedAt
        FROM food_items
    """).fetchall()

    # Clear existing system foods too (safe — local only)
    cur.execute("DELETE FROM food_user_prefs")
    cur.execute("DELETE FROM meal_food_items")  # already cleared user meals above; catches orphans
    cur.execute("DELETE FROM foods WHERE is_system_food=TRUE")

    print(f"Inserting {len(all_food_rows)} foods (user + system)...")
    for r in all_food_rows:
        ts = epoch_ms_to_ts(r["updatedAt"])
        is_sys = bool(r["isSystemFood"])
        food_uid = None if is_sys else uid
        server_id = r["serverId"] if r["serverId"] else str(uuid.uuid4())
        cur.execute("""
            INSERT INTO foods (firebase_uid, name, brand, barcode,
                calories_per100, protein_per100, carbs_per100, fat_per100,
                grams_per_piece, grams_per_cup, grams_per_tbsp, grams_per_tsp,
                glycemic_index, is_system_food, is_favorite,
                created_at, updated_at, server_id)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            ON CONFLICT (server_id) DO UPDATE SET name=EXCLUDED.name RETURNING id
        """, (food_uid, r["name"], r["brand"], r["barcode"],
              r["caloriesPer100"], r["proteinPer100"], r["carbsPer100"], r["fatPer100"],
              r["gramsPerPiece"], r["gramsPerCup"], r["gramsPerTbsp"], r["gramsPerTsp"],
              r["glycemicIndex"], is_sys, bool(r["isFavorite"]),
              ts, ts, server_id))
        sqlite_food_id_map[r["id"]] = cur.fetchone()[0]

    sys_count = sum(1 for r in all_food_rows if r["isSystemFood"])
    user_count = len(all_food_rows) - sys_count
    print(f"  {user_count} user foods, {sys_count} system foods inserted")

    # ── Meals + food items ────────────────────────────────────────────────────
    print("Inserting meals and food items...")
    sqlite_meal_id_map = {}
    meals_n = items_n = 0

    meal_rows = sc.execute(
        "SELECT id, name, updatedAt FROM meals WHERE isSystem = 0"
    ).fetchall()

    for m in meal_rows:
        ts = epoch_ms_to_ts(m["updatedAt"])
        cur.execute("""
            INSERT INTO meals (firebase_uid, name, created_at, updated_at, server_id)
            VALUES (%s,%s,%s,%s,%s) RETURNING id
        """, (uid, m["name"], ts, ts, str(uuid.uuid4())))
        pg_meal_id = cur.fetchone()[0]
        sqlite_meal_id_map[m["id"]] = pg_meal_id
        meals_n += 1

        for item in sc.execute(
            "SELECT foodId, quantity, unit, notes FROM meal_food_items WHERE mealId=?",
            (m["id"],)
        ).fetchall():
            pg_food_id = sqlite_food_id_map.get(item["foodId"])
            if pg_food_id is None:
                continue
            cur.execute("""
                INSERT INTO meal_food_items (meal_id, food_id, quantity, unit, notes)
                VALUES (%s,%s,%s,%s,%s)
            """, (pg_meal_id, pg_food_id, item["quantity"], item["unit"], item["notes"]))
            items_n += 1

    print(f"  {meals_n} meals, {items_n} food items inserted")

    # ── Diets + diet-meal links ───────────────────────────────────────────────
    print("Inserting diets and diet-meal links...")
    diets_n = links_n = 0

    diet_rows = sc.execute(
        "SELECT id, name, description, updatedAt FROM diets WHERE isSystem = 0"
    ).fetchall()

    for d in diet_rows:
        ts = epoch_ms_to_ts(d["updatedAt"])
        cur.execute("""
            INSERT INTO diets (firebase_uid, name, description, created_at, updated_at, server_id)
            VALUES (%s,%s,%s,%s,%s,%s) RETURNING id
        """, (uid, d["name"], d["description"], ts, ts, str(uuid.uuid4())))
        pg_diet_id = cur.fetchone()[0]
        diets_n += 1

        for slot in sc.execute(
            "SELECT slotType, mealId, instructions FROM diet_slots WHERE dietId=?",
            (d["id"],)
        ).fetchall():
            pg_meal_id = sqlite_meal_id_map.get(slot["mealId"])
            if pg_meal_id is None:
                continue
            slot_name = SLOT_MAP.get(slot["slotType"], slot["slotType"])
            cur.execute("""
                INSERT INTO diet_meals (diet_id, meal_id, day_of_week, slot, instructions)
                VALUES (%s,%s,%s,%s,%s)
            """, (pg_diet_id, pg_meal_id, 0, slot_name, slot["instructions"]))
            links_n += 1

    print(f"  {diets_n} diets, {links_n} diet-meal links inserted")

    pg.commit()
    cur.close()
    pg.close()
    sc.close()
    print("\nDone. Open http://localhost:3000 to see your data.")


if __name__ == "__main__":
    main()
