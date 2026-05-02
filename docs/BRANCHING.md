# MealPlan+ — Branching Strategy

## Model

```
feature/xyz
    └── PR → develop        CI runs (build + test gate)
                └── PR → main       CI runs (build + test gate)
                              └── Merge → Cloud Run deploys 🚀
```

No code ever reaches production without passing CI twice and going through a PR review.

---

## Branches

| Branch | Purpose | Who pushes |
|---|---|---|
| `main` | Production — mirrors what is deployed to Cloud Run / Vercel | Nobody directly — PRs only |
| `develop` | Integration — stable working state, all features land here first | Nobody directly — PRs only |
| `feature/*` | Individual features — one branch per GitHub issue | Developer |
| `fix/*` | Bug fixes | Developer |
| `hotfix/*` | Emergency production fixes (bypass develop, straight to main) | Developer — rare |

---

## Normal workflow

### 1. Start a feature

Always branch from `develop`, never from `main`:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/phase3c-deploy-ios-pwa
```

Name the branch after the GitHub issue or phase:
- `feature/phase3c-deploy-ios-pwa`
- `feature/workout-backend-sync`
- `fix/drive-backup-403`

### 2. Work on the branch

Commit freely. Push to remote whenever you want:

```bash
git push -u origin feature/phase3c-deploy-ios-pwa
```

CI does **not** run on `feature/*` pushes — only on PRs and pushes to `develop`/`main`.

### 3. Open a PR → develop

When the feature is ready:
- Open a PR from `feature/xyz` → `develop` on GitHub
- CI runs automatically (build + unit tests for changed modules)
- PR cannot merge if CI is red
- Merge using **Squash and merge** to keep `develop` history clean

### 4. Open a PR → main (release)

When `develop` has a set of features ready to ship:
- Open a PR from `develop` → `main`
- CI runs again as a final gate
- On merge: `backend-deploy.yml` fires automatically → Cloud Run deploys

---

## CI behaviour per event

| Event | `ci.yml` runs? | `backend-deploy.yml` runs? |
|---|---|---|
| Push to `feature/*` | ❌ | ❌ |
| Push to `develop` | ✅ build + test | ❌ |
| Push to `main` | ✅ build + test | ❌ |
| PR opened/updated → `develop` | ✅ build + test | ❌ |
| PR opened/updated → `main` | ✅ build + test | ❌ |
| PR **merged** → `main` | ✅ | ✅ **deploy** |
| PR closed (not merged) → `main` | ❌ | ❌ |

The deploy guard in `backend-deploy.yml`:
```yaml
if: github.event.pull_request.merged == true
```
Closing a PR without merging never triggers a deploy.

---

## Path filtering in CI

`ci.yml` only runs the relevant build job based on what files changed:

| Files changed | Job that runs |
|---|---|
| `android/**` or `shared/**` | Android build + unit tests |
| `backend/**` | Backend build + unit tests + Docker build check |
| `ios/**` | iOS build (macOS runner) |
| `.github/workflows/ci.yml` | All jobs |

A webapp-only change triggers no build jobs (webapp has its own Vercel build).

---

## Hotfix workflow

For urgent production bugs that can't wait for the normal develop→main cycle:

```bash
git checkout main
git pull origin main
git checkout -b hotfix/fix-critical-bug

# make the fix, commit

git push -u origin hotfix/fix-critical-bug
# open PR directly → main
# merge → Cloud Run auto-deploys

# back-merge into develop so it stays in sync
git checkout develop
git merge main
git push origin develop
```

---

## Commit message convention

```
feat(scope): short description       ← new feature
fix(scope): short description        ← bug fix
docs: short description              ← documentation only
ci: short description                ← CI/CD changes
refactor(scope): short description   ← no behaviour change
test(scope): short description       ← tests only
```

Examples:
```
feat(backup): add GZIP compression to Drive upload
fix(auth): persist Firebase UID at login to prevent stale session
ci: deploy only on develop→main PR merge
docs: add branching strategy
```

All commits include:
```
Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

---

## Branch protection rules (set in GitHub)

Recommended settings for both `main` and `develop`:
- ✅ Require a pull request before merging
- ✅ Require status checks to pass (select the `ci.yml` jobs)
- ✅ Require branches to be up to date before merging
- ✅ Do not allow bypassing the above settings
- ❌ Allow force pushes — **never**
