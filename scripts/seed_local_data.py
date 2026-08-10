#!/usr/bin/env python3
"""
Seed the running LOCAL backend (in-memory H2 via bootRun) with a full demo dataset for the local
dev account, so every screen has data: foods, meals, diets, exercise tags/exercises, workout
templates, day plans, completed sessions (WITH per-exercise + workout notes, so "Copy last" shows a
note), health trends, and a grouped meal logged to today ("Added today").

Talks to the REST API, so it works for android or webapp — whichever signs in as the dev account.
Mints a login-able account via the Firebase Web API key: set FIREBASE_WEB_API_KEY (+ optional
DEV_EMAIL / DEV_PASSWORD, default dev@mealplan.test / mealplan123); sign into the app with those.

Modes:
  seed_local_data.py               # mint the dev account and seed everything
  seed_local_data.py --print-uid   # just print the dev account's Firebase UID (used by local-up.sh)

Env: API_BASE (default http://localhost:8080), FIREBASE_WEB_API_KEY, DEV_EMAIL, DEV_PASSWORD.
Local only — API_BASE must be localhost. Re-run safe: reuses/creates by name and skips days that
already have a session. H2 forgets on restart, so re-run after each bootRun.
"""
import json, math, os, sys, urllib.request, urllib.error
from datetime import date, timedelta

API_BASE = os.environ.get("API_BASE", "http://localhost:8080")
UPDATED_AT = "2026-07-22T00:00:00Z"  # any recent instant; last-write-wins picks the newest
IDENTITY = "https://identitytoolkit.googleapis.com/v1/accounts"


# ── auth ───────────────────────────────────────────────────────────────────────
def mint() -> tuple[str, str]:
    """Create-or-sign-in the dev account via the Firebase Web API key; returns (idToken, uid)."""
    key = os.environ.get("FIREBASE_WEB_API_KEY")
    if not key:
        sys.exit("Set FIREBASE_WEB_API_KEY (or pass --token / SEED_TOKEN). See scripts/dev-seed.env.example.")
    email = os.environ.get("DEV_EMAIL", "dev@mealplan.test")
    password = os.environ.get("DEV_PASSWORD", "mealplan123")
    payload = json.dumps({"email": email, "password": password, "returnSecureToken": True}).encode()
    last = ""
    for action in ("signUp", "signInWithPassword"):  # create first, else sign in
        try:
            req = urllib.request.Request(f"{IDENTITY}:{action}?key={key}", data=payload,
                                         headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=20) as r:
                j = json.loads(r.read())
                return j["idToken"], j["localId"]
        except urllib.error.HTTPError as e:
            last = e.read().decode(errors="replace")
    sys.exit(f"Firebase auth failed for {email}: {last[:300]}")


