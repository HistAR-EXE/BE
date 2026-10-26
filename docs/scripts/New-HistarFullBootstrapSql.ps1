param(
    [string]$OutputPath = (Join-Path (Split-Path $PSScriptRoot -Parent) 'sql\bootstrap\TimeLens_full_bootstrap.sql')
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Get-FlywayMigrationVersion.ps1')
$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$docsRoot = Join-Path $beRoot 'docs'
$migrationDir = Join-Path $beRoot 'src\main\resources\db\migration'
$manifestPath = Join-Path $docsRoot 'seed-manifest.txt'
$postFlywayManifestPath = Join-Path $docsRoot 'seed-manifest-post-flyway.txt'

function Get-Utf8Sql([string]$Path) {
    if (-not (Test-Path $Path)) {
        throw "Required SQL source is missing: $Path"
    }

    return Get-Content -Raw -Encoding UTF8 $Path
}

function Add-SqlSection([System.Text.StringBuilder]$Builder, [string]$Label, [string]$Path) {
    [void]$Builder.AppendLine()
    [void]$Builder.AppendLine('-- ============================================================================')
    [void]$Builder.AppendLine("-- BEGIN $Label")
    [void]$Builder.AppendLine("-- Source: $Path")
    [void]$Builder.AppendLine('-- ============================================================================')
    [void]$Builder.AppendLine((Get-Utf8Sql $Path).TrimEnd())
    [void]$Builder.AppendLine()
    [void]$Builder.AppendLine("-- END $Label")
}

$migrations = @(Get-ChildItem $migrationDir -Filter 'V*__*.sql' |
    Sort-Object { Get-FlywayMigrationSortKey $_.Name })

if ($migrations.Count -eq 0) {
    throw "No Flyway migrations found in $migrationDir"
}

$lastMigration = $migrations[-1]
$lastVersion = Get-FlywayMigrationVersionText $lastMigration.Name
$builder = [System.Text.StringBuilder]::new()

[void]$builder.AppendLine('-- HistAR full local bootstrap SQL')
[void]$builder.AppendLine("-- Generated from schema, seed manifest, and Flyway migrations through V$lastVersion.")
[void]$builder.AppendLine('-- Run only on a new, empty local database with psql -v ON_ERROR_STOP=1 -f.')
[void]$builder.AppendLine('-- Do not run against shared, staging, or production databases.')
[void]$builder.AppendLine('\set ON_ERROR_STOP on')
[void]$builder.AppendLine('SET client_encoding TO ''UTF8'';')

Add-SqlSection $builder 'schema: TimeLens_DB_Schema.sql' (Join-Path $docsRoot 'sql\schema\TimeLens_DB_Schema.sql')
Add-SqlSection $builder 'schema: 2026-06-02_fe_compat_migration.sql' (Join-Path $docsRoot 'sql\schema\2026-06-02_fe_compat_migration.sql')

Get-Content $manifestPath | ForEach-Object {
    $seed = $_.Trim()
    if ($seed -and -not $seed.StartsWith('#')) {
        Add-SqlSection $builder "seed: $seed" (Join-Path $docsRoot "sql\seed\$seed")
    }
}

foreach ($migration in $migrations) {
    # Flyway executes PostgreSQL migrations in a transaction. Preserve that
    # boundary when flattening them for psql, including guarded V82 constraints.
    [void]$builder.AppendLine('BEGIN;')
    Add-SqlSection $builder "migration: $($migration.Name)" $migration.FullName
    [void]$builder.AppendLine('COMMIT;')
}

if (Test-Path $postFlywayManifestPath) {
    Get-Content $postFlywayManifestPath | ForEach-Object {
        $seed = $_.Trim()
        if ($seed -and -not $seed.StartsWith('#')) {
            Add-SqlSection $builder "seed: $seed" (Join-Path $docsRoot "sql\seed\$seed")
        }
    }
}

[void]$builder.AppendLine()
[void]$builder.AppendLine('-- ============================================================================')
[void]$builder.AppendLine("-- Flyway baseline after full bootstrap: V$lastVersion")
[void]$builder.AppendLine('-- ============================================================================')
[void]$builder.AppendLine('CREATE TABLE IF NOT EXISTS flyway_schema_history (')
[void]$builder.AppendLine('    installed_rank INTEGER NOT NULL,')
[void]$builder.AppendLine('    version VARCHAR(50),')
[void]$builder.AppendLine('    description VARCHAR(200) NOT NULL,')
[void]$builder.AppendLine('    type VARCHAR(20) NOT NULL,')
[void]$builder.AppendLine('    script VARCHAR(1000) NOT NULL,')
[void]$builder.AppendLine('    checksum INTEGER,')
[void]$builder.AppendLine('    installed_by VARCHAR(100) NOT NULL,')
[void]$builder.AppendLine('    installed_on TIMESTAMP NOT NULL DEFAULT NOW(),')
[void]$builder.AppendLine('    execution_time INTEGER NOT NULL,')
[void]$builder.AppendLine('    success BOOLEAN NOT NULL,')
[void]$builder.AppendLine('    PRIMARY KEY (installed_rank)')
[void]$builder.AppendLine(');')
[void]$builder.AppendLine()
[void]$builder.AppendLine('DO $$')
[void]$builder.AppendLine('BEGIN')
[void]$builder.AppendLine('    IF EXISTS (SELECT 1 FROM flyway_schema_history) THEN')
[void]$builder.AppendLine("        RAISE EXCEPTION 'Refusing to overwrite existing Flyway history. Use a new database.';")
[void]$builder.AppendLine('    END IF;')
[void]$builder.AppendLine('END $$;')
[void]$builder.AppendLine()
[void]$builder.AppendLine('INSERT INTO flyway_schema_history (')
[void]$builder.AppendLine('    installed_rank, version, description, type, script,')
[void]$builder.AppendLine('    checksum, installed_by, installed_on, execution_time, success')
[void]$builder.AppendLine(') VALUES (')
[void]$builder.AppendLine("    1, '$lastVersion', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>',")
[void]$builder.AppendLine("    NULL, 'histar-full-bootstrap', NOW(), 0, TRUE")
[void]$builder.AppendLine(');')

$outputDirectory = Split-Path $OutputPath -Parent
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
Set-Content -Path $OutputPath -Value $builder.ToString() -Encoding UTF8

Write-Host "Generated $OutputPath through Flyway V$lastVersion"
