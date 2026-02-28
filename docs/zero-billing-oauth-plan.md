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

## 6) SHA-1 Fingerprint Management

Google OAuth validates requests against SHA-1 fingerprints registered in Firebase.
**All three must be registered** — missing any one breaks OAuth for that build type.

| Fingerprint | Purpose | How to get |
|---|---|---|
| **Debug** | Local dev/testing | `./gradlew signingReport` → `Variant: debug` |
| **Release keystore** | Signed APK/AAB you upload | `./gradlew signingReport` with release signing config |
| **Play App Signing** | What users actually install (Google re-signs) | Play Console → Release → Setup → App signing → "App signing key certificate" SHA-1 |

**Current debug SHA-1:** `A8:5B:C7:99:69:02:E0:8B:CF:AB:21:38:73:C8:30:F2:6F:BB:34:66`

**Steps to add a new fingerprint:**
1. Firebase Console → Project Settings → Your Android app → Add fingerprint
2. Paste SHA-1 → Save
3. Re-download `google-services.json` → replace `app/google-services.json` → commit

**Play App Signing note:** Google Play re-signs the APK before distributing to users.
The SHA-1 on users' devices is Google's key, not the release keystore's.
Get it from Play Console **after** first upload — it cannot be known before.
Without it, OAuth will work in dev/internal testing but fail for all public users.

**When to update this:**
- New developer joins → add their debug SHA-1
- Release keystore created → add release SHA-1
- First Play Store upload → add Play App Signing SHA-1

## 7) Release Gates / Checklist
- [ ] Spark plan confirmed and no billing account attached.
- [ ] Forbidden Firebase products not present in dependencies/config.
- [ ] Android Google OAuth path tested (new + returning user).
- [ ] Local email/password path regression-tested.
- [ ] iOS release build has no OAuth entry point.
- [ ] User-facing error states verified (cancel/config/rate-limit style failures).

## 8) Incident / Degradation Behavior
- On OAuth cancellation: show cancellation message and remain on login screen.
- On config errors (missing client ID/Firebase config): show actionable message; do not crash.
- On temporary auth outages/rate limits: show retry-later message; keep local auth available.

## 9) Ownership and Resume TODOs
- Owner: App engineering.
- [ ] Add CI enforcement to verify forbidden Firebase artifacts on every PR.
- [ ] Add runbook section for free-tier auth outage response.
- [ ] Add telemetry event list for OAuth failures (non-billing diagnostics only).
- [ ] Revisit iOS OAuth only if product/compliance scope changes.
