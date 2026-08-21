#!/usr/bin/env python3
"""
Generate a Flyway migration that backfills the extra per-100g nutrients
(fiber / sugars / saturated fat / sodium) for the **system food catalog**.

Why a generator (not a live DB script):
  System foods are shared with every user and this is a prod write, so we emit a
  reviewable, versioned, idempotent SQL migration instead of hitting prod directly.
  You eyeball the values, then it lands on the next deploy like any other migration.

Data source: USDA FoodData Central (FDC) — free, accurate for whole/raw ingredients.
  Get a free key at https://fdc.nal.usda.gov/api-key-signup.html and export it:
      export FDC_API_KEY=xxxxxxxx
  (DEMO_KEY works for a handful of lookups but is rate-limited to ~30/hour.)

Manual overrides (optional): data/system_food_nutrients_overrides.json
      { "Paneer": { "fiber": 0, "sugars": 1.2, "satFat": 12.3, "sodium": 0.02 }, ... }
  Overrides win over FDC — use them for Indian/ambiguous items FDC matches poorly.

Safety of the emitted SQL:
  - Only touches is_system_food = true rows, keyed by name.
  - COALESCE(col, value) → never overwrites a value already present (fill-if-NULL).
  - Missing nutrient → emitted as NULL → COALESCE no-ops, so re-running is safe.

Usage:
  python3 scripts/seed/backfill_system_food_nutrients.py            # write V18 + report
  python3 scripts/seed/backfill_system_food_nutrients.py --dry-run  # report only, no file
  python3 scripts/seed/backfill_system_food_nutrients.py --limit 5  # test a few
"""
import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
INGREDIENTS = ROOT / "data" / "ingredients.json"
EXTRA_FOODS = ROOT / "data" / "extra_foods.json"
OVERRIDES = ROOT / "data" / "system_food_nutrients_overrides.json"
DEFAULT_OUT = ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V18__backfill_system_food_nutrients.sql"

FDC_KEY = os.environ.get("FDC_API_KEY", "DEMO_KEY")
FDC_SEARCH = "https://api.nal.usda.gov/fdc/v1/foods/search"
# Prefer analysed/reference data types over branded products for generic ingredients.
DATA_TYPES = "Foundation,SR Legacy,Survey (FNDDS)"

# FDC nutrient numbers → our fields. Sugars has two ids across datasets.
NUTRIENT_IDS = {"fiber": {1079}, "sugars": {2000, 1063}, "satFat": {1258}, "sodium": {1093}}


def load_catalog_names() -> list[str]:
    names: list[str] = []
    seen = set()
    for f in (json.load(open(INGREDIENTS))["foods"] if INGREDIENTS.exists() else []):
        n = f["name"].strip()
        if n and n not in seen:
            seen.add(n); names.append(n)
    for f in (json.load(open(EXTRA_FOODS)) if EXTRA_FOODS.exists() else []):
        n = f["name"].strip()
        if n and n not in seen:
            seen.add(n); names.append(n)
    return names


def fdc_lookup(name: str) -> dict | None:
    """Best-match FDC food → {fiber, sugars, satFat, sodium} per 100g (sodium in grams). None if no match."""
    # quote_via=quote → spaces become %20, not '+'. FDC's nginx 400s on '+' in dataType.
    qs = urllib.parse.urlencode(
        {"query": name, "dataType": DATA_TYPES, "pageSize": 1, "api_key": FDC_KEY},
        quote_via=urllib.parse.quote,
    )
    req = urllib.request.Request(f"{FDC_SEARCH}?{qs}", headers={"User-Agent": "MealPlanPlus-backfill"})
    # FDC's edge intermittently 400s / rate-limits well-formed requests; retry with backoff
    # so transient failures don't get mislabelled as genuine "no match".
    data = None
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=20) as r:
                data = json.loads(r.read().decode())
            break
        except urllib.error.HTTPError as e:
            if e.code in (400, 429, 500, 502, 503) and attempt < 4:
                time.sleep(1.5 * (attempt + 1))
                continue
            raise
        except urllib.error.URLError:
            if attempt < 4:
                time.sleep(1.5 * (attempt + 1))
                continue
            raise
    foods = data.get("foods") or []
    if not foods:
        return None
    out = {"fiber": None, "sugars": None, "satFat": None, "sodium": None}
    for n in foods[0].get("foodNutrients", []):
        nid, val = n.get("nutrientId"), n.get("value")
        if val is None:
            continue
        for key, ids in NUTRIENT_IDS.items():
            if nid in ids and out[key] is None:
                out[key] = val / 1000.0 if key == "sodium" else float(val)  # FDC sodium is mg/100g
    return out if any(v is not None for v in out.values()) else None


