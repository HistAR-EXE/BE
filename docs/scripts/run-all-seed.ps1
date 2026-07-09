# Chạy SQL migration + seed vào Postgres Docker (HistAR full stack)
param(
  [string]$Container = "histar-postgres",
  [string]$DbUser = "timelens",
  [string]$DbName = "timelens",
  [switch]$IncludeSchema
)

$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

$schemaFiles = @(
  "docs\database\TimeLens_DB_Schema.sql",
  "docs\database\2026-06-02_fe_compat_migration.sql"
)

if (-not (docker ps --format "{{.Names}}" | Select-String -SimpleMatch $Container)) {
  $fallback = docker ps --filter "name=histar-postgres" --format "{{.Names}}" | Select-Object -First 1
  if ($fallback) { $Container = $fallback }
  else { throw "Container '$Container' không chạy. Chạy: docker compose up -d (từ thư mục HistAR)" }
}

Write-Host "Container: $Container"
Write-Host "Database: $DbName"

$sqlFiles = @()
$runSchema = $false

function Invoke-PsqlCommand([string]$Sql) {
  $output = docker exec $Container psql -U $DbUser -d $DbName -tAc $Sql 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "psql query failed: $Sql`n$output"
  }
  return [string]$output
}

function Test-DatabaseHasSchema {
  $exists = (Invoke-PsqlCommand "SELECT to_regclass('public.profiles') IS NOT NULL;").Trim()
  return $exists -match '^(t|true|1)$'
}

if ($IncludeSchema) {
  if (Test-DatabaseHasSchema) {
    Write-Host "[WARN] DB da co schema (bang profiles ton tai)." -ForegroundColor Yellow
    Write-Host "       Bo qua TimeLens_DB_Schema + fe_compat_migration; chi chay upgrade/migration."
    Write-Host "       DB trong moi: dung reset-and-seed-render.ps1 de DROP SCHEMA truoc."
  }
  else {
    $runSchema = $true
    $sqlFiles += $schemaFiles
  }
}
else {
  Write-Host "Mode: upgrade only (khong tao schema tu dau)."
  Write-Host "Tip: DB trong moi: dung -IncludeSchema de tạo schema từ đầu."
}

# Chạy sau Flyway để đảm bảo cột billing/email đã có
$postSeedFiles = @(
  "docs\database\2026-07-10_demo_billing_accounts.sql",
  "docs\database\2026-07-10_flyway_baseline_after_manual_seed.sql"
)

$coreSqlFiles = @(
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
  "docs\database\2026-07-06_org_rbac_and_groups.sql",
  "docs\database\2026-07-07_monetization_p0.sql",
  "docs\database\2026-07-08_quest_steps_schema.sql",
  "docs\database\2026-07-08_quest_steps_init.sql",
  "docs\database\2026-07-09_shedlock.sql",
  "docs\database\2026-07-10_visit_sessions_ai_fields.sql"
)

function Get-RelativeSqlPath([string]$FullPath) {
  return $FullPath.Substring($root.Length + 1).Replace('/', '\')
}

function Get-ExtraDocsSqlFiles {
  $docsDir = Join-Path $root "docs\database"
  $known = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
  foreach ($item in ($schemaFiles + $sqlFiles + $coreSqlFiles + $postSeedFiles)) {
    [void]$known.Add($item)
  }

  Get-ChildItem -Path $docsDir -Filter *.sql -File |
    Sort-Object Name |
    ForEach-Object {
      $rel = Get-RelativeSqlPath $_.FullName
      if (-not $known.Contains($rel)) {
        $rel
      }
    }
}

function Get-FlywaySqlFiles {
  $migrationDir = Join-Path $root "src\main\resources\db\migration"
  if (-not (Test-Path $migrationDir)) {
    return @()
  }

  Get-ChildItem -Path $migrationDir -Filter *.sql -File |
    Sort-Object {
      if ($_.BaseName -match '^V(\d+)__') { [int]$matches[1] } else { 999999 }
    }, Name |
    ForEach-Object { Get-RelativeSqlPath $_.FullName }
}

$sqlFiles += $coreSqlFiles
$sqlFiles += Get-ExtraDocsSqlFiles
$sqlFiles += Get-FlywaySqlFiles
$sqlFiles += $postSeedFiles
$sqlFiles = $sqlFiles | Select-Object -Unique

Write-Host "Files: $($sqlFiles.Count)$(if ($runSchema) { ' (gom schema)' } else { ' (upgrade)' })"

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
