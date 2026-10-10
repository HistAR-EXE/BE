# Phase 4 production rollout runbook

Updated: 2026-10-09  
Scope: backend stabilization worktree, FE companion checkout, Render production, and the audited Railway container configuration.

This is an approval-gated runbook. No production deployment, migration, feature-flag change, metric traffic, or credentialed database connection was performed while preparing it. Do not use `BE/.env.render-db` on a developer machine for this procedure.

## Release boundary

- Deploy only an approved, committed backend revision containing migrations V38–V57 and the matching FE revision.
- Keep `PILOT_CHAT_STREAMING`, `PILOT_GUIDED_MODES`, `PILOT_JOURNEY_MEMORY`, `PILOT_TEACHER_AI_DRAFTS`, `PILOT_NARRATIVE_INVESTIGATION`, and `PILOT_QUEST_CAMPAIGNS` set to `false` for the first production boot.
- Do not enable the Củ Chi narrative pilot: its V52 seed is intentionally `DRAFT` and disabled until a historical-content specialist approves the review workflow.
- Do not run historical content seeds during this rollout. They are a separate, approved content operation.
- The workspace did not contain a provider failed-deploy log. Attach the exact Render/Railway log and deployed revision to the change record before approval.

## 1. Local preflight (read-only)

Run from the repository root. These commands do not contact production:

```powershell
git -C BE/.worktrees/stabilize-phase1 status --short
git -C FE status --short

powershell -NoProfile -ExecutionPolicy Bypass -File BE/.worktrees/stabilize-phase1/scripts/verify-migration-ledger.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File BE/.worktrees/stabilize-phase1/scripts/verify-quest-integrity.ps1 -Environment local

Push-Location BE/.worktrees/stabilize-phase1
$env:HISTAR_DOTENV_ENABLED = 'false'
.\mvnw.cmd test
Pop-Location

Push-Location FE
npx tsc -b --pretty false
npm run lint
Pop-Location
```

Expected gates: migration ledger V38–V57 passes; quest integrity passes; backend tests have zero failures/errors; TypeScript has exit 0; lint has zero errors. Run `npm run build` separately and treat a missing native dependency as an environment failure that must be repaired before promotion.

## 2. Provider deployment

Deploy the exact approved revision through the provider UI/API. Do not deploy from an uncommitted worktree.

For Render, configure the BE service from the BE repository with Dockerfile `Dockerfile`, `SPRING_PROFILES_ACTIVE=prod`, a linked internal `DATABASE_URL`, and secret-store values for the database password and application secrets. Set `DEMO_ENABLED=false` and all six pilot flags false. Confirm the image binds `0.0.0.0:$PORT`.

For Railway, the audited `BE/railway.toml` expects Dockerfile build mode and health check `/api/health/ready` with a 300-second timeout. Set the database URL and secret values in Railway variables; never commit or paste them into a terminal transcript.

Do not mix Render and Railway database or service variables. Record provider name, service name, revision/image digest, deployment time, and the previous healthy revision.

## 3. Database migration (approval required; write operation)

Only after the deployment owner approves the database change, load these variables from the provider secret store or a password manager in the current PowerShell session. Do not put real values in the command history or transcript:

```powershell
$env:PHASE4_DB_URL = 'jdbc:postgresql://<approved-host>:5432/<database>?sslmode=require'
$env:PHASE4_DB_USER = '<approved-read-write-migration-user>'
$env:PHASE4_DB_PASSWORD = '<not-for-transcript>'
```

The values above are placeholders, not values to copy. Run the configured Flyway Maven plugin from the stabilization worktree:

```powershell
Push-Location BE/.worktrees/stabilize-phase1
& .\mvnw.cmd -q `
  "-Dflyway.url=$env:PHASE4_DB_URL" `
  "-Dflyway.user=$env:PHASE4_DB_USER" `
  "-Dflyway.password=$env:PHASE4_DB_PASSWORD" `
  flyway:migrate
if ($LASTEXITCODE -ne 0) { throw 'Flyway migration failed; stop promotion and retain the previous service revision.' }
Pop-Location
```

