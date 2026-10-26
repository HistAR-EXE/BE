$ErrorActionPreference = 'Stop'
$runnerPath = Join-Path $PSScriptRoot 'bootstrap-local-db.ps1'

if (-not (Test-Path $runnerPath)) {
    throw "Bootstrap runner was not created: $runnerPath"
}

$tokens = $null
$parseErrors = $null
[void][System.Management.Automation.Language.Parser]::ParseFile(
    $runnerPath,
    [ref]$tokens,
    [ref]$parseErrors
)

if ($parseErrors.Count -gt 0) {
    throw "Bootstrap runner has PowerShell parse errors: $($parseErrors[0].Message)"
}

$runner = Get-Content -Raw -Encoding UTF8 $runnerPath
foreach ($requiredText in @(
    '[switch]$Reset',
    'TimeLens_full_bootstrap.sql',
    '$ExpectedMinFlywayVersion = 94',
    'Refusing to overwrite an existing database without -Reset',
    'Bootstrap completed successfully',
    'docker cp',
    '$containerBootstrapPath',
    'V87 passport mail attempts table and constraints: ready.',
    'V88 Vietnamese location, quest, and discovery content: ready.',
    'V89/V90 AR mail attempts and selected artifact: ready.',
    'V91 site workspace schema: ready.',
    'V92 field trip schema: ready.',
    'V93 demo dataset lifecycle: ready.',
    'V94 legacy analytics seed removal: ready.',
    '$cuChiNameUtf8Hex = ''c490e1bb8b6120c491e1baa16f2043e1bba720436869''',
    "encode(convert_to(name,'UTF8'),'hex')",
    'fk_passport_mail_profile',
    'ck_passport_mail_image_hash',
    'ck_passport_mail_state',
    'uq_passport_mail_user_request',
    'idx_passport_mail_user_created'
)) {
    if (-not $runner.Contains($requiredText)) {
        throw "Bootstrap runner is missing: $requiredText"
    }
}

if ($runner.Contains("name = 'Địa đạo Củ Chi'")) {
    throw 'Bootstrap runner must not pass a Vietnamese SQL literal through docker exec on Windows PowerShell 5.1.'
}

foreach ($requiredMigration in @(
    'V75__organization_ops_extensions.sql',
    'V76__admin_audit_hash_chain.sql',
    'V77__location_ar_readiness_seed.sql',
    'V78__station_latlng_cuuchi_placeholder.sql',
    'V79__seed_platform_accounts.sql',
    'V80__cu_chi_explore_archive_points.sql',
    'V81__reward_integrity.sql',
    'V82__seed_demo_segment_accounts.sql',
    'V83__quest_tracks_v2_schema.sql',
    'V84__quest_tracks_v2_content.sql',
    'V85__quest_media_and_minigames_v2.sql',
    'V86__cu_chi_quest_333_cover.sql',
    'V87__passport_mail_attempts.sql',
    'V88__repair_vietnamese_content.sql',
    'V89__ar_card_mail_attempts.sql',
    'V90__ar_card_mail_artifact.sql',
    'V91__site_workspaces_and_content_versions.sql',
    'V92__field_trip_workflow.sql',
    'V93__demo_dataset_lifecycle.sql',
    'V94__remove_legacy_analytics_demo_seed.sql'
)) {
    if (-not (Test-Path (Join-Path $PSScriptRoot "..\..\src\main\resources\db\migration\$requiredMigration"))) {
        throw "Bootstrap runner smoke check is missing: $requiredMigration"
    }
}

if ($runner.Contains('Get-Content -Raw -Encoding UTF8 $bootstrapPath |')) {
    throw 'Bootstrap runner must not pipe UTF-8 SQL through a Windows native command.'
}

Write-Host "Bootstrap runner structure is valid: $runnerPath"
