# Update Render PostgreSQL from a developer machine.
#
# This script intentionally targets an external PostgreSQL connection. It does
# not use Docker, local bootstrap SQL, or database reset operations.
#
# Safe default flow:
#   1. Validate the external connection.
#   2. Apply pending Flyway migrations (through the latest V__ file; currently V87).
#   3. Apply the ordered seed manifest.
#   4. Verify the latest Flyway version is successful.
#
# Usage from BE/:
#   .\docs\scripts\run-all-seed-render.ps1 -ConfirmProduction
#   .\docs\scripts\run-all-seed-render.ps1 -ConfirmProduction -SkipSeeds
#   .\docs\scripts\run-all-seed-render.ps1 -ConfirmProduction -SkipMigrations
#
[CmdletBinding()]
param(
    [string]$DatabaseUrl,
    [string]$DatabaseUser,
    [string]$DatabasePassword,
    [string]$EnvFile,
    [switch]$ConfirmProduction,
    [switch]$SkipMigrations,
    [switch]$SkipSeeds,
    [switch]$AllowNonRenderHost,
    [string]$PsqlContainer = 'histar-postgres'
)

$ErrorActionPreference = 'Stop'
$beRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $beRoot
$ExpectedMinimumFlywayVersion = 87

if (-not $ConfirmProduction) {
    throw 'This script writes to an external database. Re-run with -ConfirmProduction after verifying the Render External Database URL and taking a backup.'
}

if (-not $EnvFile) {
    $EnvFile = Join-Path $beRoot '.env.render-db'
}

function Import-ConnectionFile([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) {
            return
        }

        if ($line -match '^([^=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

Import-ConnectionFile $EnvFile

if (-not $DatabaseUrl) {
    $DatabaseUrl = $env:DATABASE_URL
}

if (-not $DatabaseUrl -and $env:PGHOST) {
    $port = if ($env:PGPORT) { $env:PGPORT } else { '5432' }
    $DatabaseUrl = "postgresql://$($env:PGHOST):$port/$($env:PGDATABASE)"
}

if (-not $DatabaseUrl) {
    throw "No Render database connection found. Set DATABASE_URL or PGHOST/PGPORT/PGDATABASE in $EnvFile."
}

$DatabaseUrl = $DatabaseUrl -replace '^postgres://', 'postgresql://'
if ($DatabaseUrl -notmatch '^postgres(?:ql)?://') {
    throw 'DATABASE_URL must be a PostgreSQL URL beginning with postgresql://.'
}

$connectionUri = [Uri]$DatabaseUrl
if ($connectionUri.Host -in @('localhost', '127.0.0.1', '::1', 'histar-postgres', 'postgres')) {
    throw "Refusing local database host '$($connectionUri.Host)'. Use Render's External Database URL."
}

if (-not $AllowNonRenderHost -and $connectionUri.Host -notmatch '(^|\.)render\.com$') {
    throw "Host '$($connectionUri.Host)' is not a Render PostgreSQL host. Use the Render External Database URL or pass -AllowNonRenderHost explicitly for an approved external Postgres."
}

if ($connectionUri.Host -match '(^|\.)render\.com$' -and $DatabaseUrl -notmatch '(?i)(^|[?&])sslmode=require(&|$)') {
    $separator = if ($DatabaseUrl.Contains('?')) { '&' } else { '?' }
    $DatabaseUrl = "$DatabaseUrl${separator}sslmode=require"
}

if (-not $DatabaseUser) {
    $DatabaseUser = $env:PGUSER
}
if (-not $DatabasePassword) {
    $DatabasePassword = $env:PGPASSWORD
}

if (-not $DatabaseUser -and $connectionUri.UserInfo) {
    $DatabaseUser = [Uri]::UnescapeDataString(($connectionUri.UserInfo -split ':', 2)[0])
}
if (-not $DatabasePassword -and $connectionUri.UserInfo -match ':') {
    $DatabasePassword = [Uri]::UnescapeDataString(($connectionUri.UserInfo -split ':', 2)[1])
}

if (-not $DatabaseUser) {
    throw 'Database user is missing. Set PGUSER or pass -DatabaseUser.'
}
if (-not $DatabasePassword) {
    throw 'Database password is missing. Set PGPASSWORD or pass -DatabasePassword.'
}

$migrationDir = Join-Path $beRoot 'src\main\resources\db\migration'
$migrations = @(Get-ChildItem -LiteralPath $migrationDir -Filter 'V*__*.sql' |
    Sort-Object { [int]([regex]::Match($_.Name, '^V(\d+)__').Groups[1].Value) })
if ($migrations.Count -eq 0) {
    throw "No Flyway migrations found in $migrationDir"
}
$duplicateVersions = @($migrations |
    Group-Object { [regex]::Match($_.Name, '^V(\d+)__').Groups[1].Value } |
    Where-Object Count -gt 1)
if ($duplicateVersions.Count -gt 0) {
    $duplicates = ($duplicateVersions | ForEach-Object { "V$($_.Name): $($_.Group.Name -join ', ')" }) -join '; '
    throw "Duplicate Flyway migration versions found: $duplicates"
}
$latestVersion = [regex]::Match($migrations[-1].Name, '^V(\d+)__').Groups[1].Value
if ([int]$latestVersion -lt $ExpectedMinimumFlywayVersion) {
    throw "Expected migrations through at least V$ExpectedMinimumFlywayVersion, but the repository only contains V$latestVersion."
}

if (-not (Test-Path -LiteralPath (Join-Path $beRoot 'mvnw.cmd'))) {
    throw 'BE/mvnw.cmd was not found.'
}

$localPsql = Get-Command psql -ErrorAction SilentlyContinue
$dockerPsql = $false
if (-not $localPsql) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'psql is not installed and Docker is unavailable. Install the PostgreSQL client or start Docker Desktop with the histar-postgres container.'
    }
    $runningContainers = @(& docker ps --format '{{.Names}}')
    if ($LASTEXITCODE -ne 0 -or $runningContainers -notcontains $PsqlContainer) {
        throw "psql is not installed and Docker container '$PsqlContainer' is not running. Start Docker Desktop or install the PostgreSQL client."
    }
    $dockerPsql = $true
    Write-Host "Using psql from Docker container '$PsqlContainer' for the external Render connection."
}

