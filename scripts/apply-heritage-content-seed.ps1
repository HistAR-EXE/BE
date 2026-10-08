[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$DatabaseUrl,
    [Parameter(Mandatory)]
    [string]$DatabaseUser,
    [Parameter(Mandatory)]
    [string]$DatabasePassword,
    [switch]$AllowReplaceableSeeds
)

$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
& (Join-Path $PSScriptRoot 'verify-migration-ledger.ps1')

if (-not $AllowReplaceableSeeds) {
    throw 'These historical seeds contain UPDATE/DELETE statements. Confirm a backup and pass -AllowReplaceableSeeds to continue.'
}

$seedDirectory = Join-Path $backendRoot 'docs/sql/seed'
$orderedSeeds = @(
    '2026-06-19_heritage_sites_from_dataset.sql',
    '2026-06-21_heritage_quests_seed.sql',
    '2026-06-23_quest_mission_keys.sql',
    '2026-06-24_quest_visual_artifact_keys.sql',
    '2026-06-25_quest_mixed_visual_pipeline.sql',
    '2026-06-26_heritage_quest_discovery_points.sql',
    '2026-06-27_heritage_p2_checkin_bonus.sql',
    '2026-07-04_quest_onsite_flag.sql',
    '2026-07-08_quest_steps_schema.sql',
    '2026-07-08_quest_steps_init.sql',
    '2026-10-07_unattached_quest_engines.sql'
)

$env:PGPASSWORD = $DatabasePassword
try {
    foreach ($seed in $orderedSeeds) {
        $path = Join-Path $seedDirectory $seed
        Write-Host "Applying controlled seed: $seed"
        & psql --dbname=$DatabaseUrl --username=$DatabaseUser --set=ON_ERROR_STOP=1 --file=$path
        if ($LASTEXITCODE -ne 0) {
            throw "Seed failed: $seed"
        }
    }
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Host 'Controlled heritage seed completed. Run verify-quest-integrity.ps1 before release.'
