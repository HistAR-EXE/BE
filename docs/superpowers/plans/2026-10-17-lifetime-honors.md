# Lifetime honors implementation plan

Goal: implement the approved A + C design and equal access for authenticated Free and Premium explorers.

Architecture: keep GET /api/leaderboard, retire week scope, query durable account XP in a read-only consistent snapshot. Return top entries, the viewer outside top, nearby entries and aggregate counts. Personal discovery/badge details use a new read-only GET /api/me/explore/summary, independently loaded. Existing Explore completion/award semantics remain intact. No XP writes or schema migration are needed for leaderboard reads.

Rules: lifetime total_points is the only ranking score; equal points share competition rank (1,1,3), UUID breaks display ties only. Exclude administrators and the explicitly seeded seed-analytics-NNN@histar.vn synthetic accounts from community honors. Zero XP viewers see their real zero balance but no competitive rank until their first XP. Group/school requests retain their scope with server membership checks. Reject anonymous and week scope. No calendar resets, monetary prizes, or new XP rewards.

## 1. Backend regressions
- Add service tests for Free access, no authentication, rejected week scope and foreign groups before implementation.
- Run `./mvnw.cmd -Dtest=LeaderboardServiceTest test`; verify failures match the new requirements.
- Add PostgreSQL-backed query tests for tied XP, viewer outside top, eligibility, zero participants and profile/XP immutability.

## 2. Backend implementation
- Add LeaderboardReadRepository with parameterized SQL, bounded output and competition rank.
- Expand LeaderboardResponse/Entry with viewer, neighbors, counts, level and badge count; retain old fields for client compatibility.
- Remove Premium gate and shared viewer cache; validate authentication/group/school scope before querying. Use read-only repeatable-read transaction for consistent top and viewer.
- Run service and PostgreSQL repository tests and the complete backend suite.

## 3. Frontend regressions and UI
- Add isolated Playwright config and tests for Free access, accurate server totals, no temporal filters/paywall, viewer outside top, empty/error states, stale request/account isolation, rules dialog and 390px viewport.
- Replace the monolithic LeaderboardPage with typed API, a dedicated loading hook, reusable avatar/row/podium and accessible rules dialog. Reuse existing AppLayout, Be Vietnam Pro, Chrono and verified heritage badges.
- Use blue/yellow hero, top three (shared ranks labeled accurately), lifetime ranking, personal milestones and nearby explorers. Never label missing data as zero; personal details may fail independently of the ranking.
- Cancel/ignore outdated requests on account/scope switches. Refresh on verified discovery, visible-tab focus and manual refresh. No fake trend indicators or inert prize buttons.

## 4. Verification and delivery
- Run `npm run build`, `npm run lint`, isolated leaderboard Playwright tests, and Explore regression tests.
- Visually inspect desktop/mobile in browser, verify no overflow, keyboard focus and error recovery.
- Record exact test counts, modified files, deployment requirements and unresolved production access limits. Do not deploy or push without a subsequent user instruction.