$env:PGPASSWORD = $DatabasePassword
try {
    function Invoke-PsqlNative([string[]]$Arguments) {
        if ($dockerPsql) {
            & docker exec -e "PGPASSWORD=$DatabasePassword" $PsqlContainer psql @Arguments
        } else {
            & psql @Arguments
        }
        if ($LASTEXITCODE -ne 0) {
            throw 'psql command failed.'
        }
    }

    function Invoke-Psql([string]$Sql) {
        Invoke-PsqlNative @(
            "--dbname=$DatabaseUrl",
            "--username=$DatabaseUser",
            '--no-align',
            '--tuples-only',
            '--set=ON_ERROR_STOP=1',
            '--command',
            $Sql
        )
    }

    function Invoke-PsqlFile([string]$SqlPath) {
        if ($dockerPsql) {
            # The seed file lives on the Windows host, not inside the psql
            # container. Stream its contents over stdin instead of passing a
            # host path that the container cannot resolve.
            Get-Content -LiteralPath $SqlPath -Raw -Encoding UTF8 |
                & docker exec -i -e "PGPASSWORD=$DatabasePassword" $PsqlContainer psql `
                    "--dbname=$DatabaseUrl" `
                    "--username=$DatabaseUser" `
                    '--set=ON_ERROR_STOP=1'
        } else {
            & psql `
                "--dbname=$DatabaseUrl" `
                "--username=$DatabaseUser" `
                '--set=ON_ERROR_STOP=1' `
                "--file=$SqlPath"
        }
        if ($LASTEXITCODE -ne 0) {
            throw 'psql command failed.'
        }
    }

    Write-Host "Checking external PostgreSQL: $($connectionUri.Host) / $($connectionUri.AbsolutePath.TrimStart('/'))"
    $identity = (Invoke-Psql "SELECT current_database() || '|' || current_user;").Trim()
    if (-not $identity) {
        throw 'Could not connect to the external PostgreSQL database.'
    }
    Write-Host "Connected as $identity"

    $jdbcUrl = $DatabaseUrl -replace '^postgresql:', 'jdbc:postgresql:'
    if (-not $SkipMigrations) {
        Write-Host "Applying Flyway migrations through V$latestVersion..."
        & (Join-Path $beRoot 'mvnw.cmd') -q `
            "-Dflyway.url=$jdbcUrl" `
            "-Dflyway.user=$DatabaseUser" `
            "-Dflyway.password=$DatabasePassword" `
            flyway:migrate
        if ($LASTEXITCODE -ne 0) {
            throw 'Flyway migration failed. Seeds were not run.'
        }
    }

    $versionCheck = (Invoke-Psql "SELECT version || ':' || success FROM flyway_schema_history WHERE version = '$latestVersion' AND success = TRUE LIMIT 1;").Trim()
    if ($versionCheck -ne "${latestVersion}:t" -and $versionCheck -ne "${latestVersion}:true") {
        throw "Flyway V$latestVersion is not recorded as successful. Actual result: '$versionCheck'"
    }
    Write-Host "Flyway V$latestVersion is successful."

    if (-not $SkipSeeds) {
        Write-Host 'Applying ordered seed manifest...'
        & (Join-Path $PSScriptRoot 'Invoke-HistarSeed.ps1') -RunSqlFile {
            param([string]$SqlPath)
            $name = Split-Path $SqlPath -Leaf
            Write-Host ">> $name"
            Invoke-PsqlFile $SqlPath
        }
    }

    Write-Host "Render PostgreSQL update completed successfully through V$latestVersion."
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
