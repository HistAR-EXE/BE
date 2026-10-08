[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$migrationDirectory = Join-Path $backendRoot 'src/main/resources/db/migration'
$requiredMigrations = 38..45 | ForEach-Object { "V$($_)__" }

foreach ($prefix in $requiredMigrations) {
    $matches = @(Get-ChildItem $migrationDirectory -Filter "$prefix*.sql")
    if ($matches.Count -ne 1) {
        throw "Expected exactly one migration matching $prefix; found $($matches.Count)."
    }
}

$seedManifest = @{
    '2026-06-19_heritage_sites_from_dataset.sql' = 'DC181EFE80D77C8A6B806526545E28BB75DFE59DA6E7F669E2B8857E3FFCD4BB'
    '2026-06-21_heritage_quests_seed.sql' = '24069098C1698E87A8CFB5DAA82E842816167C61BC6EA9795A613BFEC946FF16'
    '2026-06-23_quest_mission_keys.sql' = '69C5818C133676782D5D0603BD73FAE86B3259DB2CA0B9D96E211D9C13A3096B'
    '2026-06-24_quest_visual_artifact_keys.sql' = 'E3FDDEA84B2E4BEE24EF73CAEAFD2F49A085C5B2E60DCD56738F6052F5C01ED2'
    '2026-06-25_quest_mixed_visual_pipeline.sql' = 'CADE4E300A1D10DACEDD8DB3D0C6E41AB28DB5B3F4B8CAD68B18AF8BCD07BE8D'
    '2026-06-26_heritage_quest_discovery_points.sql' = '858E95246B5D2AC50DE39F1F5DC18435BF678FEE8308A891EFCC141D8A1D94A0'
    '2026-06-27_heritage_p2_checkin_bonus.sql' = '494CFC68F9BEF094046A88A5BC290756F3414701256F61E4B6090226C3547724'
    '2026-07-04_quest_onsite_flag.sql' = '5B1B38456CD3BCB3BE7136328F9CD738C8363644704D9EDA717D631F2B7E6405'
    '2026-07-08_quest_steps_schema.sql' = 'F896885E56E7A111F0E29F9147A29B27C14B8871E2C51033788495D4CBAB53A4'
    '2026-07-08_quest_steps_init.sql' = '6484C584505634A21A7ABDCBC70174071F1018CBC37B2D1D8E8FFF5D234EF99D'
    '2026-10-07_unattached_quest_engines.sql' = '7CEC3B3B792CDFEEC50F1C4019F917FBD976741B2062C8878B5ECA4826367389'
}

$seedDirectory = Join-Path $backendRoot 'docs/sql/seed'
foreach ($entry in $seedManifest.GetEnumerator()) {
    $path = Join-Path $seedDirectory $entry.Key
    if (-not (Test-Path $path)) {
        throw "Required controlled seed is missing: $($entry.Key)"
    }
    $actualHash = (Get-FileHash $path -Algorithm SHA256).Hash
    if ($actualHash -ne $entry.Value) {
        throw "Controlled seed hash changed: $($entry.Key). Create a new reviewed version instead of modifying it in place."
    }
}

Write-Host 'Migration ledger V38–V45 and controlled seed hashes passed.'
