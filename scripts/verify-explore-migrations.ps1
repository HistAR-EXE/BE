$ErrorActionPreference = 'Stop'
# Transactional syntax/constraint verification against the local PostgreSQL engine.
# Every table and fixture is in a temporary validation schema; rollback leaves no data behind.
$validationSetup = @'
BEGIN;
CREATE SCHEMA reward_integrity_validation;
SET LOCAL search_path TO reward_integrity_validation;
CREATE TABLE profiles(id UUID PRIMARY KEY, total_points INTEGER);
CREATE TABLE locations(id UUID PRIMARY KEY);
CREATE TABLE messages(id UUID PRIMARY KEY);
CREATE TABLE badges(id UUID PRIMARY KEY, name TEXT, description TEXT, icon_url TEXT,
    condition_type VARCHAR(50), condition_value INTEGER, location_id UUID REFERENCES locations(id));
CREATE TABLE discovery_points(id UUID PRIMARY KEY, location_id UUID NOT NULL REFERENCES locations(id),
    name VARCHAR(255), map_x_pct NUMERIC(5,2), map_y_pct NUMERIC(5,2), unlock_key VARCHAR(128),
    sort_order INTEGER, UNIQUE(location_id, unlock_key));
INSERT INTO profiles VALUES('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 125);
INSERT INTO profiles VALUES('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 200);
INSERT INTO profiles VALUES('cccccccc-cccc-4ccc-8ccc-cccccccccccc', 90);
INSERT INTO locations VALUES('11111111-1111-1111-1111-111111111111');
'@
$hibernateTables = @'
-- Emulate Hibernate-created tables: no SQL defaults, checks or foreign keys.
CREATE TABLE xp_ledger(id UUID PRIMARY KEY, user_id UUID NOT NULL, event_key VARCHAR(255) NOT NULL,
    points INTEGER NOT NULL, awarded_at TIMESTAMPTZ NOT NULL, UNIQUE(user_id, event_key));
CREATE TABLE explore_attempts(id UUID PRIMARY KEY, user_id UUID NOT NULL, location_id UUID NOT NULL,
    challenge_key VARCHAR(128) NOT NULL, answer INTEGER, passed BOOLEAN NOT NULL, submitted_at TIMESTAMPTZ NOT NULL);
CREATE INDEX hibernate_attempt_user_time ON explore_attempts(user_id, submitted_at);
ALTER TABLE messages ADD COLUMN server_generated BOOLEAN NOT NULL DEFAULT FALSE;
INSERT INTO xp_ledger VALUES
    (gen_random_uuid(), 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'baseline:v1', 150, now()),
    (gen_random_uuid(), 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'existing:event', 50, now()),
    (gen_random_uuid(), 'cccccccc-cccc-4ccc-8ccc-cccccccccccc', 'existing:event', 20, now());
INSERT INTO explore_attempts VALUES(gen_random_uuid(), 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111', 'depth_quiz:surface', 0, FALSE, now());
'@
$migrationDirectory = Join-Path $PSScriptRoot '../src/main/resources/db/migration'
$archiveSql = Get-Content -LiteralPath (Join-Path $migrationDirectory 'V74__cu_chi_explore_archive_points.sql') -Raw
$rewardSql = Get-Content -LiteralPath (Join-Path $migrationDirectory 'V75__reward_integrity.sql') -Raw
$validationAssertions = @'
DO $$ BEGIN
    ASSERT (SELECT COUNT(*) FROM discovery_points WHERE unlock_key <> 'feature:diorama') = 22, 'Expected 22 archive points';
    ASSERT (SELECT COUNT(*) FROM discovery_points WHERE unlock_key = 'feature:diorama') = 1, 'Missing diorama receipt';
    ASSERT (SELECT COUNT(*) FROM badges WHERE condition_type = 'explore') = 9, 'Expected nine badges';
    ASSERT (SELECT SUM(points) FROM xp_ledger) = 415, 'Historical XP balance changed or duplicated';
    ASSERT (SELECT COUNT(*) FROM xp_ledger WHERE event_key = 'baseline:v1') = 3, 'Opening balances duplicated';
    ASSERT (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'reward_integrity_validation'
        AND table_name = 'messages' AND column_name = 'server_generated') = 1, 'Missing message provenance';
END $$;
SELECT 'Explore migrations validated; rolling back fixtures.' AS result;
ROLLBACK;
'@
foreach ($scenario in @('fresh', 'hibernate')) {
    Write-Output "Verifying migration scenario: $scenario"
    $existingSql = if ($scenario -eq 'hibernate') { $hibernateTables } else { '' }
    $preserveAssertions = if ($scenario -eq 'hibernate') { @'
DO $$ BEGIN
    ASSERT (SELECT COUNT(*) FROM explore_attempts) = 1, 'Existing attempt was lost';
    ASSERT (SELECT points FROM xp_ledger WHERE user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' AND event_key = 'baseline:v1') = 150, 'Existing baseline changed';
    ASSERT (SELECT points FROM xp_ledger WHERE user_id = 'cccccccc-cccc-4ccc-8ccc-cccccccccccc' AND event_key = 'baseline:v1') = 70, 'Existing events were counted twice';
END $$;
'@ } else { '' }
    # Repeat V75 in the isolated transaction to verify adoption/replay does not duplicate data.
    ($validationSetup + "`n" + $existingSql + "`n" + $archiveSql + "`n" + $rewardSql + "`n" + $rewardSql + "`n" + $preserveAssertions + "`n" + $validationAssertions) |
        docker exec -i histar-postgres psql -U timelens -d timelens -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL migration verification failed: $scenario" }
}
