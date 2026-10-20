param(
    [string]$Database = 'timelens_full',
    [string]$Container = 'histar-postgres',
    [string]$DbUser = 'timelens',
    [switch]$Reset,
    # Keep committed bootstrap artifact in sync with seed-manifest (default: on).
    [switch]$SkipRegenerateBootstrap,
    # Idempotent demo revenue/audit top-up for Admin dashboard chart (default: on).
    [switch]$SkipDashboardRevenueSeed,
    # UTF-8-safe re-apply of Admin Billing CRM Kanban seed (V71 content; default: on).
    [switch]$SkipBillingCrmSeed,
    # UTF-8-safe Admin Analytics Cu Chi demo seed (docs/sql/seed; default: on).
    [switch]$SkipAnalyticsSeed,
    # Validate migration ordering/version without regenerating files or using Docker.
    [switch]$ValidateOnly
)

$ErrorActionPreference = 'Stop'
$beRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$bootstrapPath = Join-Path $beRoot 'docs\sql\bootstrap\TimeLens_full_bootstrap.sql'
$migrationDir = Join-Path $beRoot 'src\main\resources\db\migration'
$regenScript = Join-Path $PSScriptRoot 'New-HistarFullBootstrapSql.ps1'
$dashboardRevenueSeedScript = Join-Path $PSScriptRoot 'seed-admin-dashboard-revenue.ps1'
$dashboardRevenueSeed = Join-Path $beRoot 'docs\sql\seed\2026-10-15_admin_dashboard_revenue_demo.sql'
$billingCrmSeedScript = Join-Path $PSScriptRoot 'seed-admin-billing-crm.ps1'
$analyticsSeedScript = Join-Path $PSScriptRoot 'seed-admin-analytics.ps1'
$auditIpMigration = Join-Path $migrationDir 'V73__admin_audit_client_ip_location_seed.sql'
# Minimum Flyway version this bootstrap script knows how to verify end-to-end.
# Keep this aligned with the latest migration-specific smoke checks below.
$ExpectedMinFlywayVersion = 90

$migrations = @(Get-ChildItem $migrationDir -Filter 'V*__*.sql' |
    Sort-Object { [int]([regex]::Match($_.Name, '^V(\d+)__').Groups[1].Value) })
if ($migrations.Count -eq 0) {
    throw "No Flyway migrations found in $migrationDir"
}
$duplicateVersions = @($migrations |
    Group-Object { [regex]::Match($_.Name, '^V(\d+)__').Groups[1].Value } |
    Where-Object Count -gt 1)
if ($duplicateVersions.Count -gt 0) {
    $duplicates = ($duplicateVersions | ForEach-Object { "V$($_.Name): $($_.Group.Name -join ', ')" }) -join '; '
    throw "Duplicate Flyway migration versions found: $duplicates"
}
$lastMigrationName = $migrations[-1].Name
$lastVersion = [int]([regex]::Match($lastMigrationName, '^V(\d+)__').Groups[1].Value)
if ($lastVersion -lt $ExpectedMinFlywayVersion) {
    throw "Expected Flyway migrations through at least V$ExpectedMinFlywayVersion, but latest file is $lastMigrationName (V$lastVersion)."
}

if ($ValidateOnly) {
    Write-Output "Full bootstrap runner validation passed; latest migration: V$lastVersion."
    return
}

function Assert-SafeIdentifier([string]$Value, [string]$Label) {
    if ($Value -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
        throw "$Label must contain only letters, numbers, and underscores."
    }
}

Assert-SafeIdentifier $Database 'Database name'
Assert-SafeIdentifier $DbUser 'Database user'

if (-not $SkipRegenerateBootstrap) {
    if (-not (Test-Path $regenScript)) {
        throw "Bootstrap generator is missing: $regenScript"
    }
    Write-Host 'Regenerating TimeLens_full_bootstrap.sql from schema + seed-manifest + migrations...'
    & $regenScript -OutputPath $bootstrapPath
    if (-not (Test-Path $bootstrapPath)) {
        throw 'Failed to regenerate full bootstrap SQL from seed-manifest.'
    }
}

if (-not (Test-Path $bootstrapPath)) {
    throw "Bootstrap SQL is missing: $bootstrapPath. Run .\docs\scripts\New-HistarFullBootstrapSql.ps1 first."
}

$containers = @(& docker ps --format '{{.Names}}')
if ($LASTEXITCODE -ne 0 -or $containers -notcontains $Container) {
    throw "Docker container '$Container' is not running. Start Docker Compose before bootstrapping."
}

$databaseExistsOutput = @(& docker exec $Container psql -U $DbUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$Database';")
$databaseExists = ($databaseExistsOutput -join '').Trim()
if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect database '$Database' in container '$Container'."
}