def api(token, method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{API_BASE}{path}", data=data, method=method, headers={
        "Content-Type": "application/json", "Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        raw = resp.read()
        return json.loads(raw) if raw else None


# ── fixed UUIDs so meals reference foods and diets reference meals + foods ──────
F = {k: f"00000000-0000-0000-0000-0000000000{n:02x}" for n, k in enumerate(
    ["chicken", "rice", "broccoli", "egg", "oats", "banana", "yogurt", "almonds",
     "oliveoil", "salmon", "sweetpot", "bread", "peanut", "milk", "apple"], start=1)}
M = {k: f"00000000-0000-0000-0000-0000000001{n:02x}" for n, k in enumerate(
    ["chicken_rice", "oatmeal", "yogurt_bowl", "salmon", "egg_toast"], start=1)}


def food(uuid, name, kcal, p, c, f, unit="GRAM", per_piece=None, per_tbsp=None):
    d = {"serverId": uuid, "name": name, "caloriesPer100": kcal, "proteinPer100": p,
         "carbsPer100": c, "fatPer100": f, "unit": unit, "verified": True, "updatedAt": UPDATED_AT}
    if per_piece is not None:
        d["gramsPerPiece"] = per_piece
    if per_tbsp is not None:
        d["gramsPerTbsp"] = per_tbsp
    return d


def item(food_uuid, qty, unit="GRAM"):
    return {"foodId": 0, "foodServerId": food_uuid, "quantity": qty, "unit": unit}


def meal(uuid, name, slots, *items):
    return {"serverId": uuid, "name": name, "slots": list(slots), "items": list(items),
            "isFavorite": False, "updatedAt": UPDATED_AT}


def dmeal(meal_uuid, slot):
    return {"mealId": 0, "mealServerId": meal_uuid, "dayOfWeek": 0, "slot": slot}


def dfood(food_uuid, slot, qty, unit="GRAM"):
    return {"foodId": 0, "foodServerId": food_uuid, "slot": slot, "quantity": qty, "unit": unit}


def diet(uuid, name, target, meals, foods):
    return {"serverId": uuid, "name": name, "targetCalories": target, "meals": meals,
            "foodItems": foods, "tagIds": [], "isFavorite": False, "updatedAt": UPDATED_AT}


FOODS = [
    food(F["chicken"], "Chicken Breast, grilled", 165, 31, 0, 3.6),
    food(F["rice"], "Brown Rice, cooked", 123, 2.7, 26, 1),
    food(F["broccoli"], "Broccoli", 34, 2.8, 7, 0.4),
    food(F["egg"], "Egg, whole", 155, 13, 1.1, 11, unit="PIECE", per_piece=50),
    food(F["oats"], "Rolled Oats", 389, 16.9, 66.3, 6.9),
    food(F["banana"], "Banana", 89, 1.1, 22.8, 0.3, unit="PIECE", per_piece=118),
    food(F["yogurt"], "Greek Yogurt, plain", 59, 10, 3.6, 0.4),
    food(F["almonds"], "Almonds", 579, 21.2, 21.6, 49.9),
    food(F["oliveoil"], "Olive Oil", 884, 0, 0, 100, unit="TBSP", per_tbsp=13.5),
    food(F["salmon"], "Salmon Fillet", 208, 20, 0, 13),
    food(F["sweetpot"], "Sweet Potato", 86, 1.6, 20.1, 0.1),
    food(F["bread"], "Whole Wheat Bread, slice", 247, 13, 41, 3.4, unit="PIECE", per_piece=40),
    food(F["peanut"], "Peanut Butter", 588, 25, 20, 50, unit="TBSP", per_tbsp=16),
    food(F["milk"], "Milk, whole", 61, 3.2, 4.8, 3.3, unit="ML"),
    food(F["apple"], "Apple", 52, 0.3, 13.8, 0.2, unit="PIECE", per_piece=182),
]

MEALS = [
    meal(M["chicken_rice"], "Grilled Chicken & Rice", ["Noon", "Dinner"],
         item(F["chicken"], 200), item(F["rice"], 150), item(F["broccoli"], 100)),
    meal(M["oatmeal"], "Protein Oatmeal", ["Breakfast"],
         item(F["oats"], 60), item(F["milk"], 200, "ML"),
         item(F["banana"], 1, "PIECE"), item(F["peanut"], 1, "TBSP")),
    meal(M["yogurt_bowl"], "Greek Yogurt Bowl", ["Breakfast", "Evening"],
         item(F["yogurt"], 200), item(F["almonds"], 30), item(F["banana"], 1, "PIECE")),
    meal(M["salmon"], "Salmon Dinner", ["Dinner"],
         item(F["salmon"], 180), item(F["sweetpot"], 200), item(F["broccoli"], 120)),
    meal(M["egg_toast"], "Egg Toast", ["Breakfast"],
         item(F["egg"], 2, "PIECE"), item(F["bread"], 2, "PIECE"), item(F["oliveoil"], 1, "TBSP")),
]

DIETS = [
    diet("00000000-0000-0000-0000-000000000201", "High Protein Day", 2200,
         [dmeal(M["oatmeal"], "Breakfast"), dmeal(M["chicken_rice"], "Noon"), dmeal(M["salmon"], "Dinner")],
         [dfood(F["almonds"], "Evening", 30)]),
    diet("00000000-0000-0000-0000-000000000202", "Balanced 1800", 1800,
         [dmeal(M["egg_toast"], "Breakfast"), dmeal(M["chicken_rice"], "Dinner")],
         [dfood(F["apple"], "Post-Lunch", 1, "PIECE")]),
    diet("00000000-0000-0000-0000-000000000203", "Vegetarian Light", 1600,
         [dmeal(M["yogurt_bowl"], "Breakfast"), dmeal(M["oatmeal"], "Dinner")],
         [dfood(F["sweetpot"], "Noon", 200), dfood(F["broccoli"], "Noon", 150)]),
]

# Exercise tags = the fixed design palette (name -> hex).
EXERCISE_TAGS = {
    "Chest": "#A74541", "Back": "#2E69B2", "Legs": "#1D7D3E", "Shoulders": "#9A5500",
    "Arms": "#8050A0", "Core": "#007D86", "Cardio": "#A74449", "Push": "#007A97",
    "Pull": "#635BB0", "Mobility": "#00805D",
}

# (name, [tag names], description)
EXERCISES = [
    ("Bench Press", ["Chest", "Push"], "Flat barbell press. Retract the shoulder blades, lower to mid-chest, press up under control."),
    ("Incline Dumbbell Press", ["Chest", "Push"], "Bench at ~30°. Press dumbbells from shoulder height, keeping wrists stacked over elbows."),
    ("Squat", ["Legs"], "Bar on upper back. Brace core, sit down and back to depth, drive through mid-foot."),
    ("Deadlift", ["Back", "Legs", "Pull"], "Hip-hinge from the floor with a neutral spine; push the floor away and lock out tall."),
    ("Overhead Press", ["Shoulders", "Push"], "Standing barbell press. Squeeze glutes, press overhead, finish with the bar over the crown."),
    ("Pull-up", ["Back", "Pull"], "Dead hang, pull the chest to the bar leading with the elbows, lower under control."),
    ("Bicep Curl", ["Arms", "Pull"], "Curl with elbows pinned to the sides; avoid swinging, squeeze at the top."),
    ("Plank", ["Core"], "Forearm plank. Keep a straight line from head to heels; brace and breathe. Reps = seconds held."),
    ("Lunges", ["Legs"], "Step forward, drop the back knee toward the floor, drive back up. Alternate legs."),
    ("Running", ["Cardio"], "Steady-state cardio. Reps = minutes; keep an easy conversational pace."),
]

# (name, [(exercise name, sets, reps)])
WORKOUTS = [
    ("Push Day", [("Bench Press", 4, 8), ("Incline Dumbbell Press", 3, 10), ("Overhead Press", 3, 8), ("Plank", 3, 45)]),
    ("Pull Day", [("Deadlift", 4, 5), ("Pull-up", 3, 8), ("Bicep Curl", 3, 12)]),
    ("Leg Day", [("Squat", 4, 8), ("Lunges", 3, 12), ("Running", 1, 1)]),
]

# Day plans: (days_from_today, diet name or None, [workout template names]).
PLANS = [
    (0, "High Protein Day", ["Push Day"]),
    (1, None, ["Pull Day"]),
    (2, "Balanced 1800", ["Leg Day"]),
]

# Completed sessions for the Logs tab + "Copy last".
#   (name, days_ago, duration, workout_note, [(exercise, exercise_note, [(reps, weightKg|None), ...])])
LOGS = [
    ("Push Day", 1, 52, "Good energy, slept well. Chest felt strong.", [
        ("Bench Press", "Elbows tucked — add 2.5 kg next time.", [(8, 60), (8, 62.5), (7, 62.5), (6, 65)]),
        ("Incline Dumbbell Press", "Keep wrists stacked; controlled tempo.", [(10, 22), (10, 22), (9, 24)]),
        ("Overhead Press", "Slight lower-back arch — brace harder.", [(8, 40), (8, 40), (7, 42.5)]),
        ("Plank", "", [(45, None), (45, None), (40, None)]),
    ]),
    ("Pull Day", 3, 47, "Grip gave out before the back did — use straps.", [
        ("Deadlift", "Reset each rep; keep the bar close.", [(5, 100), (5, 110), (5, 110), (4, 120)]),
        ("Pull-up", "Full dead hang at the bottom.", [(8, None), (7, None), (6, None)]),
        ("Bicep Curl", "", [(12, 14), (12, 14), (10, 16)]),
    ]),
    ("Leg Day", 6, 55, "Legs shaky by the end. Solid.", [
        ("Squat", "Depth was good; drive through mid-foot.", [(8, 80), (8, 85), (7, 90), (6, 90)]),
        ("Lunges", "", [(12, 20), (12, 20), (10, 24)]),
        ("Running", "Easy conversational pace.", [(1, None)]),
    ]),
]


# ── seeding ────────────────────────────────────────────────────────────────────
def seed_nutrition(token):
    api(token, "POST", "/api/v1/sync/push", {"foods": FOODS, "meals": MEALS, "diets": DIETS})
    return len(FOODS), len(MEALS), len(DIETS)


def seed_training(token):
    tag_id = {t["name"]: t["id"] for t in (api(token, "GET", "/api/v1/tags?entityType=EXERCISE") or [])}
    for name, color in EXERCISE_TAGS.items():
        if name not in tag_id:
            tag_id[name] = api(token, "POST", "/api/v1/tags",
                               {"name": name, "entityType": "EXERCISE", "color": color})["id"]

    ex_id = {e["name"]: e["id"] for e in (api(token, "GET", "/api/v1/exercises") or [])}
    for name, tags, desc in EXERCISES:
        body = {"name": name, "description": desc, "tagIds": [tag_id[t] for t in tags]}
        if name in ex_id:
            api(token, "PUT", f"/api/v1/exercises/{ex_id[name]}", {**body, "id": ex_id[name]})
        else:
            ex_id[name] = api(token, "POST", "/api/v1/exercises", body)["id"]

    wk_existing = {w["name"] for w in (api(token, "GET", "/api/v1/workout-templates") or [])}
    made = 0
    for name, items in WORKOUTS:
        if name in wk_existing:
            continue
        exercises = [{"exerciseId": ex_id[en], "orderIndex": i,
                      "sets": [{"setNumber": k, "reps": r} for k in range(s)]}
                     for i, (en, s, r) in enumerate(items)]
        api(token, "POST", "/api/v1/workout-templates", {"name": name, "exercises": exercises})
        made += 1
    wk_id = {w["name"]: w["id"] for w in (api(token, "GET", "/api/v1/workout-templates") or [])}
    return ex_id, wk_id, len(tag_id), len(ex_id), made


def seed_plans_and_logs(token, ex_id, wk_id):
    today = date.today()
    diet_id = {d["name"]: d["id"] for d in (api(token, "GET", "/api/v1/diets") or [])}

    for offset, diet_name, wnames in PLANS:
        d = (today + timedelta(offset)).isoformat()
        planned = [{"workoutTemplateId": wk_id[w], "activityName": w} for w in wnames if w in wk_id]
        api(token, "PUT", f"/api/v1/plans/{d}",
            {"date": d, "dietId": diet_id.get(diet_name), "plannedWorkouts": planned})

    def norm(v):  # Jackson may serialize LocalDate as [y, m, d]
        return f"{v[0]:04d}-{v[1]:02d}-{v[2]:02d}" if isinstance(v, list) else v
    existing = {(s.get("name"), norm(s.get("date"))) for s in (api(token, "GET", "/api/v1/workout-sessions") or [])}
    made = 0
    for name, days_ago, dur, wnote, entries in LOGS:
        d = (today - timedelta(days_ago)).isoformat()
        if (name, d) in existing:
            continue
        sets = [{"exerciseId": ex_id[en], "setNumber": k, "reps": reps, "weightKg": wt}
                for en, _n, ss in entries for k, (reps, wt) in enumerate(ss)]
        notes = [{"exerciseId": ex_id[en], "note": n} for en, n, _ss in entries if n]
        api(token, "POST", "/api/v1/workout-sessions",
            {"name": name, "date": d, "durationMinutes": dur, "isCompleted": True,
             "sets": sets, "exerciseNotes": notes, "notes": wnote or None})
        made += 1
    return len(PLANS), made


def seed_today(token):
    """Log one grouped meal into today's Breakfast, so the "Added today" 🍲 group has data."""
    today = date.today().isoformat()
    already = any(f.get("mealName") for f in (api(token, "GET", f"/api/v1/logging/foods?date={today}") or []))
    if already:
        return 0
    id_by_sid = {f.get("serverId"): f["id"] for f in (api(token, "GET", "/api/v1/foods") or []) if f.get("serverId")}
    logged = 0
    oatmeal = next((m for m in MEALS if m["name"] == "Protein Oatmeal"), None)
    for it in (oatmeal["items"] if oatmeal else []):
        fid = id_by_sid.get(it["foodServerId"])
        if fid is None:
            continue
        api(token, "POST", "/api/v1/logging/foods",
            {"date": today, "foodId": fid, "mealSlot": "Breakfast",
             "quantity": it["quantity"], "unit": it["unit"], "mealName": "Protein Oatmeal"})
        logged += 1
    return logged


def seed_health(token):
    today = date.today()
    made = 0

    def has(type_):
        return len(api(token, "GET", f"/api/v1/health-metrics?type={type_}") or []) > 0

    def post(type_, value, unit, days_ago, secondary=None):
        d = today - timedelta(days=days_ago)
        body = {"type": type_, "value": value, "unit": unit, "recordedAt": f"{d.isoformat()}T08:00:00Z"}
        if secondary is not None:
            body["secondaryValue"] = secondary
        api(token, "POST", "/api/v1/health-metrics", body)

    if not has("GLUCOSE"):
        for i in range(21):
            v = 108 + round(12 * math.sin(i / 2.0)) + (i % 5) - 2
            post("GLUCOSE", float(v), "mg/dL", 20 - i); made += 1
    if not has("WEIGHT"):
        n = 20
        for i in range(n):
            v = round(82 - (i * 4.0 / (n - 1)) + 0.3 * math.sin(i), 1)
            post("WEIGHT", v, "kg", (n - 1 - i) * 3); made += 1
    if not has("BLOOD_PRESSURE"):
        n = 11
        for i in range(n):
            sys_ = 124 + round(6 * math.sin(i / 1.5)) - (i % 3)
            dia_ = 80 + round(4 * math.sin(i / 1.7)) - (i % 2)
            post("BLOOD_PRESSURE", float(sys_), "mmHg", (n - 1 - i) * 2, float(dia_)); made += 1
    return made


def main():
    if "--print-uid" in sys.argv:
        print(mint()[1])
        return
    # Safety: local only — never seed a remote/prod backend.
    if not any(API_BASE.startswith(p) for p in ("http://localhost", "http://127.0.0.1", "http://0.0.0.0")):
        sys.exit(f"✗ refusing: API_BASE={API_BASE} is not local.")
    token = mint()[0]
    try:
        nf, nm, nd = seed_nutrition(token)
        print(f"✓ nutrition: {nf} foods, {nm} meals, {nd} diets")
        ex_id, wk_id, tags, exs, wk = seed_training(token)
        print(f"✓ training : {tags} exercise tags, {exs} exercises, {wk} new workouts")
        plans, logs = seed_plans_and_logs(token, ex_id, wk_id)
        print(f"✓ plan/logs: {plans} day plans, {logs} new completed sessions (with notes)")
        today = seed_today(token)
        print(f"✓ today    : {today} foods logged as a grouped meal")
        health = seed_health(token)
        print(f"✓ health   : {health} new readings (glucose / weight / BP)")
        print(f"  Seeded {API_BASE}. Open Diets/Exercises to pull/refresh; Plan/Logs/Health read the server directly.")
    except urllib.error.HTTPError as e:
        sys.exit(f"✗ seed failed: HTTP {e.code}\n{e.read().decode(errors='replace')[:600]}")
    except urllib.error.URLError as e:
        sys.exit(f"✗ could not reach {API_BASE} ({e.reason}). Is the backend running?")


if __name__ == "__main__":
    main()
