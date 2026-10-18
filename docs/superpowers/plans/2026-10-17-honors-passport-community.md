# Honors passport, community and top-ten implementation plan

**Goal:** Complete approved features 4/5, balance the two-column composition, and display at most ten featured explorers with a transparent tie policy.

**Approved policy:** XP alone determines competition rank (1,1,3). Badge count desc orders equal XP for display only; account UUID provides the final stable neutral order. Never change ranks to manufacture ten distinct places. Cap main entries at ten; keep the viewer and nearby ranks independent. Explain boundary ties and category spotlight display limits.

**Architecture:** Authenticated read-only `/api/leaderboard/passport` and `/api/leaderboard/community` use confirmed DB records and no-store responses. Passport has private current-owner facts, not a public share profile. Frontend exports a PNG locally; sharing requires a user click and uses file sharing when available, download fallback otherwise. Exported images are souvenirs with a timestamp, not signed certificates and never grant rewards. Community statistics exclude administrator and known synthetic data including local UI preview identities.

**Design:** Preserve the current blue/yellow product style. Use paired grid rows (top-ten + passport/summary, timeline + badges/next action, nearby + personal discoveries) instead of one unbounded left column and a short sticky aside. A meaningful community panel pairs with nearby explorers and encouragement. History is expandable. Names wrap and cards remain readable on mobile. No artificial fixed-height filler, fake activity or infinite animation.

## Backend
- [x] Add failing tests for ten-person cap, XP ties ordered by badge count/UUID while preserving same rank, cutoff omission metadata and viewer outside ten.
- [x] Add DB-backed passport with userId/name/global XP/level/levelName/distinct badgeCount/earned heritage badge IDs/generatedAt. No arbitrary userId parameter; read-only and no rewards.
- [x] Add scope-authorized community metrics: explorers with confirmed activity, completed Củ Chi catalog challenges, distinct user/location discovery pairs, distinct awarded user/badge pairs; exclude synthetic identities, include clear metric definitions.
- [x] Test anonymous access, group boundaries, no-store, no changes to XP/ledger/badges, seed exclusion and empty states. Validate PostgreSQL SQL with rollback.

## Frontend
- [x] Add failing UI tests for main top-ten cap, cutoff tie explanation and viewer outside top, private passport choices/export/errors and community metrics/errors/mobile layout.
- [x] Add strict API validation and independent owner/scope/revision-safe loading. Passport refreshed on explicit export and sanitized by current owner; never export forged localStorage points.
- [x] Implement native canvas PNG artwork using trusted same-origin badge assets and text; no remote avatar/CORS dependency. Offer name/XP/level/badge visibility and earned badge selection, plus slogan choices. Mark as personal snapshot and include date. Browser-native sharing only after user gesture with a usable download fallback, and no fake success when canceled/failed.
- [x] Implement community band with clear scope/counter definitions and honest empty/error/retry states.
- [x] Reorganize paired responsive rows and bound main leaderboard entries, explaining same-XP cutoff while preserving actual personal ranks. Retain existing UI style and all unrelated changes.

## Verification
- [x] Run focused BE regression tests, PostgreSQL transaction harness, local authenticated API read-only checks and cleanup.
- [x] Run leaderboard UI regressions (Explore source unchanged in this feature), FE build/lint; inspect desktop/mobile/exported PNG artifacts, name widths and paired row placement.
- [x] Update delivery notes. Keep existing 18 labeled local samples for viewing; no production fixture data, push or deployment.
