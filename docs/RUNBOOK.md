# EatMyPlan — Ops Runbook

One page for "it's on fire" and "set up monitoring." Zero-billing posture: everything below is
free-tier. Anything that would incur cost is called out with ⚠️ **COST**.

Product brand = **EatMyPlan**; internal ids stay `mealplan-*` / `com.mealplanplus` (repo, GCP/Firebase
project, Cloud Run service). See [Domains & external MCP connector](#domains--external-mcp-connector).

## Facts
| | |
|---|---|
| Web (Vercel) | `https://eatmyplan.com` (+ `www.eatmyplan.com`; legacy `mealplan-plus.vercel.app` still valid) |
| API (Cloud Run) | `https://api.eatmyplan.com` (custom domain) · default `https://mealplan-api-rfo22lhanq-ez.a.run.app` · service `mealplan-api` · region `europe-west4` |
| Health | `…/actuator/health` (also checks the DB — returns `DOWN` if Neon is unreachable) |
| MCP connector | `https://api.eatmyplan.com/mcp` (Streamable HTTP; OAuth via Stytch; gated by `mcp_server` flag) |
| DB | Neon Postgres (+pgvector), scales to zero on free tier |
| Android | Crashlytics (Firebase console) |
| Logs | Cloud Run → Cloud Logging. Prod logs are **structured JSON**; every line carries `requestId`, `severity`, `service:"mealplan-api"` |
| Deploy | merge `develop→main` with `backend/**` changed → `backend-deploy.yml` builds + deploys + health-gates |

---

## Domains & external MCP connector

DNS is on **Cloudflare** (`eatmyplan.com`, bought Aug 2026). All of the below is **free** — no load
balancer, no paid Stytch tier.

### Domain map
| Host | Points to | Cloudflare record | Notes |
|---|---|---|---|
| `eatmyplan.com`, `www` | Vercel (webapp) | CNAME → Vercel, DNS-only | Production web app |
| `api.eatmyplan.com` | Cloud Run `mealplan-api` | CNAME `api` → `ghs.googlehosted.com`, **DNS-only** | Free Cloud Run **domain mapping** (region `europe-west4` supports it). Google-managed cert, auto-renews. `run.app` URL still works in parallel. |
| `login.eatmyplan.com` | Stytch **Live** | CNAME `login` → `supreme-radius-5538.customers.stytch.com`, **DNS-only** | Stytch custom auth domain (OAuth issuer). Live uses `.stytch.com`. |
| `login-test.eatmyplan.com` | Stytch **Test** | CNAME → `agreeable-cauliflower-1174.customers.stytch.dev` | Old test env (can retire). |
| CAA (apex) | — | `pki.goog`, `letsencrypt.org`, `ssl.com` | `pki.goog` is required for the Google-managed Cloud Run + Google JWKS certs. |

Recreating the Cloud Run mapping: `gcloud domains verify eatmyplan.com` (Search Console TXT) →
`gcloud beta run domain-mappings create --service=mealplan-api --domain=api.eatmyplan.com --region=europe-west4`
→ add the CNAME it prints (DNS-only) → wait ~40 min for the cert.

### External MCP connector ("bring your own Claude")
Exposes our tools over MCP so a user connects **their own** Claude (cost on their subscription).
- **Connector URL:** `https://api.eatmyplan.com/mcp` — **Streamable HTTP** (Spring AI 1.1.0 / MCP SDK
  0.16.0; `spring.ai.mcp.server.protocol=STREAMABLE`). *Not* legacy SSE — Claude speaks Streamable HTTP.
- **Gated** by the DB `mcp_server` feature flag (admin-only toggle; `AdminController`). Flag off → `/mcp` 404s.
- **Auth = OAuth 2.1** (PKCE + Dynamic Client Registration) via **Stytch Connected Apps**, bridged to
  Firebase. `McpAuthFilter` validates the Stytch access token (`StytchTokenService`, RS256/JWKS);
  the token carries `firebase_uid` so tools resolve the same user as the REST API.
- Also accepts a personal HMAC **connector token** (minted by `AdminController`) for Inspector / the
  Messages API — same `/mcp` endpoint, `Authorization: Bearer …`.

### Stytch (OAuth authorization server) — **Live** env
| | |
|---|---|
| Project id (`aud`) | `project-live-00c12304-05e2-4967-aa24-c36531744b9e` |
| Issuer / custom domain | `https://login.eatmyplan.com` (OIDC discovery at `…/.well-known/openid-configuration`) |
| Public token (webapp SDK) | `public-token-live-b07f5e90-86a1-49bc-ad1c-dcf4859705ee` |
| Trusted Auth Token profile | `trusted-token-profile-live-7ec9fec8-a7b9-41c6-89af-62a9bc603b7c` |
| Authorize (consent) URL | `https://eatmyplan.com/authorize` (webapp bridges Firebase→Stytch) |

Trusted Auth Token profile config (Firebase → Stytch bridge): **Audience** `mealplan-plus`,
**Issuer** `https://securetoken.google.com/mealplan-plus`, **JWKS**
`https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com`,
**JIT provisioning ON**, attribute map `email←email`, `token_id←sub`, **`external_user_id←sub`**
(this becomes `user.external_id`), access-token template `{"firebase_uid": {{ user.external_id }}}`
→ backend reads `STYTCH_UID_CLAIM=firebase_uid`.

> ⚠️ **Per-environment gotcha:** each Stytch environment (Test/Live) has its **own** Frontend-SDK
> authorized-domains allowlist (dashboard → SDK Configuration), separate from the authorize URL and
> custom domain. If `eatmyplan.com` isn't listed there, the webapp SDK fails with
> `bad_domain_for_stytch_sdk` (400) and `/authorize` shows "No active session detected."

### Backend env / secrets (Cloud Run, set by `backend-deploy.yml`)
`--set-env-vars` / `--set-secrets` **replace** the whole set each deploy, so all MCP/Stytch config
lives in the workflow (not set by hand):
- Inline env: `STYTCH_ISSUER`/`STYTCH_JWKS_URI` = `login.eatmyplan.com`, `STYTCH_PROJECT_ID` =
  `project-live-…`, `STYTCH_UID_CLAIM=firebase_uid`, `STYTCH_WRITE_SCOPE=openid`.
- Secret Manager: `mealplan-mcp-token-secret` (HMAC connector-token key), `mealplan-admin-emails`
  (admin allowlist). Both granted to `mealplan-deployer@…`.

### Reconnect Claude after any auth change
The backend validates **one** Stytch project at a time, so flipping Test↔Live (or changing the
issuer) breaks the existing connection until you reconnect:
1. claude.ai → Settings → Connectors → **remove** the connector, **re-add** `https://api.eatmyplan.com/mcp`
   (removing forces fresh DCR against the current project).
2. Do it in an **incognito** window (the PWA service worker caches `/authorize`), logged into
   `eatmyplan.com` first.
3. Consent, then confirm a tool call returns your data.

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
