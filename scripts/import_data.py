#!/usr/bin/env python3
"""
Import foods / meals / diets from a seed JSON into ANY account, via the REST API
(the same /api/v1/sync/push upsert-by-UUID endpoint the app uses — so records get real
server ids and match the current DB design).

Safety:
  • Defaults to API_BASE=http://localhost:8080 (local backend).
  • REFUSES to write to prod (Cloud Run / Neon) unless you pass --allow-prod.
  • --dry-run validates + builds payloads and prints a summary WITHOUT calling the API
    (no token or backend needed) — use this first to be sure.

Data sources:
  • Meals + diets: data/seed_data.json  (diets → per-slot meals → items referencing foods by NAME).
  • Food macros:   scripts/seed/prepared_foods.json (name/calories/protein/carbs/fat, per 100 g).
  Food names in meals/diets are resolved to foods that already exist for the account (GET /foods,
  which includes the shared system foods); with --what foods|all, any missing catalog foods are
  created first so meals can resolve them.

Usage:
  # 1) Safe offline check — reads the JSON, reports coverage, builds payloads, POSTs nothing:
  python3 scripts/import_data.py --what all --dry-run

  # 2) Real import into a LOCAL backend for a given account (token = a Firebase ID token):
  #    (start the backend first: SPRING_PROFILES_ACTIVE=local ./gradlew :backend:bootRun)
  SEED_TOKEN=<firebase_id_token> python3 scripts/import_data.py --what all

  # import just one kind:
  python3 scripts/import_data.py --what diets  --dry-run

Getting a Firebase ID token for the target account:
  • Sign in as that account on the webapp, DevTools → Application → IndexedDB →
    firebaseLocalStorageDb → look for the "accessToken" (the ID token), OR
  • pass it via --token / SEED_TOKEN.
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DEFAULT_SEED = REPO / "data" / "seed_data.json"
DEFAULT_CATALOG = REPO / "scripts" / "seed" / "prepared_foods.json"

# Deterministic ids so re-runs upsert instead of duplicating.
NS = uuid.UUID("6ba7b812-9dad-11d1-80b4-00c04fd430c8")
UPDATED_AT = "2026-07-30T00:00:00Z"

# seed_data unit -> API FoodUnit enum [GRAM, ML, PIECE, CUP, TBSP, TSP]
UNIT_MAP = {"g": "GRAM", "ml": "ML", "piece": "PIECE", "cup": "CUP", "glass": "CUP", "tbsp": "TBSP", "tsp": "TSP"}
# seed_data slot key -> readable slot label (stored as a free-form string)
SLOT_MAP = {
    "EARLY_MORNING": "Early Morning", "BREAKFAST": "Breakfast", "NOON": "Noon",
    "LUNCH": "Lunch", "EVENING": "Evening", "PRE_WORKOUT": "Pre-Workout",
    "POST_WORKOUT": "Post-Workout", "DINNER": "Dinner", "POST_DINNER": "Post-Dinner",
}


def norm(name: str) -> str:
    return " ".join(name.strip().lower().split())


def food_sid(name: str) -> str:
    return str(uuid.uuid5(NS, "food|" + norm(name)))


def meal_sid(name: str, items) -> str:
    sig = "|".join(f"{norm(i['food'])}:{i['quantity']}:{i['unit']}" for i in items)
    return str(uuid.uuid5(NS, f"meal|{norm(name)}|{sig}"))


def diet_sid(name: str, meal_type: str) -> str:
    return str(uuid.uuid5(NS, f"diet|{norm(name)}|{meal_type}"))


def unit(u: str) -> str:
    m = UNIT_MAP.get((u or "g").lower())
    if not m:
        raise ValueError(f"unknown unit {u!r}")
    return m


def slot(s: str) -> str:
    return SLOT_MAP.get(s, s.replace("_", " ").title())


# ── loaders ───────────────────────────────────────────────────────────────────
def load_catalog(path: Path):
    """name -> macro dict (per 100 g)."""
    data = json.loads(path.read_text())
    rows = data if isinstance(data, list) else data.get("foods", [])
    return {norm(r["name"]): r for r in rows}


def load_seed(path: Path):
    return json.loads(path.read_text())["diets"]


def referenced_foods(diets):
    names = {}
    for d in diets:
        for _s, meal in d.get("meals", {}).items():
            for it in meal.get("items", []):
                names.setdefault(norm(it["food"]), it["food"])
    return names  # normalized -> original


# ── payload builders (shapes match /sync/push, see dev-seed-h2.py) ──────────────
def build_food_payloads(catalog, only_names=None):
    out = []
    for n, r in catalog.items():
        if only_names is not None and n not in only_names:
            continue
        out.append({
            "serverId": food_sid(r["name"]),
            "name": r["name"],
            "caloriesPer100": r.get("calories", 0),
            "proteinPer100": r.get("protein", 0),
            "carbsPer100": r.get("carbs", 0),
            "fatPer100": r.get("fat", 0),
            "unit": "GRAM",
            "verified": True,
            "updatedAt": UPDATED_AT,
        })
    return out


def build_meals_and_diets(diets, resolve):
    """resolve(name)->serverId or None. Returns (meals, diet_payloads, unresolved set)."""
    meals_by_sid = {}
    diet_payloads = []
    unresolved = set()

    for d in diets:
        diet_meals = []
        for s, meal in d.get("meals", {}).items():
            items = meal.get("items", [])
            sid = meal_sid(meal["name"], items)
            if sid not in meals_by_sid:
                mitems = []
                for it in items:
                    fsid = resolve(norm(it["food"]))
                    if not fsid:
                        unresolved.add(it["food"])
                        continue
                    mitems.append({"foodId": 0, "foodServerId": fsid,
                                   "quantity": it["quantity"], "unit": unit(it["unit"])})
                meals_by_sid[sid] = {
                    "serverId": sid, "name": meal["name"], "slots": [slot(s)],
                    "items": mitems, "isFavorite": False, "updatedAt": UPDATED_AT,
                }
            diet_meals.append({"mealId": 0, "mealServerId": sid, "dayOfWeek": 0, "slot": slot(s)})
        diet_payloads.append({
            "serverId": diet_sid(d["name"], d.get("meal_type", "")),
            "name": d["name"],
            "description": d.get("description"),
            "meals": diet_meals, "foodItems": [], "tagIds": [],
            "isFavorite": False, "updatedAt": UPDATED_AT,
        })
    return list(meals_by_sid.values()), diet_payloads, unresolved


# ── API ─────────────────────────────────────────────────────────────────────--
def api(base, token, method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{base}{path}", data=data, method=method, headers={
        "Content-Type": "application/json", "Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        raw = resp.read()
        return json.loads(raw) if raw else None


def server_food_map(base, token):
    foods = api(base, token, "GET", "/api/v1/foods") or []
    return {norm(f["name"]): f.get("serverId") for f in foods if f.get("serverId")}


def is_prod(base: str) -> bool:
    return any(h in base for h in ("run.app", "neon.tech", "mealplan-api"))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--what", choices=["foods", "meals", "diets", "all"], default="all")
    ap.add_argument("--seed", type=Path, default=DEFAULT_SEED)
    ap.add_argument("--catalog", type=Path, default=DEFAULT_CATALOG)
    ap.add_argument("--api-base", default=os.environ.get("API_BASE", "http://localhost:8080"))
    ap.add_argument("--token", default=os.environ.get("SEED_TOKEN"))
    ap.add_argument("--dry-run", action="store_true", help="validate + build payloads, POST nothing")
    ap.add_argument("--allow-prod", action="store_true", help="required to target Cloud Run / Neon")
    args = ap.parse_args()

    do_foods = args.what in ("foods", "all")
    do_meals = args.what in ("meals", "all")
    do_diets = args.what in ("diets", "all")

    catalog = load_catalog(args.catalog)
    diets = load_seed(args.seed)
    ref = referenced_foods(diets)
    print(f"Loaded {len(diets)} diets, {len(ref)} referenced foods, {len(catalog)} catalog foods.")

    if not args.dry_run:
        if is_prod(args.api_base) and not args.allow_prod:
            sys.exit(f"REFUSING to write to prod ({args.api_base}). Re-run with --allow-prod if you really mean it.")
        if not args.token:
            sys.exit("No token. Pass --token or set SEED_TOKEN (a Firebase ID token for the target account).")

    # ── resolve foods ─────────────────────────────────────────────────────────
    if args.dry_run:
        # Offline: a name resolves if it's in the catalog (so a food record exists to create).
        known = set(catalog.keys())
        resolve = lambda n: (food_sid(catalog[n]["name"]) if n in known else None)
    else:
        smap = server_food_map(args.api_base, args.token)  # existing (system + user) foods
        if do_foods:
            missing = [n for n in catalog if n not in smap]
            payloads = build_food_payloads(catalog, only_names=set(missing))
            if payloads:
                api(args.api_base, args.token, "POST", "/api/v1/sync/push", {"foods": payloads})
                print(f"✓ foods   : created {len(payloads)} missing catalog foods")
            else:
                print("✓ foods   : catalog already present, nothing to create")
            smap = server_food_map(args.api_base, args.token)  # refresh
        resolve = lambda n: smap.get(n)

    meals, diet_payloads, unresolved = build_meals_and_diets(diets, resolve)

    if unresolved:
        print(f"⚠ {len(unresolved)} referenced foods not resolvable (their meal items are skipped):")
        print("   " + ", ".join(sorted(unresolved)))

    # ── dry run: report + exit ────────────────────────────────────────────────
    if args.dry_run:
        covered = len(ref) - len(unresolved)
        print(f"\nDRY RUN — would import:")
        if do_foods:
            print(f"  foods : {len(catalog)} catalog foods ({covered}/{len(ref)} referenced names covered)")
        if do_meals:
            print(f"  meals : {len(meals)} unique meals")
        if do_diets:
            print(f"  diets : {len(diet_payloads)} diets")
        if meals:
            print("  sample meal:", json.dumps(meals[0], separators=(",", ":"))[:200])
        print("\nNo data sent. Re-run without --dry-run (and with a token) to import.")
        return

    # ── real import ───────────────────────────────────────────────────────────
    push = {}
    if do_meals:
        push["meals"] = meals
    if do_diets:
        push["diets"] = diet_payloads
    if push:
        api(args.api_base, args.token, "POST", "/api/v1/sync/push", push)
        if do_meals:
            print(f"✓ meals   : {len(meals)} meals")
        if do_diets:
            print(f"✓ diets   : {len(diet_payloads)} diets")
    print(f"\nDone → {args.api_base}. Open Meals / Diets in the app to pull them in.")


if __name__ == "__main__":
    main()
