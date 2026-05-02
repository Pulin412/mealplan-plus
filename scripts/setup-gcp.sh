#!/usr/bin/env bash
# =============================================================================
# MealPlan+ — GCP Cloud Run setup script (#108)
#
# Run this ONCE to configure everything needed for backend-deploy.yml.
# Prerequisites:
#   - gcloud CLI installed and authenticated (gcloud auth login)
#   - Your Neon.tech DB credentials ready
#
# Usage:
#   chmod +x scripts/setup-gcp.sh
#   GCP_PROJECT_ID=your-project-id ./scripts/setup-gcp.sh
# =============================================================================

set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
PROJECT_ID="${GCP_PROJECT_ID:?Set GCP_PROJECT_ID env var}"
REGION="europe-west4"
SA_NAME="mealplan-deployer"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
REPO_NAME="mealplan"

echo "==> Project: $PROJECT_ID | Region: $REGION"

# ── Enable APIs ───────────────────────────────────────────────────────────────
echo "==> Enabling required APIs..."
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  cloudbuild.googleapis.com \
  --project="$PROJECT_ID"

# ── Artifact Registry repo ────────────────────────────────────────────────────
echo "==> Creating Artifact Registry repository..."
gcloud artifacts repositories create "$REPO_NAME" \
  --repository-format=docker \
  --location="$REGION" \
  --description="MealPlan+ Docker images" \
  --project="$PROJECT_ID" 2>/dev/null || echo "  (already exists, skipping)"

# ── Service account ───────────────────────────────────────────────────────────
echo "==> Creating service account: $SA_EMAIL"
gcloud iam service-accounts create "$SA_NAME" \
  --display-name="MealPlan+ GitHub Actions deployer" \
  --project="$PROJECT_ID" 2>/dev/null || echo "  (already exists, skipping)"

# Grant required roles
for ROLE in \
  roles/run.admin \
  roles/artifactregistry.writer \
  roles/iam.serviceAccountUser \
  roles/secretmanager.secretAccessor; do
  echo "  granting $ROLE..."
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA_EMAIL" \
    --role="$ROLE" \
    --quiet
done

# ── Export SA key (for GCP_SA_KEY GitHub secret) ──────────────────────────────
KEY_FILE="/tmp/mealplan-sa-key.json"
echo "==> Exporting SA key to $KEY_FILE"
gcloud iam service-accounts keys create "$KEY_FILE" \
  --iam-account="$SA_EMAIL" \
  --project="$PROJECT_ID"

echo ""
echo "==> Base64-encode this key and add it as the GCP_SA_KEY GitHub secret:"
echo ""
base64 -i "$KEY_FILE"
echo ""
echo "  (key also saved to $KEY_FILE — delete it after adding to GitHub)"

# ── Secret Manager secrets ────────────────────────────────────────────────────
echo ""
echo "==> Creating Secret Manager secrets (you will be prompted to enter values)..."

create_secret() {
  local name="$1"
  local prompt="$2"
  echo -n "  Enter value for $name ($prompt): "
  read -rs value
  echo ""
  echo -n "$value" | gcloud secrets create "$name" \
    --data-file=- \
    --project="$PROJECT_ID" 2>/dev/null || \
  echo -n "$value" | gcloud secrets versions add "$name" \
    --data-file=- \
    --project="$PROJECT_ID"
  echo "  Secret '$name' saved."
}

create_secret "mealplan-db-url"      "jdbc:postgresql://<host>/<db>?sslmode=require"
create_secret "mealplan-db-user"     "Neon.tech DB username"
create_secret "mealplan-db-password" "Neon.tech DB password"

echo ""
echo "==> Done! Next steps:"
echo ""
echo "  1. Add these GitHub secrets at:"
echo "     https://github.com/Pulin412/mealplan-plus/settings/secrets/actions"
echo ""
echo "     GCP_PROJECT_ID   = $PROJECT_ID"
echo "     GCP_SA_KEY       = <base64 output above>"
echo "     FIREBASE_PROJECT_ID = mealplan-plus"
echo ""
echo "  2. Push any change to backend/ on main to trigger the deploy."
echo "  3. Cloud Run service 'mealplan-api' will appear in:"
echo "     https://console.cloud.google.com/run?project=$PROJECT_ID"
