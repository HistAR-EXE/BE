# CP3 Full flow A-Z test - map TimeLens_CP3_Plan_Final.docx + BE scripts
# Online + Offline + data quality + optional chat cite (needs GEMINI_API_KEY)
param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$ReportFile = "docs/scripts/cp3-full-flow-report.txt",
  [switch]$SkipChatCite,
  [switch]$RunChatCiteFull
)

$ErrorActionPreference = "Stop"
$CuChi = "11111111-1111-1111-1111-111111111111"
$MainPanorama = "22222222-2222-2222-2222-222222222222"

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$reportPath = Join-Path $root $ReportFile

$results = [System.Collections.Generic.List[object]]::new()
$failCount = 0
$passCount = 0
$warnCount = 0
$skipCount = 0

function Add-Result([string]$Week, [string]$Area, [string]$Check, [string]$Status, [string]$Detail) {
  $script:results.Add([pscustomobject]@{ Week = $Week; Area = $Area; Check = $Check; Status = $Status; Detail = $Detail }) | Out-Null
  switch ($Status) {
    "PASS" { $script:passCount++ }
    "FAIL" { $script:failCount++ }
    "WARN" { $script:warnCount++ }
    "SKIP" { $script:skipCount++ }
  }
}

function Test-Api([string]$Name, [scriptblock]$Block) {
  try {
    & $Block
    return $true
  } catch {
    throw "${Name}: $($_.Exception.Message)"
  }
}

$lines = @()
$lines += "================================================================"
$lines += " CP3 FULL FLOW TEST - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += " BaseUrl: $BaseUrl"
$lines += "================================================================"
$lines += ""

# --- 0. Infrastructure ---
$lines += "[0] INFRASTRUCTURE"
try {
  $pg = docker ps --filter "name=timelens-postgres" --format "{{.Status}}" 2>$null | Select-Object -First 1
  if ($pg -match "Up") {
    Add-Result "All" "Infra" "Docker Postgres" "PASS" $pg
  } else {
    Add-Result "All" "Infra" "Docker Postgres" "FAIL" "Container not running"
  }
} catch {
  Add-Result "All" "Infra" "Docker Postgres" "WARN" "docker CLI unavailable"
}

try {
  $minio = docker ps --filter "name=timelens-minio" --format "{{.Status}}" 2>$null | Select-Object -First 1
  if ($minio -match "Up") {
    Add-Result "All" "Infra" "Docker MinIO" "PASS" $minio
  } else {
    Add-Result "All" "Infra" "Docker MinIO" "WARN" "Not running (upload may fail)"
  }
} catch {
  Add-Result "All" "Infra" "Docker MinIO" "SKIP" "docker unavailable"
}

try {
  $health = Invoke-RestMethod -Uri "$BaseUrl/api/health/ready" -TimeoutSec 10
  if ($health.database -eq "UP" -and $health.status -eq "UP") {
    Add-Result "All" "Infra" "BE /api/health/ready" "PASS" "database=UP"
  } else {
    Add-Result "All" "Infra" "BE /api/health/ready" "FAIL" ($health | ConvertTo-Json -Compress)
  }
} catch {
  Add-Result "All" "Infra" "BE /api/health/ready" "FAIL" $_.Exception.Message
  $lines += "FATAL: BE not reachable. Start: docker compose up -d; then .\mvnw spring-boot:run"
  $lines += ""
  $results | ForEach-Object { $lines += "[$($_.Status)] W$($_.Week) $($_.Area) - $($_.Check): $($_.Detail)" }
  $lines | Set-Content -Path $reportPath -Encoding UTF8
  Write-Host ($lines -join "`n")
  exit 1
}

# --- Auth setup ---
$email = "cp3.full.$([Guid]::NewGuid().ToString('N').Substring(0,8))@histar.vn"
$reg = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" `
  -Body (@{ email = $email; password = "Demo@2026"; displayName = "CP3 Full Test" } | ConvertTo-Json)
$token = $reg.data.accessToken
if (-not $token) { $token = $reg.data.token }
$headers = @{ Authorization = "Bearer $token" }
Add-Result "All" "Auth" "Register + JWT" "PASS" "email=$email"

# --- Week 1 Day 1: Data + golden path APIs ---
$lines += ""
$lines += "[W1-D1] DATA + GOLDEN PATH"

