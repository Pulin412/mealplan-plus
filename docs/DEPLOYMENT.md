# Deployment Runbook

## Architecture

```
Android/iOS apps  →  Spring Boot API (Cloud Run)  →  PostgreSQL (Neon.tech)
                            ↑
                    Firebase JWT validation
                    (JWKS, zero cost, no Admin SDK)
```

**Cost target:** $0–$10/month at personal scale.

---

## One-time setup

### 1. GCP project

```bash
gcloud projects create mealplan-plus-prod
gcloud config set project mealplan-plus-prod
gcloud services enable run.googleapis.com artifactregistry.googleapis.com
```

### 2. Artifact Registry repo

```bash
gcloud artifacts repositories create mealplan \
  --repository-format=docker \
  --location=us-central1
```

### 3. Service account for CI

```bash
gcloud iam service-accounts create github-ci \
  --display-name="GitHub CI"

gcloud projects add-iam-policy-binding mealplan-plus-prod \
  --member="serviceAccount:github-ci@mealplan-plus-prod.iam.gserviceaccount.com" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding mealplan-plus-prod \
  --member="serviceAccount:github-ci@mealplan-plus-prod.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

gcloud projects add-iam-policy-binding mealplan-plus-prod \
  --member="serviceAccount:github-ci@mealplan-plus-prod.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"

# Export key for GitHub secret GCP_SA_KEY
gcloud iam service-accounts keys create gcp-key.json \
  --iam-account=github-ci@mealplan-plus-prod.iam.gserviceaccount.com
```

### 4. Database (Neon.tech — free tier)

1. Create account at https://neon.tech
2. Create project `mealplan-plus`
3. Copy connection string: `postgresql://user:pass@host/db?sslmode=require`
4. Set as `DB_URL` GitHub secret (`jdbc:postgresql://...`)

### 5. Firebase project

1. Reuse existing Firebase project (Android already configured)
2. Note the **Project ID** → set as `FIREBASE_PROJECT_ID` secret

### 6. GitHub secrets

| Secret | Value |
|---|---|
| `GCP_PROJECT_ID` | GCP project ID |
| `GCP_SA_KEY` | Contents of `gcp-key.json` (delete local copy after) |
| `FIREBASE_PROJECT_ID` | Firebase project ID |
| `DB_URL` | `jdbc:postgresql://host/db?sslmode=require` |
| `DB_USER` | Neon.tech DB user |
| `DB_PASSWORD` | Neon.tech DB password |

---

## Deployment flow

```
feature/* branch
      │ PR → develop
      ▼
  develop branch  ──── PR checks CI ────▶ must pass
      │ PR → main (manual sign-off)
      ▼
   main branch  ──── backend-deploy.yml ────▶ Cloud Run
```

- **PR to develop:** `pr-checks.yml` runs (backend test + docker build + Android + billing guardrail)
- **PR to main:** same checks + manual review
- **Push to main:** `backend-deploy.yml` deploys to Cloud Run automatically

---

## Manual deployment (emergency)

```bash
# Build backend JAR
./gradlew :backend:bootJar

# Build + push image
REGION=us-central1
PROJECT=mealplan-plus-prod
IMAGE=$REGION-docker.pkg.dev/$PROJECT/mealplan/mealplan-api

gcloud auth configure-docker $REGION-docker.pkg.dev
docker build -t $IMAGE:manual ./backend
docker push $IMAGE:manual

# Deploy
gcloud run deploy mealplan-api \
  --image $IMAGE:manual \
  --region $REGION \
  --platform managed \
  --allow-unauthenticated \
  --min-instances 0 \
  --max-instances 3 \
  --memory 512Mi \
  --set-env-vars FIREBASE_PROJECT_ID=<id>,DB_URL=<url>,DB_USER=<user>,DB_PASSWORD=<pass>
```

---

## Rollback

```bash
# List recent revisions
gcloud run revisions list --service mealplan-api --region us-central1

# Roll back to previous revision
gcloud run services update-traffic mealplan-api \
  --region us-central1 \
  --to-revisions=mealplan-api-XXXXXXXX=100
```

---

## DB schema management

- Schema is managed by **Hibernate DDL auto** (`DDL_AUTO=update`) — safe for additive changes
- Never set `DDL_AUTO=create` or `DDL_AUTO=create-drop` in production
- For destructive migrations, write a Flyway or Liquibase script and set `DDL_AUTO=none`

---

## Cost monitoring

| Service | Free tier | Action if exceeded |
|---|---|---|
| Cloud Run | 2M req/month, 400k GB-sec | Scale max-instances down |
| Neon.tech | 0.5GB storage, 1 branch | Upgrade to $5/mo plan |
| Artifact Registry | 0.5GB free | Delete old image tags |
| GitHub Actions | 2000 min/month (private) | Optimize workflow triggers |

**Zero-billing guardrails (enforced by `pr-checks.yml`):**
- No `firebase-admin` SDK (use JWKS validation)
- Cloud Run `--min-instances 0` (scale to zero)

---

## Monitoring

```bash
# Live logs
gcloud run services logs tail mealplan-api --region us-central1

# Service URL
gcloud run services describe mealplan-api --region us-central1 --format='value(status.url)'

# Swagger UI (dev only)
open $(gcloud run services describe mealplan-api --region us-central1 --format='value(status.url)')/swagger-ui/index.html
```

---

## Secrets rotation

1. Rotate Neon.tech password → update `DB_PASSWORD` secret in GitHub → redeploy
2. Rotate GCP SA key → create new key → update `GCP_SA_KEY` → delete old key
3. Firebase project ID never changes (tied to app)
