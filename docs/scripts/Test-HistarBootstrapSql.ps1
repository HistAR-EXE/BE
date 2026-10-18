param(
    [string]$BootstrapPath = (Join-Path (Split-Path $PSScriptRoot -Parent) 'sql\bootstrap\TimeLens_full_bootstrap.sql')
)

$ErrorActionPreference = 'Stop'

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

$migrations = Get-ChildItem (Join-Path $beRoot 'src\main\resources\db\migration') -Filter 'V*__*.sql' |
    Sort-Object { [int]([regex]::Match($_.Name, '^V(\d+)__').Groups[1].Value) }

$lastVersion = [regex]::Match($migrations[-1].Name, '^V(\d+)__').Groups[1].Value
if ($lastVersion -ne '82') {
    throw "Bootstrap smoke check expects the latest migration to be V82, found V$lastVersion."
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
    'V82__passport_mail_attempts.sql'
)) {
    Assert-Contains $bootstrap "BEGIN migration: $requiredMigration"
}

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

Write-Host "Bootstrap SQL structure is valid: $BootstrapPath"