if ($databaseExists) {
    if (-not $Reset) {
        throw "Refusing to overwrite an existing database without -Reset: $Database"
    }

    Write-Host "Resetting local database $Database..."
    & docker exec $Container psql -U $DbUser -d postgres -v ON_ERROR_STOP=1 `
        -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$Database' AND pid <> pg_backend_pid();" `
        -c "DROP DATABASE $Database;"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not reset database '$Database'."
    }
}

if (-not $databaseExists -or $Reset) {
    & docker exec $Container psql -U $DbUser -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $Database OWNER $DbUser;"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not create database '$Database'."
    }
}

Write-Host "Applying full bootstrap SQL to $Database..."
$containerBootstrapPath = "/tmp/histar-full-bootstrap-$Database.sql"
& docker cp $bootstrapPath "${Container}:$containerBootstrapPath"
if ($LASTEXITCODE -ne 0) {
    throw "Could not copy bootstrap SQL into container '$Container'."
}

$bootstrapExitCode = 0
try {
    & docker exec $Container psql -v ON_ERROR_STOP=1 -U $DbUser -d $Database -f $containerBootstrapPath
    $bootstrapExitCode = $LASTEXITCODE
} finally {
    & docker exec $Container rm -f $containerBootstrapPath | Out-Null
}

if ($bootstrapExitCode -ne 0) {
    throw "Bootstrap failed. Database '$Database' was not marked as ready."
}

$historySql = "SELECT version || ':' || type || ':' || success FROM flyway_schema_history ORDER BY installed_rank;"
$history = @(& docker exec $Container psql -U $DbUser -d $Database -tAc $historySql |
    ForEach-Object { $_.Trim() })
$baselineFound = $history | Where-Object {
    $_ -match "^${lastVersion}:BASELINE:(t|true)$"
}
if ($LASTEXITCODE -ne 0 -or -not $baselineFound) {
    throw "Bootstrap completed SQL execution but Flyway baseline V$lastVersion was not recorded (latest migration: $lastMigrationName)."
}

if (-not $SkipDashboardRevenueSeed) {
    if (-not (Test-Path $dashboardRevenueSeed)) {
        throw "Dashboard revenue seed is missing: $dashboardRevenueSeed"
    }
    if (-not (Test-Path $dashboardRevenueSeedScript)) {
        throw "Dashboard revenue seed script is missing: $dashboardRevenueSeedScript"
    }
    # Idempotent top-up (also appended after Flyway migrations in TimeLens_full_bootstrap).
    & $dashboardRevenueSeedScript -Container $Container -Database $Database -User $DbUser
}

if (-not $SkipBillingCrmSeed) {
    if (-not (Test-Path $billingCrmSeedScript)) {
        throw "Billing CRM seed script is missing: $billingCrmSeedScript"
    }
    & $billingCrmSeedScript -Container $Container -Database $Database -User $DbUser
}

if (-not $SkipAnalyticsSeed) {
    if (-not (Test-Path $analyticsSeedScript)) {
        throw "Analytics seed script is missing: $analyticsSeedScript"
    }
    & $analyticsSeedScript -Container $Container -Database $Database -User $DbUser
}

# Re-apply V73 after post-bootstrap seeds so newly inserted audit rows also get clientIp/location.
if (-not (Test-Path $auditIpMigration)) {
    throw "Flyway V73 migration is missing: $auditIpMigration"
}
$remoteV73 = '/tmp/histar-v73-audit-ip-seed.sql'
& docker cp $auditIpMigration "${Container}:${remoteV73}"
if ($LASTEXITCODE -ne 0) {
    throw "Could not copy V73 audit IP seed into container '$Container'."
}
try {
    & docker exec $Container psql -U $DbUser -d $Database -v ON_ERROR_STOP=1 -f $remoteV73
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to re-apply V73 admin audit clientIp/location seed.'
    }
} finally {
    & docker exec $Container rm -f $remoteV73 | Out-Null
}

# --- Smoke checks for recent Flyway chain (V70–V88) ---
# V70: organizations.ops_note for Admin org detail.
$opsNoteReady = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'organizations' AND column_name = 'ops_note';") -join '').Trim()
if ($opsNoteReady -ne '1') {
    throw "Bootstrap baselined at V$lastVersion but organizations.ops_note is missing (expected Flyway V70)."
}
$opsNoteSeeded = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM organizations WHERE ops_note IS NOT NULL AND BTRIM(ops_note) <> '';") -join '').Trim()

# V71: Admin Billing CRM Kanban demo leads.
$crmSeeded = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM heritage_digitization_inquiries WHERE contact_email LIKE 'seed-crm-%@histar.vn';") -join '').Trim()
if ([int]$crmSeeded -lt 1) {
    throw "Bootstrap baselined at V$lastVersion but V71 CRM seed rows are missing (seed-crm-* inquiries)."
}