try {
  $locs = Invoke-RestMethod -Uri "$BaseUrl/api/locations?size=50"
  $cuChiLoc = $locs.data.items | Where-Object { $_.id -eq $CuChi } | Select-Object -First 1
  if ($cuChiLoc) {
    Add-Result "1" "Data" "Cu Chi in locations list" "PASS" $cuChiLoc.name
  } else {
    Add-Result "1" "Data" "Cu Chi in locations list" "FAIL" "UUID not found - run run-all-seed.ps1"
  }
  if ($locs.data.totalItems -ge 10) {
    Add-Result "1" "Data" "Locations count >= 10" "PASS" "total=$($locs.data.totalItems)"
  } else {
    Add-Result "1" "Data" "Locations count >= 10" "WARN" "total=$($locs.data.totalItems)"
  }
} catch {
  Add-Result "1" "Data" "GET /api/locations" "FAIL" $_.Exception.Message
}

try {
  $detail = Invoke-RestMethod -Uri "$BaseUrl/api/locations/$CuChi"
  Add-Result "1" "Data" "GET /api/locations/{id}" "PASS" $detail.data.name
} catch {
  Add-Result "1" "Data" "GET /api/locations/{id}" "FAIL" $_.Exception.Message
}

# DB sources (via docker)
try {
  $srcOut = docker exec timelens-postgres psql -U timelens -d timelens -t -c `
    "SELECT sources FROM locations WHERE id='$CuChi' LIMIT 1;" 2>$null
  $src = ($srcOut -join "").Trim()
  if ($src -match "UNESCO|Ban Quan") {
    Add-Result "1" "Data" "locations.sources Cu Chi (DB)" "PASS" ($src.Substring(0, [Math]::Min(80, $src.Length)) + "...")
  } elseif ($src.Length -gt 10) {
    Add-Result "1" "Data" "locations.sources Cu Chi (DB)" "WARN" "Has text but check content"
  } else {
    Add-Result "1" "Data" "locations.sources Cu Chi (DB)" "FAIL" "Empty - run week-1 SQL"
  }
} catch {
  Add-Result "1" "Data" "locations.sources Cu Chi (DB)" "SKIP" "docker psql unavailable"
}

# --- Week 1 Day 3: ONLINE flow ---
$lines += ""
$lines += "[W1-D3] ONLINE FLOW (Explore, 360, Chat, Slider)"

try {
  $pairs = Invoke-RestMethod -Uri "$BaseUrl/api/photo-pairs/by-location/$CuChi"
  if ($pairs.data.Count -ge 1) {
    Add-Result "1" "Online" "Photo slider (photo-pairs)" "PASS" "count=$($pairs.data.Count)"
  } else {
    Add-Result "1" "Online" "Photo slider (photo-pairs)" "FAIL" "No pairs for Cu Chi"
  }
} catch {
  Add-Result "1" "Online" "Photo slider" "FAIL" $_.Exception.Message
}

try {
  $panos = Invoke-RestMethod -Uri "$BaseUrl/api/panoramas/by-location/$CuChi"
  if ($panos.data.Count -ge 3) {
    Add-Result "1" "Online" "Virtual Tour panoramas >= 3" "PASS" "count=$($panos.data.Count)"
  } else {
    Add-Result "1" "Online" "Virtual Tour panoramas >= 3" "WARN" "count=$($panos.data.Count) - run week-3 SQL"
  }
} catch {
  Add-Result "1" "Online" "Panoramas" "FAIL" $_.Exception.Message
}

try {
  $hots = Invoke-RestMethod -Uri "$BaseUrl/api/hotspots/by-panorama/$MainPanorama"
  $scene = $hots.data | Where-Object { $_.type -eq "scene" }
  $uuidOk = ($scene | Where-Object { $_.contentRef -match '^[0-9a-f-]{36}$' }).Count
  if ($scene.Count -ge 2 -and $uuidOk -eq $scene.Count) {
    Add-Result "1" "Online" "Scene hotspots UUID" "PASS" "scene=$($scene.Count)"
  } else {
    Add-Result "1" "Online" "Scene hotspots UUID" "FAIL" "scene=$($scene.Count) uuidOk=$uuidOk"
  }
} catch {
  Add-Result "1" "Online" "Hotspots scene-link" "FAIL" $_.Exception.Message
}

try {
  $chars = Invoke-RestMethod -Uri "$BaseUrl/api/characters/by-location/$CuChi"
  if ($chars.data.Count -eq 2) {
    Add-Result "1" "Online" "Characters Cu Chi = 2" "PASS" ($chars.data.name -join ", ")
  } elseif ($chars.data.Count -ge 1) {
    Add-Result "1" "Online" "Characters Cu Chi = 2" "WARN" "count=$($chars.data.Count)"
  } else {
    Add-Result "1" "Online" "Characters Cu Chi" "FAIL" "No characters"
  }
} catch {
  Add-Result "1" "Online" "Characters" "FAIL" $_.Exception.Message
}

# Chat sample (1 in-scope + 1 out-of-scope)
if (-not $SkipChatCite) {
  try {
    $charId = (Invoke-RestMethod -Uri "$BaseUrl/api/characters/by-location/$CuChi").data[0].id
    $chat1 = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/chat" -Headers $headers -ContentType "application/json" `
      -Body (@{ characterId = $charId; message = "Bep Hoang Cam la gi?"; conversationId = $null } | ConvertTo-Json)
    $r1 = $chat1.data.reply
    if ($r1 -match 'Ngu.n\s*:') {
      Add-Result "1" "Online" "AI Chat cite (sample)" "PASS" "Has source line"
    } else {
      Add-Result "1" "Online" "AI Chat cite (sample)" "WARN" "No source line - check GEMINI_API_KEY"
    }
    $chat2 = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/chat" -Headers $headers -ContentType "application/json" `
      -Body (@{ characterId = $charId; message = "Gia ve may bay di Cu Chi?"; conversationId = $null } | ConvertTo-Json)
    if ($chat2.data.reply -match 'ch.a c. th.ng tin ch.nh x.c') {
      Add-Result "1" "Online" "AI guardrail (out-of-scope)" "PASS" "Rejected off-topic"
    } else {
      Add-Result "1" "Online" "AI guardrail (out-of-scope)" "WARN" "May not reject off-topic"
    }
  } catch {
    $err = $_.Exception.Message
    if ($err -match '422' -or $err -match 'GEMINI') {
      Add-Result "1" "Online" "AI Chat" "WARN" "GEMINI_API_KEY missing in .env - set key then re-run"
    } else {
      Add-Result "1" "Online" "AI Chat" "FAIL" $err
    }
  }
} else {
  Add-Result "1" "Online" "AI Chat" "SKIP" "-SkipChatCite"
}

