# Chạy SQL migration + seed (CP3 Tuần 1-3) vào Postgres Docker
# Lưu ý: docker compose tự chạy TimeLens_DB_Schema.sql lần đầu (volume mới).
# Script này chạy các file 2-6. Dùng -IncludeSchema chỉ khi DB trống KHÔNG qua docker init.
param(
  [string]$Container = "timelens-postgres",
  [string]$DbUser = "timelens",
  [string]$DbName = "timelens",
  [switch]$IncludeSchema
)

$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

if (-not (docker ps --format "{{.Names}}" | Select-String -SimpleMatch $Container)) {
  $fallback = docker ps --filter "name=timelens-postgres" --format "{{.Names}}" | Select-Object -First 1
  if ($fallback) { $Container = $fallback }
  else { throw "Container '$Container' không chạy. Chạy: docker compose up -d" }
}

Write-Host "Container: $Container"
Write-Host "Database: $DbName"

$sqlFiles = @()
if ($IncludeSchema) {
  $sqlFiles += "docs\database\TimeLens_DB_Schema.sql"
}
$sqlFiles += @(
  "docs\database\2026-06-02_fe_compat_migration.sql",
  "docs\database\2026-06-02_fe_compat_indexes_seed.sql",
  "docs\database\2026-06-02_fe_compat_data_topup.sql",
  "docs\week-1\2026-week1_location_sources.sql",
  "docs\week-1\2026-week1_cu_chi_knowledge.sql",
  "docs\week-3\2026-week3_update_panoramas_cu_chi.sql"
)

foreach ($rel in $sqlFiles) {
  $path = Join-Path $root $rel
  if (-not (Test-Path $path)) {
    throw "Missing SQL file: $path"
  }
  Write-Host "Running $rel ..."
  Get-Content $path -Raw -Encoding UTF8 | docker exec -i $Container psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1
  if ($LASTEXITCODE -ne 0) {
    throw "SQL failed: $rel (exit $LASTEXITCODE)"
  }
}

Write-Host ""
Write-Host "Migration/seed SQL completed OK."
