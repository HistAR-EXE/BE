$ErrorActionPreference = 'Stop'
$runnerPath = Join-Path $PSScriptRoot 'bootstrap-local-db.ps1'

if (-not (Test-Path $runnerPath)) {
    throw "Bootstrap runner was not created: $runnerPath"
}

$tokens = $null
$parseErrors = $null
[void][System.Management.Automation.Language.Parser]::ParseFile(
    $runnerPath,
    [ref]$tokens,
    [ref]$parseErrors
)

if ($parseErrors.Count -gt 0) {
    throw "Bootstrap runner has PowerShell parse errors: $($parseErrors[0].Message)"
}

$runner = Get-Content -Raw -Encoding UTF8 $runnerPath
foreach ($requiredText in @(
    '[switch]$Reset',
    'TimeLens_full_bootstrap.sql',
    '$ExpectedMinFlywayVersion = 79',
    'Refusing to overwrite an existing database without -Reset',
    'Bootstrap completed successfully',
    'docker cp',
    '$containerBootstrapPath'
)) {
    if (-not $runner.Contains($requiredText)) {
        throw "Bootstrap runner is missing: $requiredText"
    }
}

foreach ($requiredMigration in @(
    'V75__organization_ops_extensions.sql',
    'V76__admin_audit_hash_chain.sql',
    'V77__location_ar_readiness_seed.sql',
    'V78__station_latlng_cuuchi_placeholder.sql',
    'V79__seed_platform_accounts.sql'
)) {
    if (-not (Test-Path (Join-Path $PSScriptRoot "..\..\src\main\resources\db\migration\$requiredMigration"))) {
        throw "Bootstrap runner smoke check is missing: $requiredMigration"
    }
}

if ($runner.Contains('Get-Content -Raw -Encoding UTF8 $bootstrapPath |')) {
    throw 'Bootstrap runner must not pipe UTF-8 SQL through a Windows native command.'
}

Write-Host "Bootstrap runner structure is valid: $runnerPath"
