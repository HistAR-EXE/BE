# Fills VITE_FIREBASE_API_KEY and VITE_FIREBASE_APP_ID in FE/.env (gitignored).
# Get values from Firebase Console → Project histar-a08c1 → Project settings → Your apps → Web
param(
    [Parameter(Mandatory = $true)]
    [string]$ApiKey,
    [Parameter(Mandatory = $true)]
    [string]$AppId
)

$ErrorActionPreference = "Stop"
$feRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\FE")
$envFile = Join-Path $feRoot ".env"
if (-not (Test-Path $envFile)) {
    Copy-Item (Join-Path $feRoot ".env.example") $envFile
}

$lines = Get-Content $envFile -Encoding UTF8
$out = foreach ($line in $lines) {
    if ($line -match '^VITE_FIREBASE_API_KEY=') { "VITE_FIREBASE_API_KEY=$ApiKey" }
    elseif ($line -match '^VITE_FIREBASE_APP_ID=') { "VITE_FIREBASE_APP_ID=$AppId" }
    else { $line }
}
$out | Set-Content $envFile -Encoding UTF8
Write-Host "Updated FE/.env Firebase web config. Paste same vars to Vercel."
