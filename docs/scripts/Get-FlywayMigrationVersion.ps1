# Flyway encodes a dotted version with underscores: V81_5__name.sql is version 81.5.

function Get-FlywayMigrationVersionText([string]$Name) {
    if ($Name -notmatch '^V(\d+(?:_\d+)*)__') {
        throw "Unrecognized Flyway migration file name: $Name"
    }
    return ($Matches[1] -replace '_', '.')
}

function Get-FlywayMigrationSortKey([string]$Name) {
    $parts = [System.Collections.Generic.List[int]]::new()
    foreach ($part in (Get-FlywayMigrationVersionText $Name).Split('.')) {
        $parts.Add([int]$part)
    }
    while ($parts.Count -lt 4) {
        $parts.Add(0)
    }
    return (($parts | ForEach-Object { '{0:D8}' -f $_ }) -join '.')
}
