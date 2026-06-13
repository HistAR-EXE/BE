# Chạy SQL migration + seed vào Postgres Docker (HistAR full stack)
param(
  [string]$Container = "histar-postgres",
  [string]$DbUser = "timelens",
  [string]$DbName = "timelens",
  [switch]$IncludeSchema
)

$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

if (-not (docker ps --format "{{.Names}}" | Select-String -SimpleMatch $Container)) {
  $fallback = docker ps --filter "name=histar-postgres" --format "{{.Names}}" | Select-Object -First 1
  if ($fallback) { $Container = $fallback }
  else { throw "Container '$Container' không chạy. Chạy: docker compose up -d (từ thư mục HistAR)" }
}

Write-Host "Container: $Container"
Write-Host "Database: $DbName"

$sqlFiles = @()
if ($IncludeSchema) {
  $sqlFiles += "docs\database\TimeLens_DB_Schema.sql"
  $sqlFiles += "docs\database\2026-06-02_fe_compat_migration.sql"
}
$sqlFiles += @(
  "docs\database\2026-06-02_fe_compat_indexes_seed.sql",
  "docs\database\2026-06-02_fe_compat_data_topup.sql",
  "docs\database\2026-week3_update_panoramas_cu_chi.sql",
  "docs\database\2026-06-11_cp3_gamification_upgrade.sql",
  "docs\database\2026-06-11_admin_seed.sql",
  "docs\database\2026-06-11_unlock_rules.sql",
  "docs\database\2026-06-12_photo_scene_unlock_keys.sql",
  "docs\database\2026-06-12_discovery_artifact_links.sql",
  "docs\database\2026-06-12_quest_completion_trigger.sql",
  "docs\database\2026-06-12_value_layer_upgrade.sql",
  "docs\database\2026-06-13_phase_b_upgrade.sql",
  "docs\database\2026-06-14_phase1_hardening.sql",
  "docs\database\2026-06-15_visit_session_event_snapshot.sql",
  "docs\database\2026-06-16_analytics_event_metadata.sql"
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
