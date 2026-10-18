# Lifetime Honors release review — 19 October 2026

## Integration and data safety

Integrated incoming team master `50282d4` (BE) and `e36e0a1` (FE) without replacing teammate features. A Git bundle, binary working diff, untracked-file archive, safety ref, and full local PostgreSQL dump were saved before integration. Backups and credentials remain outside the Git changes.

Canonical team migrations V74–V79 are preserved. Explore uses V80/V81 and passport email uses V82. The narrowly guarded local history reconciliation was tested on a restored database first, then applied locally after another full backup. Normal Flyway validation confirms 83 migrations through V82. Full fresh bootstrap through V82 also passed on a disposable database, including passport foreign key, image/state checks, idempotency constraint and quota index. See `honors-migration-integration-2026-10-19.md` for provenance and protected-data fingerprints.

Removed only the 18 explicitly marked `ui-preview-honors` identities and their preview-owned rows. Protected XP/levels, ledger, discoveries, earned badges, Explore attempts and passport attempts match the recorded fingerprints. Preview identities, disposable databases and passport test schemas are now zero. Team platform/demo/admin seeds were preserved.

## Review fixes

- Honor eligibility now excludes normalized suspended accounts consistently with the team's new moderation rules; active/null/blank/legacy unknown statuses retain their existing eligibility.
- Passport image loading no longer sends avatar URLs, including signed URLs, to a third-party image proxy.
- PNG downloading remains available while email is pending. Unprepared exports no longer claim to be ready.
- Rules and refresh controls have explicit accessible names. Long exported names/email fit beside the decorative stamp.
- UI tests follow the current automatic-image, two-action design. The intentionally retired next-step card remains hidden; timeline and independent spotlight recognition are still tested.

## Verification

- Full merged BE suite: 545 cases, zero failures/errors; the one opt-in PostgreSQL case was skipped by default and then passed separately against a random schema. SMTP capture tests are loopback only, not external delivery.
- Three PostgreSQL SQL harnesses passed, including XP ties, badge-only display ordering, Top 10 cutoff, viewer outside Top 10, suspended accounts, group boundaries and unchanged XP. Both Explore migration scenarios passed with rollback.
- Live local API: nine groups passed, including Free/Premium parity, owner isolation, access control, private passport/email status, stories, read-only progression, and cleanup with data fingerprints unchanged. No external email sent.
- FE source admin suite: 42 cases passed. Final FE lint/build passed with zero errors; 18 existing warnings outside Honors and large shared chunk warnings remain.
- Final Honors UI suite: 35/35 passed on Edge, including automatic PNG, byte-identical email/download, privacy, suspended-profile API integration, Top 10/ties, mobile, keyboard, and stale account responses. Final related Explore suite: 15/15 passed; its fixture marks the introductory product tour complete to isolate the intended controls. Exported PNG was visually inspected after bounding identity text beside the stamp.

## Production rollout

The owner confirmed these repositories do not auto-deploy on master push. Publishing code does not apply production migrations. The deployment operator must migrate the target database through V82 before starting the new backend: production disables Flyway at boot and validates the schema. Do not run local bootstrap/reset scripts on production or use blanket Flyway repair.

Email still requires valid provider secrets and authorization of `TimeLens <histar.timelens@gmail.com>`. Automated capture and payload tests do not establish production sender authorization or Inbox delivery. No production database, mail configuration or deployment was changed during this review.
