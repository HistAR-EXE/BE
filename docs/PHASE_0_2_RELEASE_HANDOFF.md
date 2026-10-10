# Phase 0–3 release handoff

Updated: 2026-10-09

## Completed locally

- Migration ledger, audit scripts, and runbook now cover V38–V57.
- Guided chat streaming, citation-safe RAG, feature flags, resilience metrics, and golden-set checks are implemented.
- Narrative progress persists both the compatibility `state_json` and queried version/visited/evidence/outcome fields.
- Reward awards serialize on the profile row before writing the unique reward ledger.
- Specialist review API/audit trail and the review-ready Củ Chi investigation graph are implemented.
- Backend regression reports contain no Surefire failure/error entries for the latest full run.

## Required before public release

1. Provide and audit a PostgreSQL staging environment distinct from Render production:
   `STAGING_BASE_URL`, monitoring bearer token, and read-only database credentials.
2. Import the Grafana dashboard, configure the documented alerts, produce staging smoke traffic, and rerun readiness with `-RequireBusinessMetrics`.
3. Run and resolve the complete frontend E2E suite using the AI/SePay harness.
4. Run the narrative integration/concurrency suite against the PostgreSQL fixture.
5. A designated historical-content specialist must review the Củ Chi sources, translations, outcome wording and rewards through the admin review workflow. The seed remains `DRAFT` and disabled until then.
6. Commit only after all preceding verification gates pass.

## Safety boundary

`BE/.env.render-db` and Render production credentials are never a substitute for staging and remain out of scope for staging audit.
