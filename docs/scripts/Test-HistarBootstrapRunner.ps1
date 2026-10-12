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
    'Refusing to overwrite an existing database without -Reset',
    'Bootstrap completed successfully',
    'docker cp',
    '$containerBootstrapPath'
)) {
    if (-not $runner.Contains($requiredText)) {
        throw "Bootstrap runner is missing: $requiredText"
    }
}

if ($runner.Contains('Get-Content -Raw -Encoding UTF8 $bootstrapPath |')) {
    throw 'Bootstrap runner must not pipe UTF-8 SQL through a Windows native command.'
}

Write-Host "Bootstrap runner structure is valid: $runnerPath"
