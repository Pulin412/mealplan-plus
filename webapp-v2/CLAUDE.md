# webapp-v2/ — MealPlan+ Web PWA (redesign)

Fresh Next.js 14 + TypeScript + Tailwind app. **Do not import or copy code from `webapp/`.**
Screens are built one at a time as the design doc is shared.

## Commands
- Dev server: `npm run dev`
- Build + lint: `npm run build && npm run lint`
- Regen API types: `npm run gen:api`   (reads `../docs/openapi.yaml`)

## Hard rules — never break
1. **Firebase free-tier only.** Auth only — no Firestore, Functions, or Storage.
2. **`webapp/` is read-only.** Never modify files in `webapp/`.
3. Commit / push only when explicitly asked (see root `CLAUDE.md`).

## Structure
```
src/
  app/            ← Next.js App Router — add route folders here screen by screen
  components/
    ui/           ← primitive design-system components (buttons, cards, inputs)
    layout/       ← shared layout shells (BottomNav, Header, etc.)
  lib/
    api/client.ts ← typed fetch wrapper (adds Firebase Bearer token)
    auth/firebase.ts
    utils/cn.ts
  hooks/          ← shared React hooks
  types/          ← shared TypeScript types
```

## Design tokens (spec §3) — live in globals.css as CSS variables
- `--color-primary`  teal  `oklch(0.62 0.09 210)`
- `--color-bg`       `#f7f9fa`
- `--color-ink`      `#14181b`
- `--color-surface`  `#ffffff`
- `--color-border`   `#eaeef0`
- `--color-danger`   `#b23b3b`
- `--radius-card`    `12px`

## API
All calls go through `src/lib/api/client.ts` — `apiFetch<T>(path, options)`.
Auth token is injected automatically from Firebase current user.
Backend base URL: `NEXT_PUBLIC_API_BASE_URL` env var.
