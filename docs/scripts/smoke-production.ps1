# CP3 Week 3 (Ngày 17+) — Smoke toàn bộ trên URL production
param(
  [Parameter(Mandatory = $true)]
  [string]$BaseUrl
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")

Write-Host "=== Production smoke: $BaseUrl ==="

# Readiness
$ready = Invoke-RestMethod -Uri "$BaseUrl/api/health/ready"
if ($ready.database -ne "UP") {
  throw "Readiness failed: database not UP"
}
Write-Host "OK /api/health/ready"

$live = Invoke-RestMethod -Uri "$BaseUrl/api/health"
if ($live.data.status -ne "UP") {
  throw "Liveness failed"
}
Write-Host "OK /api/health"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& "$scriptDir\smoke-golden-path.ps1" -BaseUrl $BaseUrl
& "$scriptDir\smoke-week2-screens.ps1" -BaseUrl $BaseUrl

Write-Host ""
Write-Host "Production smoke PASSED for $BaseUrl"
Write-Host "Next: open FE production, test CORS login + 1 chat message."
