# Explore reward integrity — 2026-10-16

Approved scope: the Explore dashboard shows global account XP and account level. PostgreSQL owns completion, rewards and badges. Browser-only legacy progress is not imported; existing database progress is preserved.

Implementation:
1. Reproduce the summary contract mismatch (`keys` vs `unlockedKeys`) and protect completion keys from the generic discovery API.
2. Grade archive challenges on the server; require persisted clues before conclusion. Record attempts, enforce a cooldown, and award a completion only once in a transaction.
3. Serialize reward decisions per account. Persist an XP journal, preserving old totals as a baseline. Route existing point writers through the common service.
4. Persist the nine Explore badges using server evidence and global XP; expose one authenticated dashboard snapshot. Admin preview must not fabricate earned achievements.
5. Remove local achievement hydration, backfill and invented XP. Restore dashboard data from the snapshot after login; show loading/error/retry and server-confirmed feedback. Use backend account-level metadata.
6. Verify wrong/correct/replayed attempts, raw-key bypass, transaction rollback, concurrent rewards, account isolation, database migrations, frontend types/build and focused browser checks where available.

Implemented follow-through: Profile and Explore share one badge presentation catalog and use the same authenticated snapshot for XP, level and badge progress. Profile no longer invents passport stamps from badge ownership. Cedar Falls records a zero-XP viewing receipt independently of the 360 tour's fragment.

Verification completed on 2026-10-16:
- Backend: 344 tests, 0 failures, 0 errors, 0 skipped across 107 suites (`mvnw.cmd test`).
- Frontend: production build and ESLint on affected files pass.
- UI: 8 Playwright/Edge tests pass with mocked APIs, including storage deletion, forged local state, failed saves, retries, dialog keyboard behavior, account-switch races and Profile/Explore consistency.
- PostgreSQL: V74/V75 syntax, seed records, historical opening balance and new columns validated in a dedicated schema inside a transaction, then rolled back.
- Independent static review found no remaining introduced high-impact defect in the reviewed paths.

Deployment is not performed: the application's local database was observed at Flyway V67. Apply V74/V75 to the intended environment, restart backend/frontend together, and follow `explore-ui-acceptance.md` for live end-to-end acceptance. No production database data was rewritten.

Limits: persisted historical totals are not proof that all old rewards were legitimate. This change will not erase or recalculate those totals. A separate administrator audit is needed for historical correction. Merely serving educational content cannot prove that a person read it; viewing events remain distinct from server-graded challenges.

> Bản tích hợp nhóm: xem [explore-team-integration.md](explore-team-integration.md). Nếu DB từng dùng V68/V69 Khám phá cũ, cần chuyển lịch sử có kiểm tra trước khi migrate; không chạy trực tiếp để bỏ qua checksum.
