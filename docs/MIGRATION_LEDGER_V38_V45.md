# Migration ledger: V38–V45

This ledger is the release checklist for the quest/minigame data introduced after V37.
Do not edit an applied migration. Correct data with a new versioned migration.

| Version | Purpose | Primary tables | Idempotent | Seed dependency |
| --- | --- | --- | --- | --- |
| V38 | Adds Vietnamese Chrono prompts to existing dialogue steps. | `quest_steps` | Yes: targeted `UPDATE`. | Requires the two Củ Chi quest-step rows. |
| V39 | Repairs mojibake text in Củ Chi content. | `locations`, `quests`, `quest_steps` | Yes: updates are guarded by the corrupted text pattern. | Requires the original Củ Chi seed. |
| V40 | Adds Củ Chi discovery points used by dialogue/tour quests. | `discovery_points`, `quest_steps` | Yes: `ON CONFLICT DO NOTHING` plus targeted update. | Requires canonical Củ Chi location and quest rows. |
| V41 | Normalizes the first Củ Chi journey into four `quest_steps`. | `quests`, `quest_steps` | Yes: guarded columns and `ON CONFLICT`. | Requires Củ Chi location and panorama/discovery keys. |
| V42 | Creates quest minigame schema. | `quest_minigames`, `user_game_results`, `quest_steps` | Yes: `IF NOT EXISTS`. | None. |
| V43 | Seeds four quest minigames and a development-only trial quest. | `quest_minigames`, `quests`, `quest_steps`, `artifacts` | Mostly: fixed IDs/upserts. | Requires V42 and Củ Chi location. |
| V44 | Adds Phase 2 Củ Chi quests, minigame steps, and collection artifacts. | `quests`, `quest_steps`, `artifacts` | Mostly: fixed IDs/upserts. | Requires V42–V43 and Củ Chi. |
| V45 | Corrects wording for the silence relic. | `artifacts` | Yes: targeted `UPDATE`. | Requires V44 artifact seed. |

## Release checks

1. Use `scripts/verify-migration-ledger.ps1` to verify V38–V45 and the controlled heritage seed source hashes.
2. Apply Flyway migrations in version order. Never use `clean` or alter `flyway_schema_history`.
3. Use `scripts/audit-flyway-v38-v45.ps1` with read-only staging/prod credentials to verify history and smoke counts.
4. For an environment that was initialized before the heritage data moved into Flyway, run the controlled seed script only with an approved backup and `-AllowReplaceableSeeds`.
5. Run `scripts/verify-quest-integrity.ps1 -Environment staging` with staging database credentials.
6. Record the Flyway history output and validator output in the deployment record.

## Legacy seed boundary

The heritage content in `docs/sql/seed` predates Flyway and remains a controlled, versioned input until it is backfilled by new migrations. The manifest script checks exact SHA-256 hashes before execution. Its replaceable scripts are never run implicitly by CI or application startup.
