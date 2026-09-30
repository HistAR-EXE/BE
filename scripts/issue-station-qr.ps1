<#
.SYNOPSIS
  Issue STATIC signed station QR payloads (site:ST0x:0:sig) via admin API for waterproof field kit.

.EXAMPLE
  .\issue-station-qr.ps1 -BaseUrl https://histar-postgre.onrender.com -Email admin@... -Password ... -SiteCode cu-chi
#>
param(
  [string]$BaseUrl = "http://localhost:8080",
  [Parameter(Mandatory = $true)][string]$Email,
  [Parameter(Mandatory = $true)][string]$Password,
  [string]$SiteCode = "cu-chi",
  [string[]]$Stations = @("ST01", "ST02", "ST03", "ST04", "ST05", "ST06"),
  [string]$OutFile = "station-qr-payloads.txt"
)

$ErrorActionPreference = "Stop"
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body (@{
  email = $Email
  password = $Password
} | ConvertTo-Json)

$token = $login.data.accessToken
if (-not $token) { $token = $login.data.token }
if (-not $token) { throw "Login failed — no access token" }

$headers = @{ Authorization = "Bearer $token" }
$lines = @("# TimeLens STATIC station QR — site=$SiteCode — $(Get-Date -Format o)", "# Set PRESENCE_QR_HMAC_SECRET on BE for signed=true", "")

foreach ($code in $Stations) {
  $uri = "$BaseUrl/api/admin/stations/$code/qr-token?siteCode=$([uri]::EscapeDataString($SiteCode))&static=true"
  $res = Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
  $payload = $res.data.payload
  $signed = $res.data.signed
  $lines += "$SiteCode`t$code`t$payload`tsigned=$signed"
  Write-Host "$SiteCode/$code => $payload (signed=$signed)"
}

$lines | Set-Content -Path $OutFile -Encoding UTF8
Write-Host "Wrote $OutFile"
