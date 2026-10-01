# Seed Postgres Docker (histar-postgres). From BE/: .\docs\scripts\run-all-seed.ps1
param(
    [string]$Container = 'histar-postgres',
    [string]$DbUser = 'timelens',
    [string]$DbName = 'timelens',
    [switch]$IncludeSchema
)

$ErrorActionPreference = 'Stop'
$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

if (-not (docker ps --format '{{.Names}}' | Select-String -SimpleMatch $Container)) {
    $fallback = docker ps --filter 'name=histar-postgres' --format '{{.Names}}' | Select-Object -First 1
    if ($fallback) { $Container = $fallback }
    else { throw "Container '$Container' not running. Run: docker compose up -d (from HistAR root)" }
}

Write-Host "Container: $Container  Database: $DbName"

function Invoke-PsqlFile([string]$SqlPath) {
    $rel = $SqlPath.Replace('\', '/')
    Write-Host ">> $rel"
    Get-Content -Raw -Encoding UTF8 $SqlPath | docker exec -i $Container psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -f -
    if ($LASTEXITCODE -ne 0) { throw "psql failed: $SqlPath" }
}

& (Join-Path $PSScriptRoot 'Invoke-HistarSeed.ps1') -IncludeSchema:$IncludeSchema -RunSqlFile {
    param([string]$SqlPath)
    Invoke-PsqlFile $SqlPath
}

Write-Host 'Local seed completed.'