# --- Week 1 Day 4: OFFLINE flow ---
$lines += ""
$lines += "[W1-D4] OFFLINE FLOW (Check-in, Quest, Secret, Frame, Share)"

try {
  if (-not $cuChiLoc) {
    $cuChiLoc = (Invoke-RestMethod -Uri "$BaseUrl/api/locations/$CuChi").data
  }
  $lat = $cuChiLoc.latitude
  $lng = $cuChiLoc.longitude
  $checkinBody = @{
    locationId = $CuChi
    latitude = $lat
    longitude = $lng
    qrCode = "timelens:location:$CuChi"
  } | ConvertTo-Json
  $ci = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/checkins" -Headers $headers -ContentType "application/json" -Body $checkinBody
  if ($ci.data.success) {
    Add-Result "1" "Offline" "Check-in QR/GPS" "PASS" "points=$($ci.data.pointsEarned)"
  } else {
    Add-Result "1" "Offline" "Check-in QR/GPS" "FAIL" "success=false"
  }
} catch {
  Add-Result "1" "Offline" "Check-in QR/GPS" "FAIL" $_.Exception.Message
}

try {
  $quests = Invoke-RestMethod -Uri "$BaseUrl/api/quests?locationId=$CuChi"
  $qid = $quests.data.items[0].id
  Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/quests/$qid/start" -Headers $headers | Out-Null
  $prog = Invoke-RestMethod -Uri "$BaseUrl/api/quests/$qid/progress" -Headers $headers
  $meQ = Invoke-RestMethod -Uri "$BaseUrl/api/me/quests?page=0&size=10" -Headers $headers
  Add-Result "1" "Offline" "Quest start + progress" "PASS" "quest=$qid meQuests=$($meQ.data.items.Count)"
} catch {
  Add-Result "1" "Offline" "Quest flow" "FAIL" $_.Exception.Message
}

