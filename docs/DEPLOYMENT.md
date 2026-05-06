# MealPlan+ — Deployment & Services Reference

> Last updated: May 3, 2026  
> All services are live and production-ready as of Phase 3c.

---

## Live Service URLs

| Service | URL | Notes |
|---|---|---|
| **Backend API** | `https://mealplan-api-rfo22lhanq-ez.a.run.app` | Cloud Run, europe-west4 |
| **Health check** | `https://mealplan-api-rfo22lhanq-ez.a.run.app/actuator/health` | Returns `{"status":"UP"}` |
| **Swagger UI** | `https://mealplan-api-rfo22lhanq-ez.a.run.app/swagger-ui/index.html` | API docs |
| **Web App** | `https://mealplan-plus.vercel.app` | Vercel, Next.js PWA |
| **Web App Login** | `https://mealplan-plus.vercel.app/login` | Firebase Auth (Google + email) |

---

## Architecture

```
Android app  ──┐
               ├──▶  Spring Boot API (Cloud Run)  ──▶  PostgreSQL (Neon.tech)
Web PWA      ──┘           │
                    Firebase JWT validation
                    (JWKS endpoint, zero cost)
```

**Auth flow:** Firebase issues a JWT → client sends it as `Authorization: Bearer <token>` → backend validates via JWKS → returns data scoped to `firebaseUid`.

---

## 1. Backend — Google Cloud Run

| Property | Value |
|---|---|
| Service name | `mealplan-api` |
| Region | `europe-west4` (Netherlands) |
| GCP Project | `mealplan-plus` |
| Image registry | `europe-west4-docker.pkg.dev/mealplan-plus/mealplan/mealplan-api` |
| Runtime SA | `mealplan-deployer@mealplan-plus.iam.gserviceaccount.com` |
| Min instances | 0 (scales to zero, ~5-10s cold start) |
| Max instances | 3 |
| Memory | 512 Mi |
| CPU | 1 |
| Port | 8080 |

### Environment variables (set at deploy time)
| Variable | Value / Source |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `docker` |
| `FIREBASE_PROJECT_ID` | GitHub secret `FIREBASE_PROJECT_ID` |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `DDL_AUTO` | `validate` |
| `DB_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |
| `DB_URL` | Secret Manager → `mealplan-db-url` |
| `DB_USER` | Secret Manager → `mealplan-db-user` |
| `DB_PASSWORD` | Secret Manager → `mealplan-db-password` |

### Useful commands
```bash
# Get live URL
gcloud run services describe mealplan-api --region=europe-west4 --format='value(status.url)'

# Tail live logs
gcloud run services logs tail mealplan-api --region=europe-west4

# List revisions
gcloud run revisions list --service=mealplan-api --region=europe-west4

# Roll back to previous revision
gcloud run services update-traffic mealplan-api \
  --region=europe-west4 \
  --to-revisions=mealplan-api-XXXXXXXX=100

# Manual redeploy (emergency)
IMAGE=europe-west4-docker.pkg.dev/mealplan-plus/mealplan/mealplan-api
gcloud run deploy mealplan-api \
  --image=$IMAGE:latest \
  --region=europe-west4 \
  --platform=managed \
  --allow-unauthenticated
