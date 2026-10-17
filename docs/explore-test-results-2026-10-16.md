# Explore verification — 2026-10-16

## Verified results

| Layer | Result | Evidence |
| --- | --- | --- |
| Backend regression | 344 tests, 107 suites; zero failures, errors or skipped tests | `target/surefire-reports/`, `target/explore-verification-current.log` |
| Browser contract tests | 8 passed; zero unexpected, skipped or flaky cases | `../FE/test-results/explore/results.json` |
| Live Edge / API / PostgreSQL | 26 checks passed | `../FE/test-results/explore-live/results.json` |
| Frontend build | Success; existing large-bundle warning remains | `../FE/explore-build-current.log` |
| Migration preservation | Fresh and Hibernate-created schema scenarios passed, including V69 replay | `target/explore-migrations-current.log` |

## Live coverage

- Real authentication API with separate verified QA accounts, real backend on port 8080 and frontend on port 5173. No registration emails were sent. UI sessions use tokens returned by the real login API; typing through the login form was not part of this suite.
- Unauthenticated access denied; invalid location/answer rejected; raw reward-bearing discovery keys rejected.
- Wrong answer then correct retry; one-time award on replay; concurrent identical and distinct completions.
- Four stored clues required before conclusion; durable clue/conclusion rewards; cipher and dossier rewards; diorama receipt without XP.
- Throttle after 12 incorrect attempts; no XP granted for wrong/throttled answers.
- Reload, cleared localStorage followed by restoring a valid login session, forged browser XP ignored, account isolation, Profile/Explore agreement.
- Real UI wrong answer, interrupted save request, recovery and successful server-confirmed award. The interrupted request is intentionally aborted by Playwright; all successful requests use the real backend.
- Mobile badge dialog focus, Tab containment and Escape; no uncaught JavaScript errors; PostgreSQL profile totals equal ledger sums for each QA account.
- Screenshots: `../FE/test-results/explore-live/desktop.png`, `mobile-badge.png`.

## Test reliability findings

An initial contract-suite navigation timed out waiting for the complete page load. Navigation now waits for DOMContentLoaded, followed by assertions against the actual rendered XP/completion state. All eight tests passed afterwards. Artifact directories are separated so this suite does not erase live-test results.

A live-suite retry exceeded the default five-second visibility assertion while the backend suite and build were running. The live harness now allows fifteen seconds and captures a screenshot/body on failure. The final isolated live run passed all 26 checks. No product code was changed to bypass a failing assertion.

## Scope limits

These results apply to the local configuration and the cases above. They are not a guarantee against every possible vulnerability or data-loss scenario. External AI dialogue completion, GPS/check-in on a physical device, production deployment configuration, sustained load and full backup restoration were not exercised end-to-end in this run. Backend regression tests cover related server logic where present.

The main database was not reset, and personal profiles were not edited by the tests. Migration verification uses a temporary schema and rolls back. Dedicated QA accounts and their dependent local test data were cleaned up after verification.

## Repeat

From `FE`: `npx playwright test --config playwright.explore.config.ts`; `npm run build`.

From `BE`: `.\mvnw.cmd test -q`; `.\scripts\verify-explore-migrations.ps1`.

Optional live harness from `FE`: `node scripts/test-explore-live.cjs`. This intentionally creates QA fixtures in the local database; fixture UUIDs are written to its result JSON for scoped cleanup. Credentials and tokens are never written to the report.