try {
  $secret = Invoke-RestMethod -Uri "$BaseUrl/api/locations/$CuChi/secret-story" -Headers $headers
  if ($secret.data.story -or $secret.data.title) {
    Add-Result "1" "Offline" "Secret story (JWT)" "PASS" "Has content"
  } else {
    Add-Result "1" "Offline" "Secret story (JWT)" "WARN" "Empty story"
  }
} catch {
  Add-Result "1" "Offline" "Secret story" "FAIL" $_.Exception.Message
}

try {
  $frames = Invoke-RestMethod -Uri "$BaseUrl/api/photo-frames"
  $frameId = $frames.data[0].id
  $tmp = Join-Path $env:TEMP "cp3-smoke.jpg"
  [System.IO.File]::WriteAllBytes($tmp, [byte[]](0xFF, 0xD8, 0xFF, 0xD9))
  $up = curl.exe -s -X POST "$BaseUrl/api/user-creations" -H "Authorization: Bearer $token" `
    -F "frameId=$frameId" -F "variant=story" -F "file=@$tmp;type=image/jpeg"
  $upJ = $up | ConvertFrom-Json
  $cid = $upJ.data.id
  Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/user-creations/$cid/record-share" -Headers $headers | Out-Null
  $prefill = Invoke-RestMethod -Uri "$BaseUrl/api/share/prefill" -Headers $headers
  Add-Result "1" "Offline" "Photo frame + share" "PASS" "creation=$cid"
} catch {
  Add-Result "1" "Offline" "Photo frame + share" "FAIL" $_.Exception.Message
}

# --- Week 2: 5 screens ---
$lines += ""
$lines += "[W2] POLISH SCREENS (API layer)"

try {
  $profile = Invoke-RestMethod -Uri "$BaseUrl/api/profile/me" -Headers $headers
  Add-Result "2" "Screens" "Profile /me" "PASS" "level=$($profile.data.levelName)"
} catch {
  Add-Result "2" "Screens" "Profile /me" "FAIL" $_.Exception.Message
}

try {
  $badges = Invoke-RestMethod -Uri "$BaseUrl/api/badges"
  $myBadges = Invoke-RestMethod -Uri "$BaseUrl/api/me/badges" -Headers $headers
  Add-Result "2" "Screens" "Badges list" "PASS" "all=$($badges.data.Count) mine=$($myBadges.data.Count)"
} catch {
  Add-Result "2" "Screens" "Badges" "FAIL" $_.Exception.Message
}

try {
  $lbAll = Invoke-RestMethod -Uri "$BaseUrl/api/leaderboard?scope=all"
  $lbWeek = Invoke-RestMethod -Uri "$BaseUrl/api/leaderboard?scope=week"
  if ($lbAll.data.entries.Count -ge 1) {
    Add-Result "2" "Screens" "Leaderboard" "PASS" "all=$($lbAll.data.entries.Count) week=$($lbWeek.data.entries.Count)"
  } else {
    Add-Result "2" "Screens" "Leaderboard" "WARN" "Empty entries"
  }
} catch {
  Add-Result "2" "Screens" "Leaderboard" "FAIL" $_.Exception.Message
}

# --- Week 3: Panorama API (optional upload test skipped - needs real image) ---
$lines += ""
$lines += "[W3] DEPLOY READINESS"

try {
  $panoDetail = Invoke-RestMethod -Uri "$BaseUrl/api/panoramas/$MainPanorama"
  if ($panoDetail.data.imageUrl) {
    Add-Result "3" "360" "Main panorama imageUrl" "PASS" $panoDetail.data.imageUrl.Substring(0, [Math]::Min(60, $panoDetail.data.imageUrl.Length))
  } else {
    Add-Result "3" "360" "Main panorama imageUrl" "WARN" "Missing URL"
  }
} catch {
  Add-Result "3" "360" "Panorama detail" "FAIL" $_.Exception.Message
}

# Auth refresh (in-memory - document behavior)
try {
  $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" `
    -Body (@{ email = $email; password = "Demo@2026" } | ConvertTo-Json)
  $rt = $login.data.refreshToken
  if ($rt) {
    $ref = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/refresh" -ContentType "application/json" `
      -Body (@{ refreshToken = $rt } | ConvertTo-Json)
  if ($ref.data.accessToken -or $ref.data.token) {
      Add-Result "All" "Auth" "Refresh token flow" "PASS" "in-memory OK while BE up"
    } else {
      Add-Result "All" "Auth" "Refresh token flow" "FAIL" "No new token"
    }
  } else {
    Add-Result "All" "Auth" "Refresh token flow" "WARN" "No refreshToken in login response"
  }
} catch {
  Add-Result "All" "Auth" "Refresh token flow" "FAIL" $_.Exception.Message
}

