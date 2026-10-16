# Seed Admin Analytics demo data for Địa đạo Củ Chi (UTF-8-safe docker cp).
# Populates visit sessions, discoveries, quest funnel, checkins, analytics_events, pilot_events
# so /admin/analytics shows numbers above the privacy threshold (K=10).
#
# Usage:
#   .\docs\scripts\seed-admin-analytics.ps1
#   .\docs\scripts\seed-admin-analytics.ps1 -Container histar-postgres -Database timelens -User timelens

param(
    [string]$Container = 'histar-postgres',
    [string]$Database = 'timelens',
    [string]$User = 'timelens'
)

$ErrorActionPreference = 'Stop'
$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$seed = Join-Path $beRoot 'docs\sql\seed\2026-10-15_admin_analytics_cu_chi_demo.sql'

if (-not (Test-Path $seed)) {
    throw "Analytics seed missing: $seed"
}

$remote = '/tmp/histar-admin-analytics-seed.sql'
Write-Host "Seeding Admin Analytics (Cu Chi demo) via '$Container' / '$Database'..."
& docker cp $seed "${Container}:${remote}"
if ($LASTEXITCODE -ne 0) {
    throw "docker cp failed for $seed"
}

try {
    & docker exec $Container psql -U $User -d $Database -v ON_ERROR_STOP=1 -f $remote
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed while applying analytics seed."
    }
} finally {
    & docker exec $Container rm -f $remote | Out-Null
}

$users = (@(& docker exec $Container psql -U $User -d $Database -tAc `
    "SELECT COUNT(*)::text FROM profiles WHERE email LIKE 'seed-analytics-%@histar.vn';") -join '').Trim()
$events = (@(& docker exec $Container psql -U $User -d $Database -tAc `
    "SELECT COUNT(*)::text FROM analytics_events WHERE metadata = 'seed-admin-analytics-cu-chi';") -join '').Trim()

Write-Host "Done. seed users=$users; analytics_events=$events. Hard-refresh /admin/analytics (Địa đạo Củ Chi · 30 ngày)."
