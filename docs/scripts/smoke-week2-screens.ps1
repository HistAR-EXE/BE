# CP3 Week 2 — Smoke 5 màn: Explore, Quests, Profile, Leaderboard, Scan (check-in)
# Chạy khi BE + DB đã sẵn sàng (sau golden path week 1).
param(
  [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Assert-True([bool]$condition, [string]$message) {
  if (-not $condition) { throw $message }
}

Write-Host "=== Week 2 screen smoke: $BaseUrl ==="

# Health
$health = Invoke-RestMethod -Uri "$BaseUrl/api/health/ready"
Assert-True ($health.database -eq "UP") "Database not ready"

# Auth
$email = "week2.smoke.$([Guid]::NewGuid().ToString('N').Substring(0,8))@histar.vn"
$registerBody = @{ email = $email; password = "Demo@2026"; displayName = "Week2 Smoke" } | ConvertTo-Json
$register = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body $registerBody
$token = $register.data.accessToken
if (-not $token) { $token = $register.data.token }
$headers = @{ Authorization = "Bearer $token" }
Assert-True ($token.Length -gt 10) "Register failed"

# Explore — locations paginated
$locations = Invoke-RestMethod -Uri "$BaseUrl/api/locations?page=0&size=50"
Assert-True ($locations.data.items.Count -ge 1) "Explore: no locations"
$cuChiId = "11111111-1111-1111-1111-111111111111"
$locationId = ($locations.data.items | Where-Object { $_.id -eq $cuChiId } | Select-Object -First 1).id
if (-not $locationId) { $locationId = $locations.data.items[0].id }
Write-Host "OK Explore: $($locations.data.totalItems) locations"

# Quests — public list + my quests
$quests = Invoke-RestMethod -Uri "$BaseUrl/api/quests?locationId=$locationId&page=0&size=10"
Assert-True ($quests.data.items.Count -ge 1) "Quests: no quests"
$questId = $quests.data.items[0].id
$meQuests = Invoke-RestMethod -Uri "$BaseUrl/api/me/quests?page=0&size=10" -Headers $headers
Assert-True ($null -ne $meQuests.data.items) "Quests: me/quests failed"
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/quests/$questId/start" -Headers $headers | Out-Null
Write-Host "OK Quests: $($quests.data.totalItems) quests"

# Profile — me + badges (empty badges OK for new user)
$profile = Invoke-RestMethod -Uri "$BaseUrl/api/profile/me" -Headers $headers
Assert-True ($profile.data.email -eq $email) "Profile: me failed"
$badges = Invoke-RestMethod -Uri "$BaseUrl/api/me/badges" -Headers $headers
Assert-True ($null -ne $badges.data) "Profile: badges endpoint failed"
Write-Host "OK Profile: level=$($profile.data.levelName) points=$($profile.data.totalPoints) badges=$($badges.data.Count)"

# Leaderboard — all scopes (entries may be empty for edge DB; topup should have data)
$lbAll = Invoke-RestMethod -Uri "$BaseUrl/api/leaderboard?scope=all"
Assert-True ($null -ne $lbAll.data.entries) "Leaderboard: all scope failed"
$lbWeek = Invoke-RestMethod -Uri "$BaseUrl/api/leaderboard?scope=week"
Assert-True ($null -ne $lbWeek.data.entries) "Leaderboard: week scope failed"
Write-Host "OK Leaderboard: all=$($lbAll.data.entries.Count) week=$($lbWeek.data.entries.Count)"

# Scan - check-in GPS/QR (coords must match locationId)
$cuChi = $locations.data.items | Where-Object { $_.id -eq $locationId } | Select-Object -First 1
$checkinBody = @{
  locationId = $locationId
  latitude = $cuChi.latitude
  longitude = $cuChi.longitude
  qrCode = "timelens:location:$locationId"
} | ConvertTo-Json
$checkin = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/checkins" -Headers $headers -ContentType "application/json" -Body $checkinBody
Assert-True ($checkin.data.success -eq $true) "Scan: check-in failed"
Write-Host "OK Scan: check-in success"

Write-Host ""
Write-Host "Week 2 screen smoke PASSED. email=$email locationId=$locationId"