# --- Delegate existing smokes ---
$lines += ""
$lines += "[SCRIPTS] Existing smoke tests"

$smokeScripts = @(
  @{ Name = "smoke-golden-path.ps1"; Week = "1" },
  @{ Name = "smoke-week2-screens.ps1"; Week = "2" },
  @{ Name = "verify-hotspots-scene.ps1"; Week = "3" }
)

foreach ($s in $smokeScripts) {
  $scriptPath = Join-Path $PSScriptRoot $s.Name
  try {
    & powershell -ExecutionPolicy Bypass -File $scriptPath -BaseUrl $BaseUrl 2>&1 | Out-String | Out-Null
    if ($LASTEXITCODE -eq 0) {
      Add-Result $s.Week "Script" $s.Name "PASS" "exit 0"
    } else {
      Add-Result $s.Week "Script" $s.Name "FAIL" "exit $LASTEXITCODE"
    }
  } catch {
    Add-Result $s.Week "Script" $s.Name "FAIL" $_.Exception.Message
  }
}

# --- Full chat cite (optional, slow) ---
if ($RunChatCiteFull -and -not $SkipChatCite) {
  $lines += ""
  $lines += "[W1-D5] CHAT CITE 20 QUESTIONS (full)"
  try {
    & powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "chat-cite-verify.ps1") -BaseUrl $BaseUrl
    if ($LASTEXITCODE -eq 0) {
      Add-Result "1" "Online" "chat-cite-verify.ps1 (20 Q)" "PASS" "see chat-cite-verify-results.txt"
    } else {
      Add-Result "1" "Online" "chat-cite-verify.ps1 (20 Q)" "FAIL" "exit $LASTEXITCODE"
    }
  } catch {
    Add-Result "1" "Online" "chat-cite-verify.ps1" "FAIL" $_.Exception.Message
  }
}

# --- FE-only items (cannot test from BE) ---
$lines += ""
$lines += "[FE-ONLY] Cannot verify from BE repo"
Add-Result "1" "FE" "AppMode Online/Offline UI" "SKIP" "FE implementation required"
Add-Result "1" "FE" "ModeSelectPage + badges" "SKIP" "FE implementation required"
Add-Result "2" "FE" "UI polish / empty states" "SKIP" "FE implementation required"
Add-Result "2" "Team" "Focus Group 10-16 users" "SKIP" "Manual session"
Add-Result "3" "Field" "360 photos from Cu Chi trip" "SKIP" "Manual capture"
Add-Result "3" "Deploy" "Railway production" "SKIP" "Run smoke-production.ps1 when deployed"

# --- Summary table ---
$lines += ""
$lines += "================================================================"
$lines += " RESULTS"
$lines += "================================================================"
foreach ($r in $results) {
  $line = "[$($r.Status.PadRight(4))] Tuần $($r.Week) | $($r.Area.PadRight(8)) | $($r.Check)"
  if ($r.Detail) { $line += " - $($r.Detail)" }
  $lines += $line
}

$lines += ""
$lines += "SUMMARY: PASS=$passCount FAIL=$failCount WARN=$warnCount SKIP=$skipCount"
$lines += ""

if ($failCount -eq 0) {
  $lines += "VERDICT: BE ready for CP3 demo (local). Complete FE mode UI + optional -RunChatCiteFull for pitch evidence."
} else {
  $lines += "VERDICT: FIX $($failCount) FAIL item(s) before pitch."
}

$lines += ""
$lines += "Next steps:"
$lines += "  1. FE: test Online/Offline 2 mode on mobile LAN"
$lines += "  2. powershell -ExecutionPolicy Bypass -File docs\scripts\cp3-full-flow-test.ps1 -RunChatCiteFull"
$lines += "  3. After Railway deploy: smoke-production.ps1 -BaseUrl https://<host>"

$lines | Set-Content -Path $reportPath -Encoding UTF8
Write-Host ($lines -join "`n")
Write-Host ""
Write-Host "Report saved: $reportPath"

if ($failCount -gt 0) { exit 1 }
exit 0
