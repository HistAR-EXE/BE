[CmdletBinding()]
param(
    [ValidateSet('local', 'staging')]
    [string]$Environment = 'local',
    [string]$FrontendRoot
)

$ErrorActionPreference = 'Stop'

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not $FrontendRoot) {
    $commonGitDir = (& git -C $backendRoot rev-parse --path-format=absolute --git-common-dir).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw 'Cannot resolve the backend Git common directory. Pass -FrontendRoot explicitly.'
    }
    $projectRoot = Split-Path (Split-Path $commonGitDir -Parent) -Parent
    $FrontendRoot = Join-Path $projectRoot 'FE'
}

$frontendFile = Join-Path $FrontendRoot 'src/features/gamification/heritageQuestSteps.ts'
if (-not (Test-Path $frontendFile)) {
    throw "Frontend metadata was not found: $frontendFile"
}

$migrationDirectory = Join-Path $backendRoot 'src/main/resources/db/migration'
$seedDirectory = Join-Path $backendRoot 'docs/sql/seed'
$backendSqlFiles = @(
    Get-ChildItem $migrationDirectory -Filter '*.sql'
    Get-ChildItem $seedDirectory -Filter '*.sql' -ErrorAction SilentlyContinue
)
$backendSqlText = ($backendSqlFiles | Get-Content -Raw) -join "`n"
$frontendText = Get-Content $frontendFile -Raw

$frontendKeys = [regex]::Matches($frontendText, "unlockKey\s*:\s*'([^']+)'") |
    ForEach-Object { $_.Groups[1].Value } |
    Sort-Object -Unique
$missingKeys = @()
foreach ($key in $frontendKeys) {
    if ($key -match '^(checkin-bonus|checkin|era)(:|$)') {
        continue
    }
    if (-not $backendSqlText.Contains($key)) {
        $missingKeys += $key
    }
}
if ($missingKeys.Count -gt 0) {
    throw "Frontend unlockKey has no backend migration reference: $($missingKeys -join ', ')"
}

Write-Host "Static quest integrity passed: $($frontendKeys.Count) frontend unlock keys checked."

if ($Environment -eq 'local') {
    Push-Location $backendRoot
    try {
        & .\mvnw.cmd -q -Dtest=SeedConsistencyTest test
        if ($LASTEXITCODE -ne 0) {
            throw 'Runtime quest integrity tests failed.'
        }
    } finally {
        Pop-Location
    }
    Write-Host 'Local runtime quest integrity passed.'
    exit 0
}

if (([string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_URL)) -or ([string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_USERNAME)) -or ([string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_PASSWORD))) {
    throw 'For staging, set SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, and SPRING_DATASOURCE_PASSWORD.'
}

Push-Location $backendRoot
try {
    & .\mvnw.cmd -q spring-boot:run "-Dspring-boot.run.arguments=--histar.quest-integrity.run-on-startup=true --spring.jpa.hibernate.ddl-auto=validate"
    if ($LASTEXITCODE -ne 0) {
        throw 'Staging runtime quest integrity validation failed.'
    }
} finally {
    Pop-Location
}

Write-Host 'Staging runtime quest integrity passed.'
