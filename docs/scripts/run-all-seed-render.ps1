# Seed Postgres trên Render (External URL) — chạy từ laptop, một lần trước khi BE start.
# Không có auto-init trong Spring (ddl-auto=validate) — phải chạy script này thủ công.
#
# Yêu cầu: Docker (khuyến nghị) hoặc psql cài sẵn trên máy.
#
# Windows chặn .ps1? Dùng file .cmd (không cần ExecutionPolicy):
#   cd BE
#   docs\scripts\run-all-seed-render.cmd -IncludeSchema
#
# Hoặc Bypass trực tiếp:
#   powershell -ExecutionPolicy Bypass -File .\docs\scripts\run-all-seed-render.ps1 `
#     -PgHost "dpg-d9517c7lk1mc73c1u8sg-a.singapore-postgres.render.com" `
#     -Password "YOUR_PASSWORD" -IncludeSchema

param(
  [Parameter(Mandatory = $true)]
  [string]$PgHost,

  [Parameter(Mandatory = $true)]
  [string]$Password,

  [string]$DbUser = "timelens",
  [string]$DbName = "timelens",
  [int]$Port = 5432,
  [switch]$IncludeSchema,
  [switch]$UseLocalPsql
)

$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

$sqlFiles = @()
if ($IncludeSchema) {
  $sqlFiles += @(
    "docs\database\TimeLens_DB_Schema.sql",
    "docs\database\2026-06-02_fe_compat_migration.sql"
  )
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
  "docs\database\2026-06-30_cu_chi_supplementary_assets.sql",
  "docs\database\2026-07-01_cu_chi_real_panoramas.sql",
  "docs\database\2026-07-04_location_unlock.sql",
  "docs\database\2026-07-04_discovery_bindings_enrich.sql",
  "docs\database\2026-07-04_quest_onsite_flag.sql",
  "docs\database\2026-07-04_profile_tier.sql",
  "docs\database\2026-07-04_org_members_seed.sql",
  "docs\database\2026-07-05_ensure_admin_accounts.sql",
  "docs\database\2026-07-06_org_rbac_and_groups.sql"
)

function Invoke-PsqlFile {
  param([string]$FilePath)

  if ($UseLocalPsql -or (Get-Command psql -ErrorAction SilentlyContinue)) {
    $env:PGPASSWORD = $Password
    $env:PGSSLMODE = "require"
    psql -h $PgHost -p $Port -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -f $FilePath
    if ($LASTEXITCODE -ne 0) { throw "psql failed: $FilePath" }
    return
  }

  $dir = Split-Path $FilePath -Parent
  $name = Split-Path $FilePath -Leaf
  docker run --rm `
    -v "${dir}:/seed:ro" `
    -e "PGPASSWORD=$Password" `
    -e "PGSSLMODE=require" `
    postgres:16-alpine `
    psql -h $PgHost -p $Port -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -f "/seed/$name"
  if ($LASTEXITCODE -ne 0) { throw "docker psql failed: $FilePath" }
}

Write-Host "Target: ${DbUser}@${PgHost}:${Port}/${DbName} (Render external, SSL)"
Write-Host "Files: $($sqlFiles.Count)"
if (-not $IncludeSchema) {
  Write-Host "Tip: DB moi tren Render can -IncludeSchema (schema + compat truoc)."
}

foreach ($rel in $sqlFiles) {
  $path = Join-Path $root $rel
  if (-not (Test-Path $path)) {
    throw "Missing SQL file: $path"
  }
  Write-Host "Running $rel ..."
  Invoke-PsqlFile -FilePath $path
}

Write-Host ""
Write-Host "Render Postgres seed completed OK."
