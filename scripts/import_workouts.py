#!/usr/bin/env python3
"""
Import the old app's workout templates into the new backend (REST, not /sync/push).

Source: data/old_app_workouts.json (the old app's bundled templates, recovered from git). Each old
set is {reps, weight, count}; the new model wants a flat ordered list of TemplateSetDto
{setNumber, reps, weightKg}. So a set with count=N is EXPANDED into N sets, weight -> weightKg
(0 -> null, i.e. no target weight), setNumber is 0-indexed per exercise. Exercises are resolved to
ids by name (run import_exercises.py first). Idempotent: templates already present by name are skipped.

Usage:
  python3 scripts/import_workouts.py --dry-run
  python3 scripts/import_workouts.py --api-base https://mealplan-api-rfo22lhanq-ez.a.run.app \
      --token <firebase_id_token> --allow-prod
"""
import argparse
import json
import os
import sys
import urllib.request
from pathlib import Path

DEFAULT_SRC = Path(__file__).resolve().parent.parent / "data" / "old_app_workouts.json"


def expand_sets(old_sets):
    """Old [{reps,weight,count}] -> flat [{setNumber,reps,weightKg}] (count repeated, 0kg -> null)."""
    out = []
    for s in old_sets:
        w = s.get("weight")
        weight = w if (w is not None and w > 0) else None
        for _ in range(int(s.get("count", 1) or 1)):
            out.append({"setNumber": len(out), "reps": s.get("reps"), "weightKg": weight})
    return out


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

    templates = json.loads(args.src.read_text())["templates"]
    total_sets = sum(len(expand_sets(e["sets"])) for t in templates for e in t["exercises"])
    print(f"Loaded {len(templates)} templates, "
          f"{sum(len(t['exercises']) for t in templates)} exercise-slots, {total_sets} sets after expansion.")

    if args.dry_run:
        print("\nDRY RUN — would create (skipping any already present by name):")
        for t in templates:
            print(f"\n  [{t['name']}]  ({t.get('notes') or ''})")
            for e in t["exercises"]:
                sets = expand_sets(e["sets"])
                shown = ", ".join(f"{s['reps']}×{s['weightKg'] if s['weightKg'] is not None else 'BW'}" for s in sets)
                print(f"     • {e['name']:<26} {len(sets)} sets: {shown}")
        print("\nNo data sent. Re-run without --dry-run (with a token) to import.")
        return

    if is_prod(args.api_base) and not args.allow_prod:
        sys.exit(f"REFUSING to write to prod ({args.api_base}). Re-run with --allow-prod if you mean it.")
    if not args.token:
        sys.exit("No token. Pass --token or set SEED_TOKEN (a Firebase ID token for the target account).")

    ex_id = {e["name"]: e["id"] for e in (api(args.api_base, args.token, "GET", "/api/v1/exercises") or [])}
    existing = {w["name"] for w in (api(args.api_base, args.token, "GET", "/api/v1/workout-templates") or [])}

    missing_ex = sorted({e["name"] for t in templates for e in t["exercises"] if e["name"] not in ex_id})
    if missing_ex:
        sys.exit(f"These exercises aren't on the server (run import_exercises.py first): {', '.join(missing_ex)}")

    created, skipped = 0, 0
    for t in templates:
        if t["name"] in existing:
            skipped += 1
            continue
        body = {
            "name": t["name"],
            "notes": t.get("notes") or None,
            "exercises": [
                {"exerciseId": ex_id[e["name"]], "orderIndex": i, "sets": expand_sets(e["sets"])}
                for i, e in enumerate(t["exercises"])
            ],
        }
        api(args.api_base, args.token, "POST", "/api/v1/workout-templates", body)
        created += 1

    print(f"✓ templates: {created} created, {skipped} already present (skipped)")
    print(f"\nDone → {args.api_base}. Open Exercises → Workouts in the app to review them.")


if __name__ == "__main__":
    main()
