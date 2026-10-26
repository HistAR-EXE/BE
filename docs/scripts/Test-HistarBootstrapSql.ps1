param(
    [string]$BootstrapPath = (Join-Path (Split-Path $PSScriptRoot -Parent) 'sql\bootstrap\TimeLens_full_bootstrap.sql')
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Get-FlywayMigrationVersion.ps1')

function Assert-Contains([string]$Text, [string]$Needle) {
    if (-not $Text.Contains($Needle)) {
        throw "Bootstrap SQL is missing: $Needle"
    }
}

if (-not (Test-Path $BootstrapPath)) {
    throw "Bootstrap SQL was not generated: $BootstrapPath"
}

$bootstrap = Get-Content -Raw -Encoding UTF8 $BootstrapPath

Assert-Contains $bootstrap '-- HistAR full local bootstrap SQL'
Assert-Contains $bootstrap 'CREATE TABLE profiles ('
Assert-Contains $bootstrap 'CREATE TABLE IF NOT EXISTS organizations ('
Assert-Contains $bootstrap 'CREATE TABLE IF NOT EXISTS billing_settings ('
Assert-Contains $bootstrap "'<< Flyway Baseline >>'"

$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$manifest = Join-Path $beRoot 'docs\seed-manifest.txt'
Get-Content $manifest | ForEach-Object {
    $seed = $_.Trim()
    if ($seed -and -not $seed.StartsWith('#')) {
        Assert-Contains $bootstrap "BEGIN seed: $seed"
    }
}

$migrations = @(Get-ChildItem (Join-Path $beRoot 'src\main\resources\db\migration') -Filter 'V*__*.sql' |
    Sort-Object { Get-FlywayMigrationSortKey $_.Name })

$lastVersion = Get-FlywayMigrationVersionText $migrations[-1].Name
if ((Get-FlywayMigrationSortKey $migrations[-1].Name) -lt (Get-FlywayMigrationSortKey 'V94__minimum.sql')) {
    throw "Bootstrap smoke check requires migrations through V94, found V$lastVersion."
}
Assert-Contains $bootstrap "'$lastVersion',"
Assert-Contains $bootstrap 'CREATE TABLE IF NOT EXISTS passport_mail_attempts ('
Assert-Contains $bootstrap 'fk_passport_mail_profile'
Assert-Contains $bootstrap 'ck_passport_mail_image_hash'
Assert-Contains $bootstrap 'ck_passport_mail_state'
Assert-Contains $bootstrap 'uq_passport_mail_user_request'
Assert-Contains $bootstrap 'idx_passport_mail_user_created'

foreach ($migration in $migrations) {
    Assert-Contains $bootstrap "BEGIN migration: $($migration.Name)"
}

foreach ($requiredMigration in @(
    'V75__organization_ops_extensions.sql',
    'V76__admin_audit_hash_chain.sql',
    'V77__location_ar_readiness_seed.sql',
    'V78__station_latlng_cuuchi_placeholder.sql',
    'V79__seed_platform_accounts.sql',
    'V80__cu_chi_explore_archive_points.sql',
    'V81__reward_integrity.sql',
    'V82__seed_demo_segment_accounts.sql',
    'V83__quest_tracks_v2_schema.sql',
    'V84__quest_tracks_v2_content.sql',
    'V85__quest_media_and_minigames_v2.sql',
    'V86__cu_chi_quest_333_cover.sql',
    'V87__passport_mail_attempts.sql',
    'V88__repair_vietnamese_content.sql',
    'V89__ar_card_mail_attempts.sql',
    'V90__ar_card_mail_artifact.sql',
    'V91__site_workspaces_and_content_versions.sql',
    'V92__field_trip_workflow.sql',
    'V93__demo_dataset_lifecycle.sql',
    'V94__remove_legacy_analytics_demo_seed.sql'
)) {
    Assert-Contains $bootstrap "BEGIN migration: $requiredMigration"
}

Assert-Contains $bootstrap 'CREATE TABLE IF NOT EXISTS ar_card_mail_attempts ('
Assert-Contains $bootstrap 'ADD COLUMN IF NOT EXISTS artifact_id VARCHAR(32)'
Assert-Contains $bootstrap 'CREATE TABLE site_memberships ('
Assert-Contains $bootstrap 'CREATE TABLE site_content_versions ('
Assert-Contains $bootstrap 'CREATE TABLE field_trips ('
Assert-Contains $bootstrap 'CREATE TABLE field_trip_report_snapshots ('
Assert-Contains $bootstrap 'CREATE TABLE demo_dataset_state ('
Assert-Contains $bootstrap 'CREATE OR REPLACE FUNCTION purge_demo_dataset('
Assert-Contains $bootstrap 'legacy_analytics_seed_users'
Assert-Contains $bootstrap 'ADD COLUMN field_trip_id UUID REFERENCES field_trips(id) ON DELETE SET NULL'
Assert-Contains $bootstrap 'Địa đạo Củ Chi'
Assert-Contains $bootstrap 'Hành trình dưới lòng đất'

$lastMigrationMarker = "BEGIN migration: $($migrations[-1].Name)"
$lastMigrationIndex = $bootstrap.IndexOf($lastMigrationMarker)

$postFlywayManifest = Join-Path $beRoot 'docs\seed-manifest-post-flyway.txt'
if (Test-Path $postFlywayManifest) {
    Get-Content $postFlywayManifest | ForEach-Object {
        $seed = $_.Trim()
        if ($seed -and -not $seed.StartsWith('#')) {
            $seedMarker = "BEGIN seed: $seed"
            Assert-Contains $bootstrap $seedMarker
            $seedIndex = $bootstrap.IndexOf($seedMarker)
            if ($lastMigrationIndex -lt 0 -or $seedIndex -le $lastMigrationIndex) {
                throw "Post-Flyway seed must appear after migrations: $seed"
            }
        }
    }
}

$v3 = $bootstrap.IndexOf('BEGIN migration: V3__add_billing_settings.sql')
$v16 = $bootstrap.IndexOf('BEGIN migration: V16__b2c_premium_price_79000.sql')
if ($v3 -lt 0 -or $v16 -lt 0 -or $v3 -ge $v16) {
    throw 'Migration order is invalid: V3 must precede V16.'
}
$v81 = $bootstrap.IndexOf('BEGIN migration: V81__reward_integrity.sql')
$v81Minor = $bootstrap.IndexOf('BEGIN migration: V81_5__organization_invite_baseline.sql')
$v82 = $bootstrap.IndexOf('BEGIN migration: V82__seed_demo_segment_accounts.sql')
if ($v81 -lt 0 -or $v81Minor -le $v81 -or $v82 -le $v81Minor) {
    throw 'Migration order is invalid: V81 must precede V81.5, which must precede V82.'
}

Write-Host "Bootstrap SQL structure is valid: $BootstrapPath"
