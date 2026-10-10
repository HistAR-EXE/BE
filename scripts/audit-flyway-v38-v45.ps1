[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$DatabaseUrl,
    [Parameter(Mandatory)]
    [string]$DatabaseUser,
    [Parameter(Mandatory)]
    [string]$DatabasePassword
)

# Historical filename retained for existing runbook references; the audit covers V38–V57.

$ErrorActionPreference = 'Stop'
$hasPsql = $null -ne (Get-Command psql -ErrorAction SilentlyContinue)
if (-not $hasPsql -and -not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'psql or Docker is required for the read-only Flyway audit.'
}

function Invoke-ReadOnlyQuery([string]$Query) {
    if ($hasPsql) {
        return & psql --dbname=$psqlDatabaseUrl --username=$DatabaseUser --no-align --tuples-only --field-separator='|' --set=ON_ERROR_STOP=1 --command $Query
    }

    # Keep the audit usable on developer/CI hosts that do not install the
    # PostgreSQL client. Docker receives the password through its environment,
    # never in a command line or log.
    $uri = [Uri]$psqlDatabaseUrl
    $database = $uri.AbsolutePath.TrimStart('/')
    return & docker run --rm --env PGPASSWORD --env PGSSLMODE=require postgres:17 `
        psql --host $uri.Host --port $uri.Port --username $DatabaseUser --dbname $database `
        --no-align --tuples-only --field-separator='|' --set=ON_ERROR_STOP=1 --command $Query
}

$env:PGPASSWORD = $DatabasePassword
# psql understands PostgreSQL connection URIs, but not JDBC URLs copied from
# Spring/Render configuration.  Normalize the latter so the runbook works as
# written without leaking credentials into a command line.
$psqlDatabaseUrl = $DatabaseUrl -replace '^jdbc:', ''
try {
    $history = Invoke-ReadOnlyQuery "SELECT version, success FROM flyway_schema_history WHERE version BETWEEN '38' AND '57' ORDER BY version::int;"
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to read flyway_schema_history.'
    }

    $expectedVersions = 38..57 | ForEach-Object { [string]$_ }
    $observed = @{}
    foreach ($line in $history) {
        $parts = $line.Trim().Split('|')
        if ($parts.Count -eq 2) {
            $observed[$parts[0]] = $parts[1]
        }
    }
    foreach ($version in $expectedVersions) {
        if (-not $observed.ContainsKey($version) -or $observed[$version] -ne 't') {
            throw "Flyway migration V$version is missing or unsuccessful."
        }
    }

    $counts = Invoke-ReadOnlyQuery "SELECT (SELECT count(*) FROM quests), (SELECT count(*) FROM quest_steps), (SELECT count(*) FROM discovery_points), (SELECT count(*) FROM quest_minigames), (SELECT count(*) FROM artifacts WHERE unlock_minigame_id IS NOT NULL);"
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to read quest smoke counts.'
    }
    $countParts = $counts.Trim().Split('|')
    if ($countParts.Count -ne 5 -or ($countParts | ForEach-Object { [int]$_ }) -contains 0) {
        throw "Quest smoke count is incomplete: $counts"
    }
    Write-Host "Flyway V38-V57 passed. quests=$($countParts[0]) steps=$($countParts[1]) discovery=$($countParts[2]) minigames=$($countParts[3]) minigameArtifacts=$($countParts[4])"
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
