# Apply demo revenue seed for Admin dashboard chart (local Docker Postgres).
# Usage (from BE/):
#   .\docs\scripts\seed-admin-dashboard-revenue.ps1
#   .\docs\scripts\seed-admin-dashboard-revenue.ps1 -Container histar-postgres -Database histar -User postgres

param(
    [string]$Container = '',
    [string]$Database = 'timelens',
    [string]$User = 'timelens'
)

$ErrorActionPreference = 'Stop'
$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$seed = Join-Path $beRoot 'docs\sql\seed\2026-10-15_admin_dashboard_revenue_demo.sql'
if (-not (Test-Path $seed)) { throw "Missing seed file: $seed" }

if (-not $Container) {
    $names = docker ps --format '{{.Names}}' 2>$null
    $Container = @(
        $names | Where-Object { $_ -match 'postgres|histar|timelens' }
    ) | Select-Object -First 1
}
if (-not $Container) {
    throw 'No running Postgres container found. Start local DB then re-run, or pass -Container <name>.'
}

Write-Host "Seeding admin dashboard revenue via container '$Container' database '$Database'..."
# docker cp preserves UTF-8; piping Get-Content through stdin often mojibakes Vietnamese on Windows.
$remote = "/tmp/histar-admin-dashboard-revenue-$Database.sql"
& docker cp $seed "${Container}:${remote}"
if ($LASTEXITCODE -ne 0) { throw "docker cp failed for $seed" }
try {
    & docker exec $Container psql -U $User -d $Database -v ON_ERROR_STOP=1 -f $remote
    if ($LASTEXITCODE -ne 0) { throw "psql failed with exit $LASTEXITCODE" }
} finally {
    & docker exec $Container rm -f $remote | Out-Null
}
Write-Host 'Done. Refresh /admin - chart should show 6 months B2C/B2B bars.'
