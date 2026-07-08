# Fills MAIL_* and FIREBASE_SERVICE_ACCOUNT_JSON in BE/.env.production (gitignored).
# Usage (from repo root or BE/):
#   powershell -ExecutionPolicy Bypass -File BE/scripts/fill-prod-auth-env.ps1 `
#     -Gmail "you@gmail.com" `
#     -AppPassword "your16charapppassword" `
#     -FirebaseJsonPath "C:\path\to\histar-a08c1-firebase-adminsdk-*.json"
param(
    [Parameter(Mandatory = $true)]
    [string]$Gmail,
    [Parameter(Mandatory = $true)]
    [string]$AppPassword,
    [Parameter(Mandatory = $true)]
    [string]$FirebaseJsonPath
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $root ".env.production"
if (-not (Test-Path $envFile)) {
    Copy-Item (Join-Path $root ".env.production.example") $envFile
}
if (-not (Test-Path $FirebaseJsonPath)) {
    throw "Firebase JSON not found: $FirebaseJsonPath"
}

$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($FirebaseJsonPath))
$lines = Get-Content $envFile -Encoding UTF8
$keys = @(
    "FRONTEND_URL", "MAIL_ENABLED", "MAIL_HOST", "MAIL_PORT", "MAIL_SMTP_AUTH", "MAIL_SMTP_STARTTLS",
    "MAIL_USERNAME", "MAIL_PASSWORD", "MAIL_FROM", "FIREBASE_ENABLED", "FIREBASE_SERVICE_ACCOUNT_JSON"
)
$filtered = $lines | Where-Object {
    $t = $_.Trim()
    if ($t -match '^\s*#') { return $true }
    $eq = $t.IndexOf('=')
    if ($eq -le 0) { return $true }
    $key = $t.Substring(0, $eq).Trim()
    $keys -notcontains $key
}
$newBlock = @(
    "",
    "# --- Auth: email verify + Google (filled by fill-prod-auth-env.ps1) ---",
    "FRONTEND_URL=https://fe-lake-five.vercel.app",
    "MAIL_ENABLED=true",
    "MAIL_HOST=smtp.gmail.com",
    "MAIL_PORT=587",
    "MAIL_SMTP_AUTH=true",
    "MAIL_SMTP_STARTTLS=true",
    "MAIL_USERNAME=$Gmail",
    "MAIL_PASSWORD=$AppPassword",
    "MAIL_FROM=$Gmail",
    "FIREBASE_ENABLED=true",
    "FIREBASE_SERVICE_ACCOUNT_JSON=$b64"
)
($filtered + $newBlock) | Set-Content $envFile -Encoding UTF8
Write-Host "Updated $envFile (MAIL + FIREBASE). Paste same vars to Render Dashboard."