def sql_num(v) -> str:
    return "NULL" if v is None else f"{round(v, 3)}"


def esc(s: str) -> str:
    return s.replace("'", "''")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(DEFAULT_OUT))
    ap.add_argument("--dry-run", action="store_true", help="report only, don't write the migration")
    ap.add_argument("--limit", type=int, default=0, help="only process the first N foods (testing)")
    ap.add_argument("--sleep", type=float, default=0.4, help="seconds between FDC calls")
    args = ap.parse_args()

    overrides = json.load(open(OVERRIDES)) if OVERRIDES.exists() else {}
    names = load_catalog_names()
    if args.limit:
        names = names[: args.limit]
    if FDC_KEY == "DEMO_KEY":
        print("⚠ using DEMO_KEY (≈30 req/hr). Set FDC_API_KEY for the full run.", file=sys.stderr)

    rows, matched, unmatched = [], [], []
    for i, name in enumerate(names, 1):
        n = None
        src = ""
        if name in overrides:
            o = overrides[name]
            n = {"fiber": o.get("fiber"), "sugars": o.get("sugars"), "satFat": o.get("satFat"), "sodium": o.get("sodium")}
            src = "override"
        else:
            try:
                n = fdc_lookup(name)
                src = "fdc"
            except Exception as e:
                print(f"  ! {name}: FDC error {e}", file=sys.stderr)
            time.sleep(args.sleep)
        if n and any(v is not None for v in n.values()):
            matched.append(name)
            rows.append(
                f"UPDATE public.foods SET\n"
                f"    fiber_per100         = COALESCE(fiber_per100, {sql_num(n['fiber'])}),\n"
                f"    sugars_per100        = COALESCE(sugars_per100, {sql_num(n['sugars'])}),\n"
                f"    saturated_fat_per100 = COALESCE(saturated_fat_per100, {sql_num(n['satFat'])}),\n"
                f"    sodium_per100        = COALESCE(sodium_per100, {sql_num(n['sodium'])})\n"
                f"  WHERE is_system_food = true AND name = '{esc(name)}';  -- {src}"
            )
        else:
            unmatched.append(name)
        print(f"[{i}/{len(names)}] {name}: {'ok' if name in matched else 'no match'}", file=sys.stderr)

    print(f"\nmatched {len(matched)}, unmatched {len(unmatched)}", file=sys.stderr)
    if unmatched:
        print("unmatched (add to data/system_food_nutrients_overrides.json):", file=sys.stderr)
        print("  " + ", ".join(unmatched), file=sys.stderr)

    if args.dry_run:
        print("(dry run — no migration written)", file=sys.stderr)
        return 0

    header = (
        "-- V18: backfill extra per-100g nutrients for the shared system-food catalog.\n"
        "-- Generated by scripts/seed/backfill_system_food_nutrients.py (USDA FoodData Central + overrides).\n"
        "-- Idempotent & non-destructive: COALESCE only fills columns that are still NULL; re-runnable.\n"
        f"-- {len(matched)} foods updated; {len(unmatched)} left NULL (no reliable source — curate via overrides).\n\n"
    )
    Path(args.out).write_text(header + "\n\n".join(rows) + "\n")
    print(f"wrote {args.out} ({len(rows)} UPDATEs)", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
