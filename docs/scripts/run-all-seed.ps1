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
  "docs\database\2026-06-16_analytics_event_metadata.sql",
  "docs\database\2026-06-17_cu_chi_artifacts_story_admin.sql",
  "docs\database\2026-06-18_cu_chi_only_cleanup.sql",
  "docs\database\2026-06-19_heritage_sites_from_dataset.sql",
  "docs\database\2026-06-20_fix_den_hung_vuong_name.sql",
  "docs\database\2026-06-21_heritage_quests_seed.sql",
  "docs\database\2026-06-22_fix_quest_progress_current_step.sql",
  "docs\database\2026-06-23_quest_mission_keys.sql",
  "docs\database\2026-06-24_quest_visual_artifact_keys.sql",
  "docs\database\2026-06-25_quest_mixed_visual_pipeline.sql",
  "docs\database\2026-06-26_heritage_quest_discovery_points.sql",
  "docs\database\2026-06-27_heritage_p2_checkin_bonus.sql",
  "docs\database\2026-06-28_cu_chi_streetview_panoramas.sql",
  "docs\database\2026-06-29_fix_cu_chi_panorama_utf8.sql",
  "docs\database\2026-06-29_cu_chi_panorama_jpg_ext.sql",
  "docs\database\2026-06-30_cu_chi_supplementary_assets.sql"
)

foreach ($rel in $sqlFiles) {
  $path = Join-Path $root $rel
  if (-not (Test-Path $path)) {
    throw "Missing SQL file: $path"
  }
  Write-Host "Running $rel ..."
  $containerPath = "/tmp/seed-$([guid]::NewGuid().ToString('n')).sql"
  docker cp $path "${Container}:${containerPath}"
  docker exec $Container psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -f $containerPath
  if ($LASTEXITCODE -ne 0) {
    throw "SQL failed: $rel (exit $LASTEXITCODE)"
  }
}

Write-Host ""
Write-Host "Migration/seed SQL completed OK."
