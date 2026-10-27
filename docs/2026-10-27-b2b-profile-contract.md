# B2B and profile contract (2026-10-27)

Organization Premium is derived from an accepted membership, verified email and an active organization plan. Personal purchased Premium is independent and is not overwritten by organization membership. Micro has basic member/progress management; Standard/Premium add assignments, Live Board, report snapshots and group access. Account/CCU/monthly AI limits remain pooled per plan (100/15/5000, 400/40/30000, 1000/80/unlimited).

Email invitations bind the exact verified email and are accepted at authentication. Only an owner can provision a teacher. Server-side capabilities protect every privileged operation; client links are not authorization. Membership changes lock affected profile and organization rows. CCU admission cannot block own-profile information or leaving an organization.

GET /api/profile/overview uses the authenticated identity only. It returns global distinct counts, the complete location catalog, passport events, provider/account creation date and personal-vs-organization entitlement sources. Passport dates stay unknown when evidence is missing; QR verification requires a QR check-in and station code. PATCH /api/profile/me accepts normalized displayName/city/avatarUrl, never role/tier; empty optional strings clear values.

## Migration sequence

- V95__user_creations_heritage_context.sql: upstream Heritage context.
- V96__organization_email_invitations.sql: exact-email invitations.
- V97__org_report_snapshots.sql: persisted reports.

V96/V97 were numbered to avoid the upstream V95 collision before publication. Do not renumber migrations already used in shared environments. The local pre-publication database was backed up and its two B2B history entries reconciled separately; no general repair/reset is required for databases following the published sequence.

Regenerate bootstrap with docs/scripts/New-HistarFullBootstrapSql.ps1. Validate ordering with docs/scripts/bootstrap-local-db.ps1 -ValidateOnly. Bootstrap only a new empty local database; never reset a shared database. No credentials or runtime dumps belong in Git.

Tests cover entitlements, quotas, invitations, teacher provisioning, persisted snapshots, membership concurrency, passport/profile accuracy and update validation. Run targeted Maven tests with JDK21; full local verification also exercises the FE browser smoke scripts. Local email tests use MAIL_ENABLED=false and synthetic accounts.
