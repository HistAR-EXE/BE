# Verify virtual tour scene-links for main Cu Chi panorama
param([string]$BaseUrl = "http://localhost:8080")

$panoramaId = "22222222-2222-2222-2222-222222222222"
$cuChiId = "11111111-1111-1111-1111-111111111111"
$expectedTargets = @(
  "22222222-2222-2222-2222-222222222221",
  "22222222-2222-2222-2222-222222222223"
)

$hotspots = Invoke-RestMethod -Uri "$BaseUrl/api/hotspots/by-panorama/$panoramaId"
$scene = $hotspots.data | Where-Object { $_.type -eq "scene" }

Write-Host "Panorama: $panoramaId"
Write-Host "Scene hotspots: $($scene.Count)"
foreach ($h in $scene) {
  $ok = $expectedTargets -contains $h.contentRef
  $status = if ($ok) { "OK" } else { "CHECK" }
  Write-Host "  [$($h.label)] contentRef=$($h.contentRef) $status"
}

$panoramas = Invoke-RestMethod -Uri "$BaseUrl/api/panoramas/by-location/$cuChiId"
Write-Host "Panoramas at Cu Chi: $($panoramas.data.Count) (expect >= 3 after week-3 SQL)"

$uuidPattern = '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
$bad = $scene | Where-Object { $_.contentRef -notmatch $uuidPattern }
if ($bad) {
  throw "FAIL: scene hotspot contentRef is not UUID: $($bad.contentRef -join ', ')"
}
if ($panoramas.data.Count -lt 3) {
  Write-Warning "WARN: fewer than 3 panoramas - run run-all-seed.ps1"
}
Write-Host "verify-hotspots-scene OK"
