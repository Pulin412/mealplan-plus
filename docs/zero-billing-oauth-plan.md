# Zero-Billing OAuth Plan (Execution Baseline)

## 1) Goal and Non-Goals
- Goal: Add Google OAuth for Android authentication without introducing any billable dependency path.
- Non-goals:
  - No Apple Sign-In in this phase.
  - No iOS OAuth release support.
  - No cloud data sync/storage.
  - No phone/SMS auth.

## 2) Hard No-Billing Rules
- Keep Firebase project on Spark (free) tier.
- Do not link Cloud Billing account to this auth project.
- Do not enable paid Firebase data products.
- If free-tier limits are hit, service degradation is acceptable; no paid fallback.

## 3) Allowed Auth Methods
- Android:
  - Google OAuth sign-in (feature-flagged on by default).
  - Existing local email/password remains available.
- iOS:
  - Existing local login/signup flow only for release builds.

## 4) Explicitly Forbidden Features
- Phone Auth / SMS.
- Firestore.
- Cloud Functions.
- Cloud Storage.
- Realtime Database.
- Identity Platform upgrade for Firebase Auth.

## 5) Platform Policy Note
- iOS OAuth is disabled for release in this phase (`oauth_google_ios_enabled = false`).
- Rationale: keep scope aligned to no-billing + avoid Apple sign-in compliance expansion.

## 6) Release Gates / Checklist
- [ ] Spark plan confirmed and no billing account attached.
- [ ] Forbidden Firebase products not present in dependencies/config.
- [ ] Android Google OAuth path tested (new + returning user).
- [ ] Local email/password path regression-tested.
- [ ] iOS release build has no OAuth entry point.
- [ ] User-facing error states verified (cancel/config/rate-limit style failures).

## 7) Incident / Degradation Behavior
- On OAuth cancellation: show cancellation message and remain on login screen.
- On config errors (missing client ID/Firebase config): show actionable message; do not crash.
- On temporary auth outages/rate limits: show retry-later message; keep local auth available.

## 8) Ownership and Resume TODOs
- Owner: App engineering.
- [ ] Add CI enforcement to verify forbidden Firebase artifacts on every PR.
- [ ] Add runbook section for free-tier auth outage response.
- [ ] Add telemetry event list for OAuth failures (non-billing diagnostics only).
- [ ] Revisit iOS OAuth only if product/compliance scope changes.
