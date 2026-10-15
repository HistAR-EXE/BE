# Re-apply Admin Billing CRM Kanban seed (V71) with UTF-8-safe docker cp.
# Avoids PowerShell Get-Content piping which corrupts Vietnamese on Windows.
#
# Usage:
#   .\docs\scripts\seed-admin-billing-crm.ps1
#   .\docs\scripts\seed-admin-billing-crm.ps1 -Container histar-postgres -Database timelens -User timelens

param(
    [string]$Container = 'histar-postgres',
    [string]$Database = 'timelens',
    [string]$User = 'timelens'
)

$ErrorActionPreference = 'Stop'
$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$seed = Join-Path $beRoot 'src\main\resources\db\migration\V71__admin_billing_crm_pipeline_seed.sql'

if (-not (Test-Path $seed)) {
    throw "CRM seed missing: $seed"
}

$remote = '/tmp/histar-v71-crm-seed.sql'
Write-Host "Seeding Admin Billing CRM pipeline (UTF-8) via '$Container' / '$Database'..."
& docker cp $seed "${Container}:${remote}"
if ($LASTEXITCODE -ne 0) {
    throw "docker cp failed for $seed"
}

try {
    & docker exec $Container psql -U $User -d $Database -v ON_ERROR_STOP=1 -f $remote
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed while applying CRM seed."
    }
} finally {
    & docker exec $Container rm -f $remote | Out-Null
}

$count = (@(& docker exec $Container psql -U $User -d $Database -tAc `
    "SELECT COUNT(*)::text FROM heritage_digitization_inquiries WHERE contact_email LIKE 'seed-crm-%@histar.vn';") -join '').Trim()
$sample = (@(& docker exec $Container psql -U $User -d $Database -tAc `
    "SELECT site_name FROM heritage_digitization_inquiries WHERE contact_email = 'seed-crm-lead-cva@histar.vn' LIMIT 1;") -join '').Trim()

if ($sample -match 'Ã|Æ°|á»') {
    throw "CRM seed still has mojibake in site_name: $sample"
}

Write-Host "Done. seed-crm inquiries=$count; sample='$sample'. Hard-refresh /admin/billing Kanban."
