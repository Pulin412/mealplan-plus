#!/usr/bin/env python3
"""
Import the old app's exercise library into the new backend (REST, not /sync/push).

The old app shipped a bundled library in android/src/main/assets/exercises.json (removed at the
v2 cutover). Those 49 exercises are preserved at data/old_app_exercises.json. The new model has no
muscleGroup/equipment/category fields — it uses TAGS — so this maps each old muscleGroup/category to
one of the new app's design-palette exercise tags, and keeps the old equipment string as the
description. Idempotent: exercises/tags already present (matched by name) are left untouched.

Usage:
  # dry run — build + print the payloads, POST nothing (no token needed):
  python3 scripts/import_exercises.py --dry-run

  # real import into a LOCAL backend:
  SEED_TOKEN=<firebase_id_token> python3 scripts/import_exercises.py

  # real import into PROD (Cloud Run) — requires --allow-prod and the target account's token:
  python3 scripts/import_exercises.py --api-base https://mealplan-api-rfo22lhanq-ez.a.run.app \
      --token <firebase_id_token> --allow-prod
"""
import argparse
import json
import os
import sys
import urllib.request
from pathlib import Path

DEFAULT_SRC = Path(__file__).resolve().parent.parent / "data" / "old_app_exercises.json"

# New app's design-palette exercise tags (name -> hex), from scripts/dev-seed-h2.py.
PALETTE = {
    "Chest": "#A74541", "Back": "#2E69B2", "Legs": "#1D7D3E", "Shoulders": "#9A5500",
    "Arms": "#8050A0", "Core": "#007D86", "Cardio": "#A74449", "Mobility": "#00805D",
}

# Old granular muscleGroup -> new palette tag.
MG_TO_TAG = {
    "Chest": "Chest", "Back": "Back", "Shoulders": "Shoulders", "Core": "Core",
    "Biceps": "Arms", "Triceps": "Arms",
    "Legs": "Legs", "Quadriceps": "Legs", "Hamstrings": "Legs",
}


def tag_for(ex):
    """One palette tag for an old exercise (or None if nothing sensible maps)."""
    mg, cat = ex.get("muscleGroup"), ex.get("category")
    if mg in MG_TO_TAG:
        return MG_TO_TAG[mg]
    if cat == "FLEXIBILITY":   # Yoga Flow, Static Stretching, Foam Rolling, Dynamic Warm-Up
        return "Mobility"
    if cat == "CARDIO":        # Running, Cycling, Rowing, …
        return "Cardio"
    if mg == "Full Body":      # Burpee, Kettlebell Swing (category OTHER)
        return "Cardio"
    return None


def build(exercises):
    """-> (set of tag names used, [{'name','description','_tag'} ...])."""
    used_tags, rows = set(), []
    for ex in exercises:
        tag = tag_for(ex)
        if tag:
            used_tags.add(tag)
        rows.append({
            "name": ex["name"],
            "description": ex.get("equipment") or None,  # keep the old equipment string as a note
            "_tag": tag,
        })
    return used_tags, rows


def api(base, token, method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{base}{path}", data=data, method=method, headers={
        "Content-Type": "application/json", "Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        raw = resp.read()
        return json.loads(raw) if raw else None


def is_prod(base):
    return any(h in base for h in ("run.app", "neon.tech", "mealplan-api"))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--src", type=Path, default=DEFAULT_SRC)
    ap.add_argument("--api-base", default=os.environ.get("API_BASE", "http://localhost:8080"))
    ap.add_argument("--token", default=os.environ.get("SEED_TOKEN"))
    ap.add_argument("--dry-run", action="store_true", help="build + print payloads, POST nothing")
    ap.add_argument("--allow-prod", action="store_true", help="required to target Cloud Run / Neon")
    args = ap.parse_args()

    exercises = json.loads(args.src.read_text())
    used_tags, rows = build(exercises)
    print(f"Loaded {len(rows)} exercises; {len(used_tags)} tags used: {', '.join(sorted(used_tags))}")

    if args.dry_run:
        by_tag = {}
        for r in rows:
            by_tag.setdefault(r["_tag"] or "(no tag)", []).append(r)
        print("\nDRY RUN — would create (skipping any already present by name):")
        for tag in sorted(by_tag):
            print(f"\n  ▸ {tag}")
            for r in by_tag[tag]:
                desc = f"  [{r['description']}]" if r["description"] else ""
                print(f"     • {r['name']}{desc}")
        print("\nNo data sent. Re-run without --dry-run (with a token) to import.")
        return

    if is_prod(args.api_base) and not args.allow_prod:
        sys.exit(f"REFUSING to write to prod ({args.api_base}). Re-run with --allow-prod if you mean it.")
    if not args.token:
        sys.exit("No token. Pass --token or set SEED_TOKEN (a Firebase ID token for the target account).")

    # Tags: reuse existing by name, create missing (re-run safe).
    tag_id = {t["name"]: t["id"] for t in (api(args.api_base, args.token, "GET", "/api/v1/tags?entityType=EXERCISE") or [])}
    created_tags = 0
    for name in sorted(used_tags):
        if name not in tag_id:
            tag_id[name] = api(args.api_base, args.token, "POST", "/api/v1/tags",
                               {"name": name, "entityType": "EXERCISE", "color": PALETTE.get(name, "#7B8288")})["id"]
            created_tags += 1

    # Exercises: create only those missing by name (leave existing untouched — don't clobber edits).
    existing = {e["name"] for e in (api(args.api_base, args.token, "GET", "/api/v1/exercises") or [])}
    created, skipped = 0, 0
    for r in rows:
        if r["name"] in existing:
            skipped += 1
            continue
        body = {"name": r["name"], "description": r["description"],
                "tagIds": [tag_id[r["_tag"]]] if r["_tag"] else []}
        api(args.api_base, args.token, "POST", "/api/v1/exercises", body)
        created += 1

    print(f"✓ tags     : {created_tags} created, {len(tag_id) - created_tags} reused")
    print(f"✓ exercises: {created} created, {skipped} already present (skipped)")
    print(f"\nDone → {args.api_base}. Open Exercises in the app to review them.")


if __name__ == "__main__":
    main()
