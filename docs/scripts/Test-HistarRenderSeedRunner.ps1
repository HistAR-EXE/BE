$ErrorActionPreference = 'Stop'
$runnerPath = Join-Path $PSScriptRoot 'run-all-seed-render.ps1'

if (-not (Test-Path $runnerPath)) {
    throw "Render seed runner was not found: $runnerPath"
}

$tokens = $null
$parseErrors = $null
[void][System.Management.Automation.Language.Parser]::ParseFile(
    $runnerPath,
    [ref]$tokens,
    [ref]$parseErrors
)

if ($parseErrors.Count -gt 0) {
    throw "Render seed runner has PowerShell parse errors: $($parseErrors[0].Message)"
}

$runner = Get-Content -Raw -Encoding UTF8 $runnerPath
foreach ($requiredText in @(
    '$ExpectedMinimumFlywayVersion = 94',
    'Flyway V$latestVersion is successful.',
    'V91 site workspace schema is ready.',
    'V92 field trip schema is ready.',
    'V93 demo dataset lifecycle is ready.',
    'V94 legacy analytics seed removal is ready.'
)) {
    if (-not $runner.Contains($requiredText)) {
        throw "Render seed runner is missing: $requiredText"
    }
}

foreach ($requiredMigration in @(
    'V90__ar_card_mail_artifact.sql',
    'V91__site_workspaces_and_content_versions.sql',
    'V92__field_trip_workflow.sql',
    'V93__demo_dataset_lifecycle.sql',
    'V94__remove_legacy_analytics_demo_seed.sql'
)) {
    if (-not (Test-Path (Join-Path $PSScriptRoot "..\..\src\main\resources\db\migration\$requiredMigration"))) {
        throw "Render seed runner migration chain is missing: $requiredMigration"
    }
}

Write-Host "Render seed runner structure is valid: $runnerPath"
