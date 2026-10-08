[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$DatabaseUrl,
    [Parameter(Mandatory)]
    [string]$DatabaseUser,
    [Parameter(Mandatory)]
    [string]$DatabasePassword
)

$ErrorActionPreference = 'Stop'
if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw 'psql is required for the read-only Flyway audit.'
}

$env:PGPASSWORD = $DatabasePassword
try {
    $history = & psql --dbname=$DatabaseUrl --username=$DatabaseUser --no-align --tuples-only --field-separator='|' --set=ON_ERROR_STOP=1 --command "SELECT version, success FROM flyway_schema_history WHERE version IN ('38','39','40','41','42','43','44','45') ORDER BY version::int;"
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to read flyway_schema_history.'
    }

    $expectedVersions = 38..45 | ForEach-Object { [string]$_ }
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

    $counts = & psql --dbname=$DatabaseUrl --username=$DatabaseUser --no-align --tuples-only --field-separator='|' --set=ON_ERROR_STOP=1 --command "SELECT (SELECT count(*) FROM quests), (SELECT count(*) FROM quest_steps), (SELECT count(*) FROM discovery_points), (SELECT count(*) FROM quest_minigames), (SELECT count(*) FROM artifacts WHERE unlock_minigame_id IS NOT NULL);"
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to read quest smoke counts.'
    }
    $countParts = $counts.Trim().Split('|')
    if ($countParts.Count -ne 5 -or ($countParts | ForEach-Object { [int]$_ }) -contains 0) {
        throw "Quest smoke count is incomplete: $counts"
    }
    Write-Host "Flyway V38-V45 passed. quests=$($countParts[0]) steps=$($countParts[1]) discovery=$($countParts[2]) minigames=$($countParts[3]) minigameArtifacts=$($countParts[4])"
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
