# Local honors migration integration (19 October 2026)

Canonical incoming team migrations retain V68–V79. Unpublished Explore migrations move from local V74/V75 (or earlier V68/V69) to V80/V81; passport attempts moves from local V76 to V82. SQL contents and Flyway checksums are preserved.

`scripts/reconcile-local-explore-history.ps1` defaults to a rollback-only dry run. It requires exact version/script/description/type/checksum/success identities, refuses occupied destinations, checks existing application tables, and locks Flyway history. Apply first creates a full recoverable dump under ignored `target/db-backups`. No generic `flyway repair`, application-data deletion, or teammate migration rewriting is used.

| Unpublished source | Canonical target | Checksum |
| --- | --- | --- |
| V68 or V74 cu chi explore archive points | V80 | -524819468 |
| V69 or V75 reward integrity | V81 | -1175323133 |
| V76 passport mail attempts (when present) | V82 | 1978920354 |

Commands from BE, selecting the intended restored clone explicitly:

```powershell
./scripts/reconcile-local-explore-history.ps1 -Database CLONE_DATABASE
./scripts/reconcile-local-explore-history.ps1 -Database CLONE_DATABASE -Apply
```

Then run Flyway against that same selected database with `outOfOrder=true` once to execute pending team migrations, followed by ordinary validation. The helper preserves history checksum, description, installation rank, and timestamps; only the unpublished version/script identities change.

## Existing teammate V72 checksum drift

The fresh local backup records V72 checksum `1069287222`. Incoming team commit `70d3e9d` changes it to `-1730327056`. Git comparison from `afcab3a` confirms only BOM removal, explanatory comments, and removal of `ON COMMIT DROP` from the session-scoped temporary `seed_analytics_users` table. All persistent statements are identical. Inside Flyway's migration transaction, the persistent data effect is identical; the change supports psql autocommit bootstrap sessions.

Flyway correctly refuses normal validation until this specific prior-checksum provenance is addressed. Do not rerun the analytics seed or use blanket repair. Any checksum adoption must independently guard the exact V72 script/description/type/success/old checksum and preserve all application rows. An additive migration alone cannot resolve checksum validation of an already-applied version.

## Clone evidence

Fresh backup `histar-before-honors-publish-20261019-000104.dump` was restored into disposable database `honors_publish_clone_20261019`. Guarded unpublished-history dry run and apply passed there. Resolved V80/V81/V82 checksums exactly match their original applied checksums. Initial standalone Flyway migration refused the pre-existing V72 mismatch before executing pending V74–V79. The original `timelens` database was not modified by this verification.

Completed clone result: explicit `-AdoptKnownTeamV72Checksum` was enabled only after confirming the exact published source normalized SHA-256 `6be1303d487a54e166d326fa9e085205da16e897bd9dfddb82ca8c2eb1156364`. The helper checks this file identity and exact previous V72 history before changing checksum `1069287222` to `-1730327056`. It does not rerun the seed. The explicit switch remains off by default.

Flyway then applied all six pending team V74–V79 migrations on the clone and ordinary validation confirmed **83 valid migrations through V82**. See ignored `target/honors-clone-flyway.log`. The standalone verifier avoided Maven while the main test process owned it.

Protected-data comparison after migration matched the original exactly, excluding the 18 authorized `ui-preview-honors` cleanup accounts and the three explicitly updated team platform seed accounts for the profile XP/level comparison:

| Data | Rows | Matching aggregate fingerprint |
| --- | ---: | --- |
| Non-preview XP ledger | 148 | 409e3b29ebfb858931795688e4d6da4e |
| Discoveries | 567 | 9861fe679f270372f940cf2f0c3c40ad |
| Non-preview earned badges | 5 | ac3fd3fd3c1e399666017277b63ed2f5 |
| Explore attempts | 18 | 60da05541155fd9c65bdd1aca489b254 |
| Passport mail attempts | 2 | 21e299af92fb13704b8d884c23005a38 |
| Non-preview/non-platform profile XP and level | 131 | f9f8b7a087b00e9f7f19f155dd101ae5 |

For the original local database, the parent operator can review and run the same dry run with `-AdoptKnownTeamV72Checksum`, then apply with a fresh full backup, then migrate once with out-of-order enabled and validate normally. Clone verification itself did not mutate the original database.

## Fresh bootstrap through V82

Updated bootstrap runner minimum and structure smoke tests to V82, regenerated the full SQL from all unchanged teammate schema/seed/migration sources, and added explicit passport owner/hash/state/idempotency constraints plus quota-index verification. Both PowerShell structure smoke tests passed. The full default bootstrap runner (including teammate dashboard, billing, and analytics top-ups) passed on newly created disposable `honors_bootstrap_verify_20261019`, with baseline V82 and passport constraints/index ready. Evidence: ignored `target/honors-bootstrap-v82.log`.

After the parent reconciled and migrated original `timelens`, an independent read-only recheck matched every protected fingerprint above exactly and confirmed zero `ui-preview-honors` identities. No original database writes or resets were performed by this verification.
