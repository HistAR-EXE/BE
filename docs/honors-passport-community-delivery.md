# Honors passport, community and ten featured explorers

Backend implementation on `feat/lifetime-honors`; preserves prior uncommitted Honors/Explore changes. No migrations, reward writes, production operations or local backend restart.

## Contracts

- Main leaderboard always returns at most ten entries, independent of the older configurable limit. XP alone determines competition rank (1,1,3). Distinct badge count descending, then UUID ascending, orders equal-XP entries for display. Viewer and two ordinal neighbors on each side remain independent of the display cap.
- Metadata: `displayLimit: 10`, `cutoffXp: integer | null` (null when fewer than ten displayed), `omittedTiedAtCutoff: nonnegative integer`. A partitioned XP peer count computes omitted peers even when none of those peers are in the bounded viewer neighborhood. Badge counts are distinct badge IDs.
- `GET /api/leaderboard/passport` returns only authenticated owner facts: `userId`, `displayName`, global `totalPoints`, configured `level`/`levelName`, distinct global `badgeCount`, earned heritage badge slug/date records, and `generatedAt`. A persisted award with an unknown date remains earned with `earnedAt: null`. No avatar, email, arbitrary-target lookup, reward evaluation or writes.
- `GET /api/leaderboard/community?scope=all|city|school|group&city=...&groupId=...` shares leaderboard scope authorization and returns `scope`, `explorers`, `completedChallenges`, `discoveries`, `badges`, `updatedAt`. Group counts never include outsiders. Every read is a read-only repeatable-read transaction, and both routes set `Cache-Control: private, no-store`.

## Official counter definitions

Eligible profiles exclude administrators, paired analytics bootstrap identities, paired reserved legacy emails/original sample password hashes, and provider `ui-preview-honors`. Ranking and category spotlights retain the existing visible local UI samples; only official community totals exclude them.

- Explorers: eligible profiles with positive XP or at least one persisted discovery/badge record. Unused zero-XP accounts do not count.
- Completed challenges: distinct `(user, location, challenge key)` records for the exact Củ Chi server catalog. The current catalog contains **18** keys (13 quizzes, four clues, one conclusion); no nineteenth challenge is invented.
- Discoveries: distinct `(user, location)` records whose location currently exists.
- Badges: distinct `(user, badge)` earned pairs.

## Verification

Tests were written before implementation; the red run failed because the new facts repository/service API did not yet exist. Focused Maven tests cover authentication, private no-store responses, owner-only passport, null award dates, distinct counts, XP tie ranking/display order/cutoff metadata, outside-top viewer/nearby entries, synthetic eligibility, zero-XP activity, and scoped boundaries. Existing stories regressions remain included. Final focused result: **36 tests passed, zero failures/errors** (LeaderboardServiceTest 11, LeaderboardReadRepositoryTest 7, HonorsPassportCommunityTest 7, HonorsStoriesTest 11). Command: `mvn -o -q -Dmaven.compiler.useIncrementalCompilation=false -Dtest=LeaderboardServiceTest,LeaderboardReadRepositoryTest,HonorsPassportCommunityTest,HonorsStoriesTest test`.

All three PostgreSQL scripts pass using temporary tables inside a rolled-back transaction:

```powershell
node scripts/test-honors-postgres.cjs
node scripts/test-stories-postgres.cjs
node scripts/test-passport-community-postgres.cjs
```

The new script executes extracted repository SQL rather than a separately reimplemented query. It verifies the ten-row cap, true XP ranks, distinct badge ordering, partitioned cutoff peers, the viewer outside ten, persisted/null-date passport awards, distinct official metrics, and group boundaries.
