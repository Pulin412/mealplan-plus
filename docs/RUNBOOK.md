# MealPlan+ — Ops Runbook

One page for "it's on fire" and "set up monitoring." Zero-billing posture: everything below is
free-tier. Anything that would incur cost is called out with ⚠️ **COST**.

## Facts
| | |
|---|---|
| API (Cloud Run) | `https://mealplan-api-rfo22lhanq-ez.a.run.app` · service `mealplan-api` · region `europe-west4` |
| Health | `…/actuator/health` (also checks the DB — returns `DOWN` if Neon is unreachable) |
| Web (Vercel) | `https://mealplan-plus.vercel.app` |
| DB | Neon Postgres (+pgvector), scales to zero on free tier |
| Android | Crashlytics (Firebase console) |
| Logs | Cloud Run → Cloud Logging. Prod logs are **structured JSON**; every line carries `requestId`, `severity`, `service:"mealplan-api"` |
| Deploy | merge `develop→main` with `backend/**` changed → `backend-deploy.yml` builds + deploys + health-gates |

---

## 🔥 Rollback (backend) — redeploy the previous revision
The fastest fix for a bad deploy. Cloud Run keeps every revision; just shift 100% traffic back.
No rebuild, takes seconds.

```bash
# 1. List revisions, newest first — find the last-known-good one.
gcloud run revisions list --service=mealplan-api --region=europe-west4 \
  --format='table(metadata.name, status.conditions[0].lastTransitionTime, spec.containers[0].image)'

# 2. Send all traffic to that revision.
gcloud run services update-traffic mealplan-api --region=europe-west4 \
  --to-revisions=<GOOD_REVISION_NAME>=100

# 3. Confirm.
curl -s https://mealplan-api-rfo22lhanq-ez.a.run.app/actuator/health   # -> {"status":"UP"}
```

To return to normal (latest) after fixing: `--to-latest`. Webapp rollback = redeploy the prior
deployment from the Vercel dashboard. Android = re-publish the prior APK from the GitHub Release.

---

## Where to look when something breaks
1. **Is it up?** `curl …/actuator/health`. `DOWN` usually means Neon is unreachable.
2. **Backend errors / a specific request** — Cloud Logging. Filter by severity or by a request's id
   (the API returns it as the `X-Request-Id` response header; ask the reporter to grab it):
   ```
   resource.type="cloud_run_revision" jsonPayload.requestId="<id>"
   severity>=ERROR
   ```
3. **Android crash?** Firebase Console → Crashlytics → filter by app version.
4. **Recent change?** `git log --oneline origin/main` — last merge to `main` is the last deploy.

---

## Monitoring setup (do these for beta — all free, all console actions)

### 1. UptimeRobot — keep-warm + uptime alert  *(highest leverage; free)*
Pings `/actuator/health` from outside every 5 min: keeps one Cloud Run instance (and the Neon
connection, since health checks the DB) warm **and** emails you on downtime.
- uptimerobot.com → free account → **Add New Monitor** → type **HTTP(s)**.
- URL: `https://mealplan-api-rfo22lhanq-ez.a.run.app/actuator/health`, interval **5 min**.
- **Timeout ~30s** and alert only after **2 consecutive** failures — Spring cold starts can be slow;
  this avoids a false "down" on the first cold ping.
- Add an email alert contact.
- Note: this is a *partial* warm-keeper — an instance can still scale down between pings. The only
  guaranteed-warm option is Cloud Run `min-instances=1`, which is ⚠️ **COST** (~$40–60/mo). Not for beta.

### 2. Cloud Monitoring — 5xx alert  *(free)*
Cloud Console → **Monitoring → Alerting → Create Policy**.
- Metric: **Cloud Run Revision → Request Count**, filter `response_code_class = 5xx`, service `mealplan-api`.
- Condition: rolling window count **> a few** over 5 min (tune to taste).
- Notification channel: email. Save.
- (Optional, redundant with UptimeRobot: **Monitoring → Uptime checks** on `/actuator/health`.)

### 3. Crashlytics — confirm it's live  *(free)*
Firebase Console → **Crashlytics**. Confirm events are arriving for the current app version
(**v2.2.3+**) and watch **crash-free users %** per version. If nothing shows: force a test crash on a
debug build, or check the Crashlytics Gradle plugin/mapping upload.

---

## Sentry — enabling later (code is already wired, dormant)
The backend ships the Sentry SDK but sends **nothing** until `SENTRY_DSN` is set (empty DSN = SDK
disabled). Environment is `prod` in prod, and `send-default-pii=false` / `traces-sample-rate=0` keep
it free-tier and PII-free (errors only, no headers/IP, no performance sampling).

To turn it on:
1. sentry.io → free account → new **Spring Boot** project → copy the **DSN**.
2. ⚠️ **Gotcha:** `backend-deploy.yml` uses `--set-env-vars` / `--set-secrets`, which **replace** the
   whole env set on every deploy. A DSN set by hand in the console gets **wiped on the next deploy**.
   Do it durably instead:
   ```bash
   # a. Store the DSN in Secret Manager.
   printf '%s' '<YOUR_SENTRY_DSN>' | gcloud secrets create mealplan-sentry-dsn --data-file=-
   # b. Grant the deploy SA access (same SA the workflow uses).
   gcloud secrets add-iam-policy-binding mealplan-sentry-dsn \
     --member="serviceAccount:mealplan-deployer@<PROJECT_ID>.iam.gserviceaccount.com" \
     --role="roles/secretmanager.secretAccessor"
   ```
   Then append `SENTRY_DSN=mealplan-sentry-dsn:latest` to the `--set-secrets=` line in
   `backend-deploy.yml` and merge to `main`. (This is a deploy-config change — get sign-off first.)
3. Verify: after deploy, trigger a handled 500 and confirm it lands in Sentry.

---

## Deferred (post-beta, not needed yet)
Request-id/MDC correlation and JSON logging are **done**. Not yet built: Cloud Run/Neon dashboards
(latency p95, error rate, cold-start freq), Neon PITR check, Vercel Analytics. Discuss dashboards
before building — see the observability plan.
