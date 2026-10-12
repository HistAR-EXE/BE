param(
    [string]$Database = 'timelens_full',
    [string]$Container = 'histar-postgres',
    [string]$DbUser = 'timelens',
    [switch]$Reset
)

$ErrorActionPreference = 'Stop'
$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$bootstrapPath = Join-Path $beRoot 'docs\sql\bootstrap\TimeLens_full_bootstrap.sql'
$migrationDir = Join-Path $beRoot 'src\main\resources\db\migration'

function Assert-SafeIdentifier([string]$Value, [string]$Label) {
    if ($Value -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
        throw "$Label must contain only letters, numbers, and underscores."
    }
}

Assert-SafeIdentifier $Database 'Database name'
Assert-SafeIdentifier $DbUser 'Database user'

if (-not (Test-Path $bootstrapPath)) {
    throw "Bootstrap SQL is missing: $bootstrapPath. Run .\docs\scripts\New-HistarFullBootstrapSql.ps1 first."
}

$containers = @(& docker ps --format '{{.Names}}')
if ($LASTEXITCODE -ne 0 -or $containers -notcontains $Container) {
    throw "Docker container '$Container' is not running. Start Docker Compose before bootstrapping."
}

$databaseExistsOutput = @(& docker exec $Container psql -U $DbUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$Database';")
$databaseExists = ($databaseExistsOutput -join '').Trim()
if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect database '$Database' in container '$Container'."
}

if ($databaseExists) {
    if (-not $Reset) {
        throw "Refusing to overwrite an existing database without -Reset: $Database"
    }

    Write-Host "Resetting local database $Database..."
    & docker exec $Container psql -U $DbUser -d postgres -v ON_ERROR_STOP=1 `
        -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$Database' AND pid <> pg_backend_pid();" `
        -c "DROP DATABASE $Database;"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not reset database '$Database'."
    }
}

if (-not $databaseExists -or $Reset) {
    & docker exec $Container psql -U $DbUser -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $Database OWNER $DbUser;"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not create database '$Database'."
    }
}

Write-Host "Applying full bootstrap SQL to $Database..."
$containerBootstrapPath = "/tmp/histar-full-bootstrap-$Database.sql"
& docker cp $bootstrapPath "${Container}:$containerBootstrapPath"
if ($LASTEXITCODE -ne 0) {
    throw "Could not copy bootstrap SQL into container '$Container'."
}

$bootstrapExitCode = 0
try {
    & docker exec $Container psql -v ON_ERROR_STOP=1 -U $DbUser -d $Database -f $containerBootstrapPath
    $bootstrapExitCode = $LASTEXITCODE
} finally {
    & docker exec $Container rm -f $containerBootstrapPath | Out-Null
}

if ($bootstrapExitCode -ne 0) {
    throw "Bootstrap failed. Database '$Database' was not marked as ready."
}

$migrations = Get-ChildItem $migrationDir -Filter 'V*__*.sql' |
    Sort-Object { [int]([regex]::Match($_.Name, '^V(\d+)__').Groups[1].Value) }
$lastVersion = [regex]::Match($migrations[-1].Name, '^V(\d+)__').Groups[1].Value
$expectedBaseline = "${lastVersion}:BASELINE:true"
$historySql = "SELECT version || ':' || type || ':' || success FROM flyway_schema_history ORDER BY installed_rank;"
$history = @(& docker exec $Container psql -U $DbUser -d $Database -tAc $historySql)
if ($LASTEXITCODE -ne 0 -or $history -notcontains $expectedBaseline) {
    throw "Bootstrap completed SQL execution but Flyway baseline V$lastVersion was not recorded."
}

Write-Host "Bootstrap completed successfully: $Database is seeded and baselined at Flyway V$lastVersion."
