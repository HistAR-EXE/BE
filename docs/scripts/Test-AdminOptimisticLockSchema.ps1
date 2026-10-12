param(
    [string]$Database = "timelens",
    [string]$Container = "histar-postgres",
    [string]$DbUser = "timelens"
)

$ErrorActionPreference = "Stop"

$tables = @("discovery_points", "artifacts", "quests")

foreach ($table in $tables) {
    $sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '$table' AND column_name = 'row_version';"
    $result = (& docker exec $Container psql -U $DbUser -d $Database -tAc $sql | Out-String).Trim()

    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect $table.row_version in database '$Database'."
    }

    if ($result -ne "1") {
        throw "Missing optimistic-lock column: $table.row_version"
    }
}

Write-Host "Admin optimistic-lock schema is present in database '$Database'."
