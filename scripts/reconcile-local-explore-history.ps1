param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z_][a-zA-Z0-9_]*$')]
    [string]$Database,
    [string]$Container = 'histar-postgres',
    [string]$User = 'timelens',
    [switch]$Apply
)

$ErrorActionPreference = 'Stop'
# Only the exact unpublished Explore history is recognized. Canonical V68/V69
# from the team must never be repaired, removed or relabelled by this script.
$sql = @'
BEGIN;
LOCK TABLE flyway_schema_history IN EXCLUSIVE MODE;
DO $$ BEGIN
    ASSERT (SELECT count(*) FROM flyway_schema_history WHERE version='68'
        AND script='V68__cu_chi_explore_archive_points.sql' AND checksum=-524819468 AND success)=1,
        'Not the recognized legacy Explore V68; refusing to change history';
    ASSERT (SELECT count(*) FROM flyway_schema_history WHERE version='69'
        AND script='V69__reward_integrity.sql' AND checksum=-1175323133 AND success)=1,
        'Not the recognized legacy Explore V69; refusing to change history';
    ASSERT NOT EXISTS(SELECT 1 FROM flyway_schema_history WHERE version IN ('74','75')),
        'V74/V75 already occupied; refusing to change history';
    ASSERT to_regclass('public.xp_ledger') IS NOT NULL AND to_regclass('public.explore_attempts') IS NOT NULL,
        'Missing legacy Explore schema; refusing to change history';
END $$;
UPDATE flyway_schema_history SET version='74', script='V74__cu_chi_explore_archive_points.sql' WHERE version='68';
UPDATE flyway_schema_history SET version='75', script='V75__reward_integrity.sql' WHERE version='69';
'@

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
    Write-Output 'Legacy history moved to V74/V75. Apply pending team V68-V73 with Flyway outOfOrder=true, then validate normally.'
} else {
    Write-Output 'Dry run passed; no changes committed. Use -Apply only after testing a restored DB clone.'
}