# V72 / analytics demo: seeded analytics profiles (also applied via seed-admin-analytics.ps1).
$analyticsUsers = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM profiles WHERE email LIKE 'seed-analytics-%@histar.vn';") -join '').Trim()
if ([int]$analyticsUsers -lt 1) {
    throw "Bootstrap baselined at V$lastVersion but analytics demo profiles are missing (seed-analytics-*@histar.vn)."
}

# V73: admin_audit_events carry clientIp in metadata_json.
$auditWithIp = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM admin_audit_events WHERE metadata_json::jsonb ? 'clientIp' OR metadata_json::jsonb ? 'ip';") -join '').Trim()
if ([int]$auditWithIp -lt 1) {
    throw "Bootstrap baselined at V$lastVersion but V73 audit clientIp/location metadata is missing."
}

# V77: AR readiness is explicitly seeded for the Củ Chi pilot.
$cuChiArReady = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM locations WHERE id = '11111111-1111-1111-1111-111111111111' AND is_ar_available = TRUE;") -join '').Trim()
if ([int]$cuChiArReady -ne 1) {
    throw "Bootstrap baselined at V$lastVersion but V77 Củ Chi AR readiness is not TRUE."
}

# V78: Củ Chi stations have the placeholder coordinates required by the local pilot.
$stationCoordinates = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM stations WHERE site_code = 'cu-chi' AND code IN ('ST01','ST02','ST03','ST04','ST05','ST06') AND lat IS NOT NULL AND lng IS NOT NULL;") -join '').Trim()
if ([int]$stationCoordinates -lt 6) {
    throw "Bootstrap baselined at V$lastVersion but V78 Củ Chi station coordinates are incomplete ($stationCoordinates/6)."
}

# V79: local platform accounts are present and verified after a reset.
$platformAccounts = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM profiles WHERE email IN ('admin@histar.vn','demo@histar.vn','teacher@histar.vn') AND email_verified = TRUE;") -join '').Trim()
if ([int]$platformAccounts -lt 3) {
    throw "Bootstrap baselined at V$lastVersion but V79 verified platform accounts are incomplete ($platformAccounts/3)."
}

# V80/V81: Explore archive points and reward integrity migrations are included.
$exploreArchiveReady = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM discovery_points WHERE location_id = '11111111-1111-1111-1111-111111111111' AND deleted_at IS NULL AND unlock_key <> 'feature:diorama';") -join '').Trim()
if ([int]$exploreArchiveReady -lt 22) {
    throw "Bootstrap baselined at V$lastVersion but V80 Explore archive points are incomplete ($exploreArchiveReady/22)."
}
$rewardIntegrityReady = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('xp_ledger','explore_attempts');") -join '').Trim()
if ([int]$rewardIntegrityReady -ne 2) {
    throw "Bootstrap baselined at V$lastVersion but V81 reward integrity tables are incomplete ($rewardIntegrityReady/2)."
}

# V82: demo accounts for the three business segments are present and verified.
$demoSegmentAccounts = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM profiles WHERE email IN ('premium@histar.vn','org-standard@histar.vn','org-micro@histar.vn') AND email_verified = TRUE;") -join '').Trim()
if ([int]$demoSegmentAccounts -lt 3) {
    throw "Bootstrap baselined at V$lastVersion but V82 demo segment accounts are incomplete ($demoSegmentAccounts/3)."
}

# V83: quest track and step metadata required by Quest v2 exists.
$questTrackColumns = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM information_schema.columns WHERE table_schema = 'public' AND ((table_name = 'quests' AND column_name IN ('track','sort_order','difficulty','estimated_minutes')) OR (table_name = 'quest_steps' AND column_name IN ('is_required','is_premium')));") -join '').Trim()
if ([int]$questTrackColumns -ne 6) {
    throw "Bootstrap baselined at V$lastVersion but V83 Quest v2 columns are incomplete ($questTrackColumns/6)."
}

# V84: Củ Chi has the seeded onsite quest-track content.
$cuChiQuestTrackContent = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM quest_steps WHERE quest_id = '33333333-3333-3333-3333-333333333340' AND unlock_key LIKE 'station:%';") -join '').Trim()
if ([int]$cuChiQuestTrackContent -lt 6) {
    throw "Bootstrap baselined at V$lastVersion but V84 Củ Chi onsite quest steps are incomplete ($cuChiQuestTrackContent/6)."
}

