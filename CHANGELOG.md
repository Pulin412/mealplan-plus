# Changelog

Curated release notes. **Android** and **Webapp** version independently, each with its own
section below. On release, the workflow pulls the matching `### [<version>]` block into the
GitHub Release body (see `scripts/changelog.sh`):

- **Android** — before dispatching `android-release.yml`, rename `[Unreleased]` to the version
  you're releasing (+ today's date). If no matching block exists, the workflow uses the manual
  `notes` input instead.
- **Webapp** — bump `webapp/package.json` and add a `### [<that version>]` block; on merge to
  `main`, `webapp-release.yml` publishes `webapp-v<version>` with this block.

Keep entries short and user-facing, grouped under `#### Added` / `#### Fixed` / `#### Changed`.

## Android

### [2.2.24] - 2026-10-11

#### Added
- Journal: a daily diary — jump to any date, see each day's diet and workouts with their status, tap a completed workout for a read-only log, and keep private notes. Lock notes behind your fingerprint/face or device PIN.
- Health: log glucose, weight and blood pressure together in one quick form.
- Diets: delete tags you no longer use, right from the Diets screen or the diet builder.
- Plan: sort the "Choose a diet" picker (Recent / Name / Calories / Protein); diets now sort by name by default.
- Groceries: add your own custom items to a shopping list.

### [2.3.0] - 2026-08-13

#### Added
- Workouts: exercises now have a type — Strength (reps + weight), Cardio (time + distance), or Timed (duration only). The runner, workout builder, and logs all adapt to each type.
- Plan: assign individual meals to a day's slots — on top of, or instead of, a diet. Today and your shopping list include them.

### [2.2.15] - 2026-08-12

#### Fixed
- Workout session: serialized auto-save — fixes duplicate sets and trimmed notes when resuming a session after leaving it.

#### Added
- Workout session: confirm dialog before leaving an in-progress workout.

### [2.2.14] - 2026-08-12

#### Added
- Home: already-planned workouts are hidden from the "add workout" picker.
- Workout session: add/remove exercises on the fly (remove only the ones you added); a standalone single-exercise log no longer offers "add exercise".

## Webapp

### [0.4.0] - 2026-10-11

#### Added
- Journal: a daily diary — jump to any date, see each day's diet and workouts with their status, tap a completed workout for a read-only log, and keep private notes. Lock notes behind a PIN.
- Health: log glucose, weight and blood pressure together in one quick form.
- Diets: delete tags you no longer use, from the Diets screen or the diet builder.
- Plan: sort the "Choose a diet" picker (Recent / Name / Calories / Protein); diets now sort by name by default.
- Groceries: add your own custom items to a shopping list.

### [0.3.0] - 2026-08-13

#### Added
- Workouts: exercises now have a type — Strength (reps + weight), Cardio (time + distance), or Timed (duration only). The runner, workout builder, and logs all adapt to each type.
- Plan: assign individual meals to a day's slots — on top of, or instead of, a diet. Today and your shopping list include them.

### [0.2.0] - 2026-08-12

#### Fixed
- Workout session: serialized saves — fixes duplicate sets and trimmed notes when resuming a session.

#### Added
- Workout session: confirm before leaving an in-progress workout.
- Home: already-planned workouts are hidden from the "add workout" picker.

#### Changed
- App version now sourced from `package.json` (shown in Settings); bumped to 0.2.0.
