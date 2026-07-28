# MealPlan+ — Feature Prioritization & MVP (redesign)

**Product thesis:** the job-to-be-done is *"plan what I'll eat, log what I actually ate, and see how I'm tracking."*
Everything is prioritized by how directly it serves that loop: **Plan → Log → Track**.
Priority = user value ÷ effort, adjusted for dependencies. PWA-parity is flagged per feature since
Android (Compose) and the Next.js PWA must stay in sync.

Legend — Priority: **P0** MVP core loop · **P1** completes the loop / retention · **P2** differentiators · **P3** later.
PWA: ✅ full parity · ⚠️ adapt for web · ❌ Android-only.

---

## P0 — MVP (the core loop, nothing else)

| # | Feature | Screens | Why it's MVP | PWA |
|---|---------|---------|--------------|-----|
| 1 | **Auth** (login, sign-up, forgot password) | `auth/` | Gate for sync + identity. Keep it minimal. | ✅ |
| 2 | **Food library** — browse, search, add custom food | `foods/`, AddFood | Atomic unit everything else references. | ✅ |
| 3 | **Daily log** — log a food/meal to a slot (B/L/D) | `log/` | THE daily action. If nothing else works, this must. | ✅ |
| 4 | **Home / Today** — today's plan + quick-log + streak nudge | `home/` | First screen every session; the loop's front door. | ✅ |
| 5 | **Meals** — create/edit reusable food collections | `meals/` | Makes logging fast (log "Oatmeal bowl", not 5 foods). | ✅ |
| 6 | **Offline-first persistence** | (infra) | Core promise; the app must work with no network. | ⚠️ (service worker) |

**MVP definition of done:** a user can sign in, build a small food/meal library, and log meals
against Breakfast/Lunch/Dinner every day — fully offline — and see today at a glance. That's a
shippable product on its own.

---

## P1 — Completes the loop & drives retention

| # | Feature | Screens | Why | PWA |
|---|---------|---------|-----|-----|
| 7 | **Diets / templates** — build a diet with per-slot meals | `diets/` | Turns logging into *planning*; the "plan" half of the loop. | ✅ |
| 8 | **Calendar / day planning** — plan days from a template (`planned_slots`) | `calendar/` | Look-ahead planning; the app's real hook. | ⚠️ |
| 9 | **Health metrics** — log weight, view steps/calories | `health/` | The "track" half — progress, not just logging. | ⚠️ (Health Connect is Android-only; web = manual entry) |
| 10 | **Charts / trends** | `charts/` | Makes tracking rewarding; drives return visits. | ✅ |
| 11 | **Smart notifications** — remind only when a slot is unlogged | (infra) | Retention lever; low effort, high stickiness. | ⚠️ (web push differs) |

---

## P2 — Differentiators (fast, low-friction wins)

| # | Feature | Screens | Why | PWA |
|---|---------|---------|-----|-----|
| 12 | **Barcode scanner** | `scanner/` | Removes the biggest logging friction. | ⚠️ (getUserMedia on web) |
| 13 | **Online food search** (external DB) | OnlineSearch | Reduces manual food entry. | ✅ |
| 14 | **Grocery lists** — generate from a plan | `grocery/` | Extends value beyond logging into the kitchen. | ✅ |
| 15 | **Profile & Settings** | `profile/`, `settings/` | Needed, but not a reason anyone downloads. | ✅ |

---

## P3 — Later (scope risk or platform-specific)

| # | Feature | Screens | Note | PWA |
|---|---------|---------|------|-----|
| 16 | **Workout tracking** — exercises, log, history | `workout/` | Strong pillar but a *different* JTBD. Consider a separate phase/module — don't let it dilute the food MVP. | ⚠️ |
| 17 | **Home-screen widget** (Glance) | `widget/` | Android-only; no PWA equivalent. | ❌ |
| 18 | **Backup / restore** (local + Drive) | `backup/` | Power-user feature; defer until data is worth protecting. | ❌ |
| 19 | **Multi-device sync** (delta push/pull) | (infra) | Only matters once someone uses 2 devices. | ✅ |

---

## Suggested build order (one screen at a time, per your redesign approach)

Follow the loop, not the org chart. Each step is a working slice:

1. Auth → 2. Home/Today (empty state) → 3. Food library + Add Food → 4. Daily Log →
5. Meals → *(MVP shippable here)* → 6. Diets → 7. Calendar → 8. Health → 9. Charts →
10. Notifications → then P2, then P3.

**PWA parity rule while designing:** any screen touching Health Connect (9), widget (17),
notifications (11), or the scanner (12) needs an explicit "web variant" note during design,
since those capabilities differ on Safari/PWA. Everything in P0 is fully portable — start there.

---

## Assumptions (adjust and I'll re-cut the list)
- MVP = lean relaunch of the *core loop*, not parity with the current build.
- Workout tracking is treated as a **separate product pillar**, deliberately out of the food MVP.
- Auth stays in MVP (sync + identity depend on it); could drop to local-only if you want an even leaner v0.