This applies pending versioned migrations in order. Do not use `flyway clean`, delete rows, edit `flyway_schema_history`, or roll back an applied migration. V53–V57 are required for the Phase 4 audit/release paths.

After migration, run the read-only history audit from a secure, non-transcribed shell, then clear the password variable:

```powershell
Push-Location BE/.worktrees/stabilize-phase1
& .\scripts\audit-flyway-v38-v45.ps1 `
  -DatabaseUrl $env:PHASE4_DB_URL `
  -DatabaseUser $env:PHASE4_DB_USER `
  -DatabasePassword $env:PHASE4_DB_PASSWORD
Pop-Location
$env:PHASE4_DB_PASSWORD = $null
```

The audit must report every Flyway version V38–V57 successful. If the provider cannot run this with secret-backed environment variables, stop and use its secret-backed migration job instead of pasting a password.

## 4. Health and content checks (read-only)

Set the approved public origin without a trailing slash and run:

```powershell
$env:PHASE4_BASE_URL = 'https://<approved-backend-origin>'
$health = Invoke-RestMethod "$env:PHASE4_BASE_URL/api/health"
$ready = Invoke-RestMethod "$env:PHASE4_BASE_URL/api/health/ready"
if ($health.data.status -ne 'UP') { throw "Health is not UP: $($health | ConvertTo-Json -Compress)" }
if ($ready.status -ne 'UP' -or $ready.database -ne 'UP') { throw "Readiness is not UP: $($ready | ConvertTo-Json -Compress)" }
Write-Host 'Health and database readiness passed.'
```

Then run the non-mutating readiness helper against staging before production promotion:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File BE/.worktrees/stabilize-phase1/scripts/verify-staging-readiness.ps1 `
  -BaseUrl $env:PHASE4_STAGING_BASE_URL
```

The helper must pass readiness and a protected/non-public Prometheus scrape. Do not expose `/actuator/prometheus` directly to the browser internet.

## 5. Metric smoke test (approval required for synthetic business events)

First import `observability/grafana-timelens-production-dashboard.json` into the approved Prometheus/Grafana workspace and confirm the staging target is scraping. On staging, use a disposable account to produce exactly one successful and one failing minigame attempt, one quest start/complete path, and one cited guided-chat response. Do not use a real learner account.

Rerun readiness with the metric gate:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File BE/.worktrees/stabilize-phase1/scripts/verify-staging-readiness.ps1 `
  -BaseUrl $env:PHASE4_STAGING_BASE_URL `
  -RequireBusinessMetrics
```

Observe the dashboard for at least 15 minutes. Stop promotion if chat p95 exceeds 10 seconds, fallback exceeds 20%, citation rate is below 80%, quest abandonment exceeds 60% over 24 hours, or minigame failure exceeds 50% for 15 minutes. Production metric traffic is a separate approval-gated action; this audit did not generate it.

## 6. Controlled activation

Promote the same verified revision only after health, migration, content, metric, and FE deployment checks pass. Keep all Phase 4 flags false for the initial boot. If a pilot is later approved, change one flag at a time, record the cohort and start time, then watch its corresponding dashboard panels for 15 minutes before the next flag.

## 7. Rollback

1. Stop promotion and route traffic back to the recorded previous healthy backend revision (Render rollback/deploy previous revision, or Railway redeploy the previous image digest).
2. Confirm `GET /api/health/ready` returns HTTP 200 with `status=UP` and `database=UP`.
3. Keep V53–V57 applied. Do not delete data or alter Flyway history. Correct schema/data issues with a reviewed forward migration or a separately approved database restore plan.
4. If only a pilot path is unhealthy, return its feature flag to `false` and leave unrelated flags unchanged.
5. Record revision, migration versions, error window, health output, dashboard queries/screenshots, and the rollback decision.

## Release record

Record these values before closing the change: approved BE revision, approved FE revision, provider/service, previous revision, Flyway history result V38–V57, readiness result, dashboard observation window, flags, and rollback target. No Phase 4 production rollout is complete until each item has evidence attached.
