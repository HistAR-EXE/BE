param(
  [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Assert-True([bool]$condition, [string]$message) {
  if (-not $condition) { throw $message }
}

$email = "demo.week4.$([Guid]::NewGuid().ToString('N').Substring(0,8))@histar.vn"
$password = "Demo@2026"

$registerBody = @{ email = $email; password = $password; displayName = "Demo Week4" } | ConvertTo-Json
$register = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body $registerBody
$token = $register.data.token
$userId = $register.data.userId
Assert-True ($null -ne $token -and $token.Length -gt 10) "Register failed: missing token"

# Củ Chi — golden path demo location (có photo pairs, panoramas, quests)
$cuChiId = "11111111-1111-1111-1111-111111111111"
$locations = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/locations?size=50"
$locationId = ($locations.data.items | Where-Object { $_.id -eq $cuChiId } | Select-Object -First 1).id
if (-not $locationId) {
  foreach ($loc in $locations.data.items) {
    $pairs = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/photo-pairs/by-location/$($loc.id)"
    if ($pairs.data.Count -ge 1) { $locationId = $loc.id; break }
  }
}
Assert-True ($null -ne $locationId) "No location with photo pairs found"

$photoPairs = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/photo-pairs/by-location/$locationId"
Assert-True ($photoPairs.data.Count -ge 1) "No photo pairs"

$photoScenes = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/photo-scenes/by-location/$locationId"
Assert-True ($photoScenes.data.Count -ge 1) "No photo scenes"

$artifacts = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/artifacts?locationId=$locationId"
Assert-True ($artifacts.data.Count -ge 1) "No artifacts catalog"

$discoveryPoints = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/discovery-points/by-location/$locationId"
Assert-True ($discoveryPoints.data.Count -ge 1) "No discovery points"

$characters = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/characters/by-location/$locationId"
Assert-True ($characters.data.Count -ge 1) "No characters"

$panoramas = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/panoramas/by-location/$locationId"
$panoramaId = $panoramas.data[0].id
Assert-True ($null -ne $panoramaId) "No panoramas"

$hotspots = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/hotspots/by-panorama/$panoramaId"
Assert-True ($hotspots.data.Count -ge 1) "No hotspots"

$quests = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/quests?locationId=$locationId"
$questId = $quests.data.items[0].id
Assert-True ($null -ne $questId) "No quests"

$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/quests/$questId/start" -Headers $headers | Out-Null

$checkinBody = @{
  locationId = $locationId
  latitude = 11.143
  longitude = 106.461
  qrCode = "timelens:location:$locationId"
} | ConvertTo-Json
$checkin = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/checkins" -Headers $headers -ContentType "application/json" -Body $checkinBody
Assert-True ($checkin.data.success -eq $true) "Check-in failed"

$myArtifacts = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/me/artifacts?locationId=$locationId" -Headers $headers
Assert-True ($myArtifacts.data.total -ge 1) "No my artifacts"

$discoverySummary = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/me/discoveries/summary?locationId=$locationId" -Headers $headers
Assert-True ($discoverySummary.data.total -ge 1) "No discovery summary"

$frames = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/photo-frames"
$frameId = $frames.data[0].id
Assert-True ($null -ne $frameId) "No frame found"

$tempFile = Join-Path $env:TEMP "timelens-smoke.jpg"
[System.IO.File]::WriteAllBytes($tempFile, [System.Text.Encoding]::UTF8.GetBytes("demo"))

$upload = curl.exe -s -X POST "$BaseUrl/api/user-creations" -H "Authorization: Bearer $token" -F "frameId=$frameId" -F "variant=story" -F "file=@$tempFile;type=image/jpeg"
$uploadJson = $upload | ConvertFrom-Json
$creationId = $uploadJson.data.id
Assert-True ($null -ne $creationId) "Upload failed"

Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/user-creations/$creationId/record-share" -Headers $headers | Out-Null

$leaderboard = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/leaderboard?scope=all"
Assert-True ($leaderboard.data.entries.Count -ge 1) "Leaderboard empty"

Write-Host "Smoke golden path OK. userId=$userId locationId=$locationId questId=$questId"
