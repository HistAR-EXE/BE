# CP3 - Test 5 cau chat mau: cite + guardrail, delay dai tranh 429
param(
  [string]$BaseUrl = "http://localhost:8080",
  [int]$DelaySeconds = 15,
  [int]$RetryWaitSeconds = 45
)

$ErrorActionPreference = "Stop"
$CuChi = "11111111-1111-1111-1111-111111111111"
$citePattern = 'Ngu.n\s*:'
$rejectPattern = 'ch.a c. th.ng tin ch.nh x.c'

# 3 cau trong pham vi + 2 cau ngoai pham vi (indices giong bo 20 cau)
$questions = @(
  "Bep Hoang Cam la gi va tai sao quan trong?",
  "Dia dao Cu Chi dai bao nhieu km?",
  "Ham dia dao co may tang?",
  "Gia ve may bay di Cu Chi hom nay bao nhieu?",
  "Nam nao dia dao Cu Chi duoc UNESCO cong nhan?"
)
$outOfScopeIdx = @(4, 5)

$email = "cite5.$([Guid]::NewGuid().ToString('N').Substring(0,8))@histar.vn"
$reg = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" `
  -Body (@{ email = $email; password = "Demo@2026"; displayName = "Cite5" } | ConvertTo-Json)
$token = $reg.data.accessToken
if (-not $token) { $token = $reg.data.token }
$headers = @{ Authorization = "Bearer $token" }

$chars = Invoke-RestMethod -Uri "$BaseUrl/api/characters/by-location/$CuChi"
$charId = $chars.data[0].id
$charName = $chars.data[0].name

$lines = @()
$lines += "=== Chat cite verify (5 cau) $(Get-Date -Format o) ==="
$lines += "Character: $charName ($charId)"
$lines += "Delay: ${DelaySeconds}s | Retry wait: ${RetryWaitSeconds}s"
$lines += ""

$passCite = 0
$passReject = 0
$outOfScopeTotal = 0
$failChat = 0
$idx = 0

function Invoke-ChatQuestion {
  param([string]$Question, [hashtable]$Headers, [string]$CharId)
  $body = @{ characterId = $CharId; message = $Question; conversationId = $null } | ConvertTo-Json -Compress
  return Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/chat" -Headers $Headers `
    -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
}

foreach ($q in $questions) {
  $idx++
  Write-Host "[$idx/5] Dang hoi (cho ${DelaySeconds}s truoc cau tiep theo sau khi xong)..."
  $ok = $false
  $attempt = 0
  while (-not $ok -and $attempt -lt 2) {
    $attempt++
    try {
      $chat = Invoke-ChatQuestion -Question $q -Headers $headers -CharId $charId
      $reply = $chat.data.reply
      $hasCite = $reply -match $citePattern
      $hasReject = $reply -match $rejectPattern
      $isOutOfScope = $outOfScopeIdx -contains $idx

      if ($hasCite) { $passCite++ }
      if ($isOutOfScope) {
        $outOfScopeTotal++
        if ($hasReject) { $passReject++ }
      }

      $suffix = if ($attempt -gt 1) { " (retry OK)" } else { "" }
      $lines += "--- Cau $idx$suffix ---"
      $lines += "Hoi: $q"
      $lines += "Tra loi:"
      $lines += $reply
      $lines += "Cite: $(if ($hasCite) { 'YES' } else { 'NO' }) | Out-of-scope: $(if ($isOutOfScope) { 'YES' } else { 'no' }) | Rejected: $(if ($hasReject) { 'YES' } else { 'no' })"
      $lines += ""
      Write-Host "  -> OK (cite=$(if ($hasCite) {'YES'} else {'NO'}))"
      $ok = $true
    } catch {
      $errDetail = $_.Exception.Message
      if ($_.ErrorDetails.Message) { $errDetail = $_.ErrorDetails.Message }
      if ($attempt -lt 2 -and ($errDetail -match '429|quá tải|422|TOO_MANY|Gemini')) {
        Write-Host "  -> Loi quota/rate - cho ${RetryWaitSeconds}s roi thu lai..."
        Start-Sleep -Seconds $RetryWaitSeconds
      } else {
        $failChat++
        $lines += "--- Cau $idx ---"
        $lines += "Hoi: $q"
        $lines += "ERROR: $errDetail"
        $lines += ""
        Write-Host "  -> FAIL: $errDetail"
        $ok = $true
      }
    }
  }
  if ($idx -lt $questions.Count) {
    Write-Host "  Cho ${DelaySeconds}s..."
    Start-Sleep -Seconds $DelaySeconds
  }
}

$lines += "=== SUMMARY ==="
$lines += "Co dong Nguon: $passCite / $idx"
$lines += "Tu choi dung (ngoai pham vi): $passReject / $outOfScopeTotal"
$lines += "Chat errors: $failChat"

$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$outPath = Join-Path $root "docs\scripts\chat-cite-verify-5-results.txt"
$lines | Set-Content -Path $outPath -Encoding UTF8
Write-Host ""
Write-Host ($lines -join "`n")
Write-Host ""
Write-Host "Saved: $outPath"

if ($failChat -gt 0) { exit 1 }
exit 0
