# Staging release runbook

This runbook releases the backend safely to staging and proves the production-stability work before a
production deployment. It deliberately uses read-only database checks and does not put credentials in
source control.

## Required access

- `STAGING_BASE_URL`: HTTPS origin of the staging backend, without a trailing slash.
- Read-only staging DB credentials in `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
  `SPRING_DATASOURCE_PASSWORD`.
- A monitoring-proxy bearer token only if `/actuator/prometheus` is protected externally.
- `psql` installed locally for the Flyway audit.

Never paste these values into an issue, commit, or terminal recording. Set them in the current shell or
the deployment secret store.

## Pre-deploy gate

1. Build and test the backend: `./mvnw.cmd test`.
2. Run `./scripts/verify-migration-ledger.ps1` and `./scripts/verify-quest-integrity.ps1 -Environment local`.
3. Review `docs/MIGRATION_LEDGER_V38_V45.md`; only deploy a build whose database is compatible with its
   recorded V38--V45 sequence.
4. Confirm the deployment environment has `SPRING_PROFILES_ACTIVE=prod`, a valid database URL, required
   AI credentials, allowed CORS origins, and an intentionally chosen `FLYWAY_ENABLED` value.
5. Record the currently deployed image/revision and confirm a rollback target exists before applying the
   new release.

## Staging deployment and verification

1. Deploy the selected image/revision through the hosting provider. Wait for the provider health check
   `/api/health/ready` to become healthy.
2. In a shell containing the DB credentials, audit migrations and content:

   ```powershell
   ./scripts/audit-flyway-v38-v45.ps1 -DatabaseUrl $env:SPRING_DATASOURCE_URL -DatabaseUser $env:SPRING_DATASOURCE_USERNAME -DatabasePassword $env:SPRING_DATASOURCE_PASSWORD
   ./scripts/verify-quest-integrity.ps1 -Environment staging
   ```

3. Validate readiness and the scrape endpoint. Add `-MonitoringBearerToken` only when the monitoring
   proxy requires it:

   ```powershell
   ./scripts/verify-staging-readiness.ps1 -BaseUrl $env:STAGING_BASE_URL -MonitoringBearerToken $env:MONITORING_BEARER_TOKEN
   ```

4. With a designated disposable staging account, complete one successful and one failing minigame attempt,
   start and complete a quest, and send a RAG-enabled chat request that returns a citation. This is the
   only step that intentionally generates business events.
5. Repeat the previous command with `-RequireBusinessMetrics`. It must show all five TimeLens metric
   families. Import `observability/grafana-timelens-production-dashboard.json`, select the staging
   Prometheus data source, and confirm every panel renders data.
6. Add `observability/prometheus-timelens-staging.yml` to the Prometheus configuration, replace the
   placeholder host with the staging host, then validate and reload Prometheus using the platform's normal
   configuration deployment process. Do not expose `/actuator/prometheus` to the public browser internet.

## Release decision

Promote only when the health, Flyway, quest-integrity, scrape, and business-metric gates pass. Observe the
dashboard for at least 15 minutes. Investigate before promotion when chat p95 exceeds 10 seconds, fallback
rate exceeds 20%, citation rate drops below 80%, quest abandonment exceeds 60% over 24 hours, or minigame
failure exceeds 50% for 15 minutes.

## Rollback

1. Stop promotion and route traffic back to the previously recorded, healthy backend revision.
2. Confirm `/api/health/ready` is UP and re-run the non-mutating staging-readiness script.
3. Do **not** roll back the database by deleting rows or changing Flyway history. Forward-fix with a new,
   reviewed migration unless a separately tested database-restoration plan is approved.
4. Capture the release revision, error window, dashboard screenshots/queries, and the decision in the
   incident or deployment record. Disable or reduce traffic to the affected feature if the rollback cannot
   restore a safe experience.
