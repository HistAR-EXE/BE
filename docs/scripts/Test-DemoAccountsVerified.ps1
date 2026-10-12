$ErrorActionPreference = 'Stop'
$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$seedPath = Join-Path $beRoot 'docs\sql\seed\2026-07-05_ensure_admin_accounts.sql'
$repairPath = Join-Path $beRoot 'src\main\resources\db\migration\V63__verify_seed_accounts.sql'

if (-not (Test-Path $repairPath)) {
    throw 'Missing V63 repair migration for seeded account verification.'
}

$seed = Get-Content -Raw -Encoding UTF8 $seedPath
$repair = Get-Content -Raw -Encoding UTF8 $repairPath

foreach ($email in @('admin@histar.vn', 'demo@histar.vn', 'teacher@histar.vn')) {
    if (-not $seed.Contains($email)) {
        throw "Seed does not cover $email"
    }
    if (-not $repair.Contains($email)) {
        throw "Repair migration does not cover $email"
    }
}

if (-not $seed.Contains('email_verified = TRUE')) {
    throw 'Re-running the admin seed must preserve verified demo accounts.'
}
if (-not $repair.Contains('email_verified = TRUE')) {
    throw 'Repair migration must verify existing seeded accounts.'
}

Write-Host 'Seeded demo/admin/teacher accounts remain email-verified.'
