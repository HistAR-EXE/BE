# Ảnh & chia sẻ — local delivery, 2026-10-28

User-approved flow: one Account entry and one canonical /photo-frame screen with Tạo ảnh → Xem lại & lưu → Tải & chia sẻ. /share links redirect, retaining creation/station/source context and discarding outputUrl query input.

## Implemented
- Bright blue/white interface aligned with Profile/Settings, desktop editing controls and preview, responsive mobile layout and three-step status.
- Original and published Heritage modes retained. Review happens locally before upload; returning to edit preserves saved output and restores owner-scoped IndexedDB draft when available.
- Original photo export now prepares the actual 1080 square/story file, applies light grading and chosen frame, and previews/uploads the same bytes. Broken frame blocks save with a useful error instead of uploading a different image.
- Owner-scoped saved-photo lookup/download, verified image format and matching filename, editable caption, native share, download and clipboard tools. Slow caption suggestions do not overwrite typing.
- Download, copy, native cancellation and unsupported-share download do not report a share or award XP. Successful native sharing or explicit self-declared manual confirmation uses the existing idempotent record-share API. External posting is not independently verified.
- Library image metadata restores station return even without original URL context. Camera safety at ST06, private Heritage drafts and offline queue retained.

## Verification
- 20/20 helper/profile/settings/teacher regression tests.
- 39/39 Heritage regression tests.
- 7/7 Playwright UI/privacy/mobile tests, including generated-composite-only upload and ST06 library-only camera safety.
- Browser integration: eight scenario groups covering create/review/save, cancel/download/native/fallback sharing, edit restoration, mobile width, legacy URL safety, missing/foreign creation, API retry, station metadata and delayed caption.
- Original-photo browser pixel regression checks crop dimensions, grade, frame overlay and failed-frame rejection.
- Lint, TypeScript and production Vite bundle checked. Existing prebuild media-network checks were not rerun because no media catalog or asset changes were made.

Browser tests use synthetic pictures and mocked upload/storage/social APIs; login/profile endpoints were checked against the local backend. No external photo upload or social post was performed. This is not a claim of live cloud-storage/social end-to-end verification.

## Additional local defect
Real authenticated traffic exposed expired CCU-session reactivation: a bulk stale-session delete removed the currently managed entity before Hibernate updated it, leading to request failure and login redirect. Fixed by refreshing the current session in place, deleting stale peer sessions only within the same organization lock, and routing heartbeats through the same admission checks. RED reproduced the old stale-row failure; GREEN passed 7 focused H2/JPA CCU/auth tests.

Includes the removal of retired journey/group/squad features. Implementation and repository publication are tracked separately in Git history.

## Repeat checks
From FE: npm run test:photo-flow; npm run test:photo-flow:ui; npm run test:photo-flow:browser. Browser checks expect local Vite on localhost:5173 and the seeded teacher-standard local account. Storage/network fixtures are scoped to HTTP /api paths and do not intercept source modules.


## Verified running local environment
Backend repackaged successfully and restarted on port18080 (PID36688 at handover); frontend Vite on localhost:5173 proxies to it. MAIL_ENABLED=false. PostgreSQL real-data smoke artificially expired only the seeded teacher-standard active-session timestamp, then all six simultaneous authenticated profile requests returned200. Runtime log: docs/photo-flow-backend.stdout.log; evidence: docs/photo-flow-ccu-postgres-smoke.log. No schema migration was added by this feature.
