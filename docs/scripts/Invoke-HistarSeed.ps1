# Shared seed runner — reads BE/docs/seed-manifest.txt
param(
    [Parameter(Mandatory = $true)]
    [scriptblock]$RunSqlFile,

    [switch]$IncludeSchema
)

$ErrorActionPreference = 'Stop'
$docsRoot = Split-Path $PSScriptRoot -Parent
$manifest = Join-Path $docsRoot 'seed-manifest.txt'
$seedDir = Join-Path $docsRoot 'sql\seed'
$schemaDir = Join-Path $docsRoot 'sql\schema'

if (-not (Test-Path $manifest)) {
    throw "Missing seed manifest: $manifest"
}

if ($IncludeSchema) {
    foreach ($schemaFile in @(
            'TimeLens_DB_Schema.sql',
            '2026-06-02_fe_compat_migration.sql'
        )) {
        $path = Join-Path $schemaDir $schemaFile
        if (-not (Test-Path $path)) { throw "Schema file not found: $path" }
        & $RunSqlFile $path
    }
}

Get-Content $manifest | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) { return }
    $path = Join-Path $seedDir $line
    if (-not (Test-Path $path)) {
        throw "Seed file listed in manifest but missing: $path"
    }
    & $RunSqlFile $path
}