# V85: Quest v2 media and mini-game configurations are applied.
$questMediaMinigameReady = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM quest_minigames WHERE id = 'a1000001-0000-4000-8000-000000000004' AND config::text LIKE '%/images/minigames/dem-lo-1.jpg%';") -join '').Trim()
if ([int]$questMediaMinigameReady -ne 1) {
    throw "Bootstrap baselined at V$lastVersion but V85 Quest media/mini-game content is missing."
}

# V86: the Củ Chi quest cover uses the local FE asset.
$cuChiQuestCoverReady = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM quests WHERE id = '33333333-3333-3333-3333-333333333333' AND cover_image = '/images/cu-chi/loi-xuong-ham.jpg';") -join '').Trim()
if ([int]$cuChiQuestCoverReady -ne 1) {
    throw "Bootstrap baselined at V$lastVersion but V86 Củ Chi quest cover is missing."
}

# V87: passport attempts require the enforced owner/hash/state/idempotency constraints and quota index.
$passportMailReady = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT (SELECT COUNT(*) FROM pg_constraint WHERE conrelid = to_regclass('public.passport_mail_attempts') AND conname IN ('fk_passport_mail_profile','ck_passport_mail_image_hash','ck_passport_mail_state','uq_passport_mail_user_request')) + CASE WHEN to_regclass('public.idx_passport_mail_user_created') IS NOT NULL THEN 1 ELSE 0 END;") -join '').Trim()
if ($LASTEXITCODE -ne 0 -or $passportMailReady -ne '5') {
    throw "Bootstrap baselined at V$lastVersion but V87 passport mail attempts constraints/index are incomplete ($passportMailReady/5)."
}

# V88: Vietnamese location, quest, and discovery content is restored as UTF-8.
$cuChiNameUtf8Hex = 'c490e1bb8b6120c491e1baa16f2043e1bba720436869'
$vietnameseContentReady = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc `
    "SELECT COUNT(*)::text FROM locations WHERE id = '11111111-1111-1111-1111-111111111111' AND encode(convert_to(name,'UTF8'),'hex') = '$cuChiNameUtf8Hex' AND description NOT LIKE '%?%';") -join '').Trim()
if ($LASTEXITCODE -ne 0 -or $vietnameseContentReady -ne '1') {
    throw "Bootstrap baselined at V$lastVersion but V88 Vietnamese content repair is missing."
}

# V89/V90: authenticated per-artifact AR email reservations.
$arMailReady = (@(& docker exec $Container psql -U $DbUser -d $Database -tAc "SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='ar_card_mail_attempts' AND column_name='artifact_id';") -join '').Trim()
if ($LASTEXITCODE -ne 0 -or $arMailReady -ne '1') {
    throw "Bootstrap baselined at V$lastVersion but selected-artifact AR mail schema is missing."
}
Write-Host 'V89/V90 AR mail attempts and selected artifact: ready.'

Write-Host "Bootstrap completed successfully: $Database is seeded and baselined at Flyway V$lastVersion ($lastMigrationName)."
Write-Host "V70 organizations.ops_note ready; orgs with seeded ops note: $opsNoteSeeded."
Write-Host "V71 billing CRM seed inquiries: $crmSeeded."
Write-Host "V72/analytics demo profiles: $analyticsUsers."
Write-Host "V73 audit events with clientIp/ip: $auditWithIp."
Write-Host "V77 Củ Chi AR readiness: ready."
Write-Host "V78 Củ Chi station coordinates: $stationCoordinates/6."
Write-Host "V79 verified platform accounts: $platformAccounts/3."
Write-Host "V80 Explore archive points: $exploreArchiveReady/22."
Write-Host 'V81 reward integrity tables: xp_ledger and explore_attempts.'
Write-Host "V82 verified demo segment accounts: $demoSegmentAccounts/3."
Write-Host "V83 Quest v2 metadata columns: $questTrackColumns/6."
Write-Host "V84 Củ Chi onsite quest steps: $cuChiQuestTrackContent/6."
Write-Host 'V85 Quest media and mini-game content: ready.'
Write-Host 'V86 Củ Chi quest cover: ready.'
Write-Host 'V87 passport mail attempts table and constraints: ready.'
Write-Host 'V88 Vietnamese location, quest, and discovery content: ready.'
Write-Host 'Hard-refresh Admin Organizations / Billing CRM / Analytics / Audit after bootstrap.'
if (-not $SkipDashboardRevenueSeed) {
    Write-Host 'Admin dashboard revenue demo seed applied (B2C/B2B 6-month chart + sample audit).'
}
if (-not $SkipBillingCrmSeed) {
    Write-Host 'Admin Billing CRM Kanban seed re-applied (UTF-8-safe).'
}
if (-not $SkipAnalyticsSeed) {
    Write-Host 'Admin Analytics Cu Chi demo seed applied (UTF-8-safe).'
}