```

### Deploy trigger
Automatic via `backend-deploy.yml` on **PR merge to `main`** when `backend/**` files change. Includes build + test + Docker push + health check verification.

---

## 2. Database — Neon.tech (PostgreSQL + pgvector)

| Property | Value |
|---|---|
| Provider | [neon.tech](https://neon.tech) |
| Plan | Free tier (0.5 GB storage) |
| Region | EU Central 1 (Frankfurt) |
| Database | `neondb` |
| Extensions | `pgvector` (enabled via V2 migration) |
| SSL | Required (`sslmode=require`) |
| Schema management | Flyway migrations (`V1__init.sql` → current) |

### Connection string format
```
jdbc:postgresql://ep-XXXX.eu-central-1.aws.neon.tech/neondb?sslmode=require
```
_(actual credentials stored in GCP Secret Manager — never in code)_

### Secret Manager secrets
| Secret name | Contains |
|---|---|
| `mealplan-db-url` | Full JDBC connection string |
| `mealplan-db-user` | DB username |
| `mealplan-db-password` | DB password |

### Schema
- **V1** — full initial schema (users, foods, meals, diets, grocery, health_metrics, daily_logs, tombstones, etc.)
- **V2** — pgvector extension + `entity_embeddings` table with HNSW index
- **V3** — exercises, workout_sessions, workout_sets
- **V4** — day_plans
- **V5** — user profile columns, is_favorite on foods
- **V6** — workout_templates, template_exercises

### Useful queries
```sql
-- Check Flyway migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- Check row counts per table
SELECT schemaname, tablename, n_live_tup
FROM pg_stat_user_tables ORDER BY n_live_tup DESC;

-- Reset schema (DANGER — only on dev/fresh DB)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

---

## 3. Web App — Vercel

| Property | Value |
|---|---|
| Provider | [vercel.com](https://vercel.com) |
| Plan | Hobby (free) |
| Production URL | `https://mealplan-plus.vercel.app` |
| Git integration | GitHub → `Pulin412/mealplan-plus`, branch `main` |
| Root directory | `webapp/` |
| Framework preset | Next.js |
| Region | Edge (Frankfurt / fra1) |

### Environment variables (set in Vercel dashboard)
| Variable | Value |
|---|---|
| `NEXT_PUBLIC_FIREBASE_API_KEY` | Firebase project API key |
| `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN` | `mealplan-plus.firebaseapp.com` |
| `NEXT_PUBLIC_FIREBASE_PROJECT_ID` | `mealplan-plus` |
| `NEXT_PUBLIC_FIREBASE_APP_ID` | Firebase app ID |
| `NEXT_PUBLIC_API_BASE_URL` | `https://mealplan-api-rfo22lhanq-ez.a.run.app` |

### Deploy trigger
Auto-deploys when `main` branch is updated in Vercel's GitHub integration. Only the `webapp/` subdirectory is built.

### Firebase authorized domains
These domains are added to Firebase Console → Authentication → Settings → Authorized domains:
- `mealplan-plus.vercel.app`
- `mealplan-plus-git-main-pulins412-4817s-projects.vercel.app`

---

## 4. GitHub Actions — CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | Push to `develop` | Build + test: Android (assembleDebug), Backend (build+test+Docker check), Webapp (tsc+lint+build) — path-filtered |
| `backend-deploy.yml` | PR merged → `main`, `backend/**` changed | Build → push Docker image → Cloud Run deploy → health check → prune old images |

### GitHub Secrets required
| Secret | Purpose |
|---|---|
| `GCP_PROJECT_ID` | GCP project identifier |
| `GCP_SA_KEY` | Base64-encoded service account JSON (deployer SA) |
| `FIREBASE_PROJECT_ID` | Firebase project ID for backend auth |

---

## 5. Cost Summary

| Service | Free tier | Current usage |
|---|---|---|
| Cloud Run | 2M req/month, 400k GB-sec compute | Personal scale — well within free tier |
| Neon.tech | 0.5 GB storage | ~few MB (early stage) |
| Artifact Registry | 0.5 GB | Pruned to last 3 images (~200 MB each) |
| Vercel Hobby | 100 GB bandwidth/month | Personal scale — free |
| Firebase Auth | 50k MAU free | Personal use |
| GCP Secret Manager | 6 active secrets free | 3 secrets in use |

**Expected monthly cost: $0** at current personal scale.

---

## 6. Rollback Procedures

### Backend rollback
```bash
# List revisions with traffic
gcloud run revisions list --service=mealplan-api --region=europe-west4

# Send 100% traffic to a previous revision
gcloud run services update-traffic mealplan-api \
  --region=europe-west4 \
  --to-revisions=mealplan-api-00005-abc=100
```

### Webapp rollback
Vercel dashboard → Deployments → find the previous good deployment → **⋯ → Promote to Production**.

### Database rollback
No automatic rollback — Flyway migrations are forward-only. For emergencies:
1. Restore from Neon.tech's point-in-time recovery (available on paid plans)
2. Or revert the migration SQL manually (only safe if no new data was written)

---

## 7. Secrets Rotation

| Secret | How to rotate |
|---|---|
| Neon DB password | Neon console → reset password → update Secret Manager `mealplan-db-password` → redeploy backend |
| GCP SA key | `gcloud iam service-accounts keys create` → update `GCP_SA_KEY` in GitHub → delete old key |
| Firebase API key | Firebase console → regenerate → update Vercel env vars → redeploy webapp |
