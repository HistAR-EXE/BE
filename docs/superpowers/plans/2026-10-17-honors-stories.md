# Honors stories implementation plan

**Goal:** Extend the approved lifetime honors page with an evidence-backed personal timeline, four inclusive achievement spotlights, and actionable next steps.

**Architecture:** A separate authenticated, read-only `/api/leaderboard/stories` endpoint returns timeline, paths and scope-aware spotlights. Existing ranking and Explore summary remain independent so optional story failures cannot hide the board. No migrations, XP writes, badge awards, browser progress imports or production fixture writes.

**Design:** Blue/yellow editorial journey: a compact personal expedition strip before the main board, four achievement cards with actual people and explicit criteria, and a dated personal timeline with an expandable history. Existing top podium and personal summary remain. Motion honors reduced-motion preferences. All new copy in Vietnamese, equal Free/Premium rules, no weekly/monthly competition.

## Task 1 — Evidence API
- [x] Add backend failing tests for personal-only timeline, unknown dates, stable chronological order, completed paths and nearest unfinished target, spotlight ties, group boundaries and read-only behavior.
- [x] Add DTOs, repository/service and controller mapping. Contract: `{userId, timeline, paths, spotlights, updatedAt}`; timeline `{id,title,description,occurredAt,href,icon}`; paths `{id,title,description,completed,total,percent,href,actionLabel,complete}`; spotlight `{id,title,description,unit,leaders,totalAchievers,viewerCount,viewerRank}` with leader `{userId,displayName,avatarUrl,count,currentUser}`.
- [x] Scope validation reuses existing leaderboard authorization. Spotlight results are bounded (three leaders per category); all equal metrics share rank. Admin/Analytics/bootstrap exclusion matches ranking. Timeline evidence only belongs to the authenticated viewer; missing historical dates stay null.
- [x] Paths cover Củ Chi depths (4), ciphers (3), evidence with conclusion (5), dossier quizzes (6), using exact server catalog keys. Links target unfinished content through allowlisted Explore query parameters. Full completion changes invitation to a completed state, no phantom rewards.
- [x] Run `./mvnw.cmd -Dtest=HonorsStoriesTest,LeaderboardServiceTest,LeaderboardReadRepositoryTest test`; verify PostgreSQL SQL with isolated test data and rollback.

## Task 2 — Narrative UI
- [x] Add UI fixtures and failing tests for story error isolation, missing dates, true dated timeline, path deep links, completion state, four spotlight categories, tied leaders and long mobile names.
- [x] Add strict API parser and independent owner/scope/revision keyed stories request in useHonors. Optional errors show a retry without concealing valid ranks.
- [x] Build `HonorsStories.tsx` and scoped CSS for the four achievement spotlights, personal timeline and next-action cards; add keyboard-operable history expansion and criterion explanations.
- [x] Add allowlisted Explore route query handling to select the exact unfinished depth/cipher/dossier/evidence view and scroll to its section after loading, preserving existing submission/security behavior.
- [x] Integrate into LeaderboardPage without duplicating the old next-badge panel; preserve personal counts and existing main board.

## Task 3 — Verification and delivery
- [x] Run full leaderboard UI suite and Explore regression suite, FE build/lint and backend relevant/full tests as warranted.
- [x] Read actual local API, verify XP/badges/ledger fingerprint unchanged across repeated views. Retain existing 18 explicitly labeled local UI fixtures.
- [x] Inspect desktop/mobile screenshots and correct clipping, focus, hierarchy, misleading success states or horizontal overflow.
- [x] Update delivery notes with fresh results, known limits and local preview link. Leave work uncommitted on existing feature branch; no push/deploy requested.
