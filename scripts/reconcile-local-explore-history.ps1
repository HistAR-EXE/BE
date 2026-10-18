param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z_][a-zA-Z0-9_]*$')]
    [string]$Database,
    [string]$Container = 'histar-postgres',
    [string]$User = 'timelens',
    [switch]$Apply,
    [switch]$AdoptKnownTeamV72Checksum
)

$ErrorActionPreference = 'Stop'
# Only exact unpublished Explore/passport identities are relabelled. Canonical team history
# stays intact. V72 source-equivalent checksum adoption requires an explicit opt-in switch.
$sql = @'
BEGIN;
LOCK TABLE flyway_schema_history IN EXCLUSIVE MODE;
SET LOCAL plpgsql.check_asserts = on;
DO $$
DECLARE archive_version text; reward_version text;
BEGIN
    IF EXISTS(SELECT 1 FROM flyway_schema_history WHERE version='74' AND script='V74__cu_chi_explore_archive_points.sql') THEN
        archive_version := '74'; reward_version := '75';
    ELSIF EXISTS(SELECT 1 FROM flyway_schema_history WHERE version='80' AND script='V80__cu_chi_explore_archive_points.sql') THEN
        archive_version := '80'; reward_version := '81';
    ELSE
        archive_version := '68'; reward_version := '69';
    END IF;
    ASSERT (SELECT count(*) FROM flyway_schema_history WHERE version=archive_version
        AND description='cu chi explore archive points' AND type='SQL'
        AND script='V' || archive_version || '__cu_chi_explore_archive_points.sql' AND checksum=-524819468 AND success)=1,
        'Not a recognized unpublished Explore V68/V74; refusing to change history';
    ASSERT (SELECT count(*) FROM flyway_schema_history WHERE version=reward_version
        AND description='reward integrity' AND type='SQL'
        AND script='V' || reward_version || '__reward_integrity.sql' AND checksum=-1175323133 AND success)=1,
        'Not a recognized unpublished reward V69/V75; refusing to change history';
    ASSERT archive_version='80' OR NOT EXISTS(SELECT 1 FROM flyway_schema_history WHERE version IN ('80','81','82')),
        'V80/V81/V82 already occupied; refusing to change history';
    ASSERT to_regclass('public.xp_ledger') IS NOT NULL AND to_regclass('public.explore_attempts') IS NOT NULL,
        'Missing legacy Explore schema; refusing to change history';
    IF EXISTS(SELECT 1 FROM flyway_schema_history WHERE version='76' AND script='V76__passport_mail_attempts.sql') THEN
        ASSERT (SELECT count(*) FROM flyway_schema_history WHERE version='76'
            AND description='passport mail attempts' AND type='SQL'
            AND script='V76__passport_mail_attempts.sql' AND checksum=1978920354 AND success)=1,
            'Not the recognized unpublished passport V76; refusing to change history';
        ASSERT to_regclass('public.passport_mail_attempts') IS NOT NULL,
            'Missing passport schema; refusing to change history';
        UPDATE flyway_schema_history SET version='82', script='V82__passport_mail_attempts.sql' WHERE version='76';
    END IF;
    UPDATE flyway_schema_history SET version='80', script='V80__cu_chi_explore_archive_points.sql' WHERE version=archive_version;
    UPDATE flyway_schema_history SET version='81', script='V81__reward_integrity.sql' WHERE version=reward_version;
END $$;
'@

if ($AdoptKnownTeamV72Checksum) {
    # Exact normalized published source; team 70d3e9d only changes TEMP table lifetime/comments/BOM.
    $teamV72Path = Join-Path $PSScriptRoot '../src/main/resources/db/migration/V72__admin_analytics_cu_chi_demo_seed.sql'
    $teamV72Text = [IO.File]::ReadAllText($teamV72Path).TrimStart([char]0xFEFF).Replace("`r`n", "`n")
    $teamV72Hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($teamV72Text))).ToLowerInvariant()
    if ($teamV72Hash -ne '6be1303d487a54e166d326fa9e085205da16e897bd9dfddb82ca8c2eb1156364') {
        throw 'Team V72 source differs from verified persistent-equivalent revision; refusing checksum adoption.'
    }
    $sql += @'

DO $$ BEGIN
    ASSERT (SELECT count(*) FROM flyway_schema_history WHERE version='72'
        AND description='admin analytics cu chi demo seed' AND type='SQL'
        AND script='V72__admin_analytics_cu_chi_demo_seed.sql'
        AND checksum=1069287222 AND success)=1,
        'V72 is not the exact recognized prior team migration; refusing checksum adoption';
    UPDATE flyway_schema_history SET checksum=-1730327056 WHERE version='72';
END $$;
'@
}

if ($Apply) {
    # Preserve a full recoverable DB snapshot before touching metadata.
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $remote = "/tmp/explore-history-$Database-$stamp.dump"
    $backupDirectory = Join-Path $PSScriptRoot '../target/db-backups'
    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
    & docker exec $Container pg_dump -U $User -d $Database -Fc -f $remote
    if ($LASTEXITCODE -ne 0) { throw 'Backup failed; history was not changed.' }
    & docker cp "${Container}:$remote" "$backupDirectory/"
    if ($LASTEXITCODE -ne 0) { throw 'Backup copy failed; history was not changed.' }
    Write-Output "Backup saved in $backupDirectory"
    $sql += "`nCOMMIT;"
} else {
    $sql += "`nROLLBACK;"
}

$sql | & docker exec -i $Container psql -U $User -d $Database -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) { throw 'Legacy history identity check failed; transaction rolled back.' }
if ($Apply) {
    Write-Output 'Recognized history moved to V80/V81 and V82 when unpublished passport V76 exists. Apply pending team V68-V79 with Flyway outOfOrder=true, then validate normally.'
} else {
    Write-Output 'Dry run passed; no changes committed. Use -Apply only after testing a restored DB clone.'
}
