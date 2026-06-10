# CP3 - Test 20 chat questions: cite source + guardrail
# Requires: BE running with GEMINI_API_KEY, run-all-seed.ps1 done
param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$QuestionsFile = "chat-cite-verify-questions.json"
)

$ErrorActionPreference = "Stop"
$CuChi = "11111111-1111-1111-1111-111111111111"
$citePattern = 'Ngu.n\s*:'
$rejectPattern = 'ch.a c. th.ng tin ch.nh x.c'
# 1-based indices: off-topic / trap questions in questions JSON
$outOfScopeIdx = @(6, 7, 8, 9, 14, 16, 17, 18)

$qPath = Join-Path $PSScriptRoot $QuestionsFile
if (-not (Test-Path $qPath)) { throw "Missing questions file: $qPath" }
$questions = Get-Content $qPath -Raw -Encoding UTF8 | ConvertFrom-Json

# Register
$email = "cite.verify.$([Guid]::NewGuid().ToString('N').Substring(0,8))@histar.vn"
$reg = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" `
  -Body (@{ email = $email; password = "Demo@2026"; displayName = "Cite Verify" } | ConvertTo-Json)
$token = $reg.data.accessToken
if (-not $token) { $token = $reg.data.token }
$headers = @{ Authorization = "Bearer $token" }

$chars = Invoke-RestMethod -Uri "$BaseUrl/api/characters/by-location/$CuChi"
if ($chars.data.Count -lt 1) { throw "No characters for Cu Chi" }
$charId = $chars.data[0].id
$charName = $chars.data[0].name

$lines = @()
$lines += "=== Chat cite verify $(Get-Date -Format o) ==="
$lines += "Character: $charName ($charId)"
$lines += "Questions: $($questions.Count)"
$lines += ""

$passCite = 0
$passReject = 0
$outOfScopeTotal = 0
$idx = 0
$failChat = 0

foreach ($q in $questions) {
  $idx++
  try {
    $body = @{ characterId = $charId; message = $q; conversationId = $null } | ConvertTo-Json -Compress
    $chat = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/chat" -Headers $headers `
      -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
    $reply = $chat.data.reply

    $hasCite = $reply -match $citePattern
    $hasReject = $reply -match $rejectPattern
    $isOutOfScope = $outOfScopeIdx -contains $idx

    if ($hasCite) { $passCite++ }
    if ($isOutOfScope) {
      $outOfScopeTotal++
      if ($hasReject) { $passReject++ }
    }

    $lines += "--- Cau $idx ---"
    $lines += "Hoi: $q"
    $lines += "Tra loi:"
    $lines += $reply
    $lines += "Cite: $(if ($hasCite) { 'YES' } else { 'NO' }) | Out-of-scope: $(if ($isOutOfScope) { 'YES' } else { 'no' }) | Rejected: $(if ($hasReject) { 'YES' } else { 'no' })"
    $lines += ""
  } catch {
    $errDetail = $_.Exception.Message
    if ($_.ErrorDetails.Message) { $errDetail = $_.ErrorDetails.Message }
    if ($errDetail -match '429|quá tải|TOO_MANY') {
      Write-Host "Rate limit - waiting 30s before retry (cau $idx)..."
      Start-Sleep -Seconds 30
      try {
        $body = @{ characterId = $charId; message = $q; conversationId = $null } | ConvertTo-Json -Compress
        $chat = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/chat" -Headers $headers `
          -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
        $reply = $chat.data.reply
        $hasCite = $reply -match $citePattern
        $hasReject = $reply -match $rejectPattern
        $isOutOfScope = $outOfScopeIdx -contains $idx
        if ($hasCite) { $passCite++ }
        if ($isOutOfScope) { $outOfScopeTotal++; if ($hasReject) { $passReject++ } }
        $lines += "--- Cau $idx (retry OK) ---"
        $lines += "Hoi: $q"
        $lines += "Tra loi:"
        $lines += $reply
        $lines += "Cite: $(if ($hasCite) { 'YES' } else { 'NO' }) | Out-of-scope: $(if ($isOutOfScope) { 'YES' } else { 'no' }) | Rejected: $(if ($hasReject) { 'YES' } else { 'no' })"
        $lines += ""
        Start-Sleep -Seconds 5
        continue
      } catch {
        $errDetail = $_.ErrorDetails.Message
        if (-not $errDetail) { $errDetail = $_.Exception.Message }
      }
    }
    $failChat++
    $lines += "--- Cau $idx ---"
    $lines += "Hoi: $q"
    $lines += "ERROR: $errDetail"
    $lines += ""
  }
  Start-Sleep -Seconds 5
}

$lines += "=== SUMMARY ==="
$lines += "Co dong Nguon: $passCite / $idx"
$lines += "Tu choi dung (ngoai pham vi): $passReject / $outOfScopeTotal (out-of-scope questions)"
$lines += "Chat errors: $failChat"
if ($failChat -gt 0) {
  $lines += "NOTE: Neu loi 422 GEMINI_API_KEY - restart BE sau khi set .env"
}

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$outPath = Join-Path $root "docs\scripts\chat-cite-verify-results.txt"
$lines | Set-Content -Path $outPath -Encoding UTF8
Write-Host ($lines -join "`n")
Write-Host ""
Write-Host "Saved: $outPath"

if ($failChat -gt 0) { exit 1 }
if ($passCite -lt 15) { exit 2 }
exit 0
