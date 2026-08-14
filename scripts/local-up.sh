#!/usr/bin/env bash
#
# One-command LOCAL dev: start the backend (in-memory H2) and seed a full demo dataset, then just
# open the webapp or android and sign in. No adb / no manual token / never touches prod.
#
#   ./scripts/local-up.sh                 # start backend + seed
#   ./scripts/local-up.sh --seed-only     # backend already running — just (re)seed
#
# Config lives in scripts/local-seed.env (gitignored — copy scripts/local-seed.env.example). You need
# a Firebase Web API key so the script can mint a login-able dev account and seed it. Sign into the
# app with DEV_EMAIL / DEV_PASSWORD (default dev@mealplan.test / mealplan123).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT/scripts/local-seed.env"
[ -f "$ENV_FILE" ] && { set -a; . "$ENV_FILE"; set +a; }
# Also load backend/.env (provider API keys: GROQ_API_KEY / GEMINI_API_KEY / … — gitignored) so the
# failover chain has real keys. Spring Boot won't read a .env file on its own; we export it here.
[ -f "$ROOT/backend/.env" ] && { set -a; . "$ROOT/backend/.env"; set +a; }

export API_BASE="${API_BASE:-http://localhost:8080}"
export DEV_EMAIL="${DEV_EMAIL:-dev@mealplan.test}"
export DEV_PASSWORD="${DEV_PASSWORD:-mealplan123}"
SEED="python3 $ROOT/scripts/seed_local_data.py"
LOG="$ROOT/scripts/.local-backend.log"

# Safety: local only. Refuse to point at anything but localhost so we can never seed a remote/prod
# backend by accident.
case "$API_BASE" in
  http://localhost*|http://127.0.0.1*|http://0.0.0.0*) : ;;
  *) echo "✗ refusing: API_BASE=$API_BASE is not local." >&2; exit 1 ;;
esac

wait_for_health() {
  echo "▶ waiting for backend at $API_BASE …"
  for _ in $(seq 1 90); do
    curl -sf "$API_BASE/actuator/health" >/dev/null 2>&1 && { echo "  backend up."; return 0; }
    sleep 2
  done
  echo "✗ backend did not become healthy in time." >&2; return 1
}

: "${FIREBASE_WEB_API_KEY:?Set FIREBASE_WEB_API_KEY in scripts/local-seed.env (copy scripts/local-seed.env.example).}"

# ── seed-only: don't start a backend, just seed the running one ──────────────────
if [ "${1:-}" = "--seed-only" ]; then
  wait_for_health
  $SEED
  exit 0
fi

echo "▶ minting dev account ($DEV_EMAIL) …"
DEV_UID="$($SEED --print-uid)"
echo "  uid: $DEV_UID"

# Safety: force local in-memory H2. The default config falls back to $DB_URL (Neon prod) if it's set
# in the environment, so scrub any prod DB/profile vars before launching — local must NEVER touch prod.
unset DB_URL DB_USER DB_PASSWORD DB_DRIVER DB_DIALECT DDL_AUTO SPRING_PROFILES_ACTIVE SPRING_DATASOURCE_URL

echo "▶ starting backend (in-memory H2, no prod DB) with social dummies following $DEV_EMAIL …"
(
  cd "$ROOT/backend"
  DEV_SEED_SOCIAL_FIREBASE_API_KEY="$FIREBASE_WEB_API_KEY" \
  DEV_SEED_SOCIAL_PRIMARY_UID="$DEV_UID" \
    ./gradlew bootRun
) >"$LOG" 2>&1 &
BACKEND_PID=$!
trap 'echo; echo "▶ stopping backend …"; kill "$BACKEND_PID" 2>/dev/null || true' INT TERM EXIT

wait_for_health || { echo "  see $LOG"; exit 1; }

echo "▶ seeding demo data …"
$SEED

cat <<EOF

────────────────────────────────────────────────────────────
✅ Ready — backend on $API_BASE (in-memory H2; logs: scripts/.local-backend.log)

   Sign in on webapp (cd webapp && npm run dev → http://localhost:3000)
   or android with:
       ${DEV_EMAIL}  /  ${DEV_PASSWORD}

   Social dummies: alex / priya / sam / maya / leo @mealplan.test (pw mealplan123)
   Ctrl+C to stop the backend.
────────────────────────────────────────────────────────────
EOF

wait "$BACKEND_PID"
