# Seed Render / external Postgres using BE/.env.render-db (DATABASE_URL or PG* vars).
# Run from BE/: .\docs\scripts\run-all-seed-render.ps1
param(
    [switch]$IncludeSchema
)

$ErrorActionPreference = 'Stop'
$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $beRoot

$envFile = Join-Path $beRoot '.env.render-db'
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim().Trim('"')
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

if ($env:DATABASE_URL) {
    $conn = $env:DATABASE_URL
}
elseif ($env:PGHOST) {
    $conn = "postgresql://${env:PGUSER}:${env:PGPASSWORD}@${env:PGHOST}:$($env:PGPORT ?? '5432')/${env:PGDATABASE}"
}
else {
    throw 'Set DATABASE_URL or PGHOST/PGUSER/PGPASSWORD/PGDATABASE in BE/.env.render-db'
}

function Invoke-PsqlFile([string]$SqlPath) {
    $name = Split-Path $SqlPath -Leaf
    Write-Host ">> $name"
    psql $conn -v ON_ERROR_STOP=1 -f $SqlPath
    if ($LASTEXITCODE -ne 0) { throw "psql failed: $SqlPath" }
}

& (Join-Path $PSScriptRoot 'Invoke-HistarSeed.ps1') -IncludeSchema:$IncludeSchema -RunSqlFile {
    param([string]$SqlPath)
    Invoke-PsqlFile $SqlPath
}

Write-Host 'Render seed completed.'
