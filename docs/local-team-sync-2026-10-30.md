# Team synchronization and local readiness — 2026-10-30

Integrated remote FE `a9ed054` and BE `fb120c1` before committing local work. Preserved the teammate's admin refinements, quest changes, public consultation form, inquiry details and migrations V105–V109. Local work retains camera/PWA, photo frames and saved images, billing history, public branding, readable location URLs and consent updates. No force push or remote history rewrite is required.

Safety snapshots are retained as Git stashes named `safety-before-team-sync-2026-10-30` in FE and BE. The pre-migration database backup is outside Git at workspace `local-db-backups/timelens-before-sync-20261030.dump`. Do not commit database dumps, environment files or runtime logs.

Local PostgreSQL container `histar-postgres`, database `timelens`: Flyway validated 116 migrations and applied V105–V109 successfully. Latest successful version is 109, failed history rows: zero. Other older developer databases and all existing volumes are retained.

Local photo storage: `histar-minio-local`, volume `histar_minio_local`, ports bound to loopback 9000/9001. This separate service does not replace any previous media volumes. Mail and Firebase disabled in the local backend launcher.

Start backend: `powershell -ExecutionPolicy Bypass -File scripts/start-synced-local.ps1`. It uses explicit localhost configuration and does not load a deployment `.env`. Backend startup on port8080 and Hibernate validation passed. Seed check: 10 locations,12 characters,14 photo pairs,14 panoramas,30 hotspots,17 quests,21 badges.

Start FE from the sibling FE checkout: `npm run dev -- --host 127.0.0.1 --port 5173`. Its Vite API proxy targets backend8080. AI integrations still require separately running/configuring the selected local AI provider; no live AI request or paid-provider validation is implied by backend readiness.

Verification after integration: BE full Maven test run exited0 (1046 tests in reports,0 failures/errors,3 skipped); FE TypeScript, production Vite/PWA build, full lint (0 errors,14 existing warnings),50 admin tests,12 photo-flow tests,8 payment/quest checks,readable-route checks,3 editor browser cases and guest consultation browser case passed. The isolated guest test fixture now supplies consent because its normal global setup was absent from the integration test config.
