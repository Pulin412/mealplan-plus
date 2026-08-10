# Local dev scripts

## One command: start backend + seed everything

```bash
cp scripts/local-seed.env.example scripts/local-seed.env   # then set FIREBASE_WEB_API_KEY
./scripts/local-up.sh
```

Starts the backend (**in-memory H2 — never touches prod/Neon**) and seeds a full demo dataset for a
login-able dev account, so every screen has data. When it prints **Ready**, open a client and sign in:

- **Webapp:** `cd webapp && npm run dev` → http://localhost:3000
- **Android:** run the app (debug points at the local backend)
- Sign in with **`dev@mealplan.test` / `mealplan123`** (configurable in `local-seed.env`)

`Ctrl+C` stops the backend. H2 is in-memory, so just re-run `local-up.sh` for a clean slate.
Backend already running? `./scripts/local-up.sh --seed-only` reseeds without a restart.

What gets seeded: 15 foods, 5 meals, 3 diets, 10 exercises + tags, 3 workout templates, 3 day plans,
3 completed sessions **with per-exercise + workout notes** (so **Copy last** shows a note), a grouped
meal logged to **today** ("Added today"), ~52 health readings, and 5 social dummies
(`alex/priya/sam/maya/leo @mealplan.test`, pw `mealplan123`) who follow the dev account.

## Config & safety

`FIREBASE_WEB_API_KEY` (Firebase console → Project settings → General → Web API key) lets the script
mint the dev account. `scripts/local-seed.env` holds it and is **gitignored** — never commit it.

The script is **local-only by design**: it forces in-memory H2 (scrubs `DB_URL` /
`SPRING_PROFILES_ACTIVE`) and refuses any non-localhost `API_BASE`, so it can't seed prod.

> Note: the dev account + social dummies are created in the project's shared Firebase Auth (there's a
> single Firebase project) — that's auth only, no DB impact. Fully isolating dev auth would need a
> separate Firebase project. Later this whole flow can move behind a Spring `local` profile.
