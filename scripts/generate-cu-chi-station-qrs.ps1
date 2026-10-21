[CmdletBinding()]
param(
    [string]$OutputDirectory = '',
    [string]$SiteCode = 'cu-chi',
    [string]$Secret = $env:PRESENCE_QR_HMAC_SECRET,
    [string]$EnvFile = '',
    [switch]$AllowUnsignedDemo,
    [int]$Size = 900
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $PSScriptRoot '..\docs\qr\cu-chi'
}
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $PSScriptRoot '..\.env'
}

$npmCommand = if ($env:OS -eq 'Windows_NT') { 'npm.cmd' } else { 'npm' }
if (-not (Get-Command $npmCommand -ErrorAction SilentlyContinue)) {
    throw 'npm is required to render QR PNG files.'
}

$site = $SiteCode.Trim().ToLowerInvariant()
if ($site -notmatch '^[a-z0-9][a-z0-9-]{0,63}$') {
    throw "Invalid site code: $SiteCode"
}

if ([string]::IsNullOrWhiteSpace($Secret) -and (Test-Path -LiteralPath $EnvFile)) {
    $secretLine = Get-Content -LiteralPath $EnvFile -Encoding UTF8 |
        Where-Object { $_ -match '^\s*PRESENCE_QR_HMAC_SECRET\s*=' } |
        Select-Object -Last 1
    if ($secretLine) {
        $Secret = (($secretLine -split '=', 2)[1]).Trim().Trim('"').Trim("'")
    }
}

if ([string]::IsNullOrWhiteSpace($Secret) -and -not $AllowUnsignedDemo) {
    throw 'PRESENCE_QR_HMAC_SECRET is missing. Configure it in BE/.env or pass -AllowUnsignedDemo explicitly for a local-only demo.'
}

# JSON unicode escapes keep station names intact even under Windows PowerShell 5.1,
# which reads BOM-less .ps1 source as an ANSI code page.
$stations = ConvertFrom-Json '[
  {"Code":"ST01","Name":"Ph\u00f2ng h\u1ecdp"},
  {"Code":"ST02","Name":"Kho ng\u1ea7m"},
  {"Code":"ST03","Name":"Gi\u1ebfng h\u1ea7m & th\u00f4ng gi\u00f3"},
  {"Code":"ST04","Name":"Qu\u00e2n y"},
  {"Code":"ST05","Name":"X\u01b0\u1edfng v\u0169 kh\u00ed"},
  {"Code":"ST06","Name":"H\u1ea7m ph\u00f2ng th\u1ee7"}
]'

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

function Get-Signature([string]$Message, [string]$Key) {
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([System.Text.Encoding]::UTF8.GetBytes($Key))
    try {
        return ([BitConverter]::ToString($hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($Message))) -replace '-', '').ToLowerInvariant()
    } finally {
        $hmac.Dispose()
    }
}

$manifest = [System.Collections.Generic.List[object]]::new()
$mode = if ([string]::IsNullOrWhiteSpace($Secret)) { 'UNSIGNED_DEMO' } else { 'SIGNED_STATIC' }

foreach ($station in $stations) {
    $unsigned = "$site`:$($station.Code):0"
    $signature = if ([string]::IsNullOrWhiteSpace($Secret)) { '' } else { Get-Signature $unsigned $Secret }
    $payload = "$unsigned`:$signature"
    $fileName = "$($station.Code).png"
    $outputPath = Join-Path $OutputDirectory $fileName

    & $npmCommand exec --yes --package=qrcode -- qrcode -t png -e H -w $Size -q 4 -o $outputPath -- $payload
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $outputPath)) {
        throw "Failed to render QR for $($station.Code)."
    }

    $manifest.Add([ordered]@{
        siteCode = $site
        stationCode = $station.Code
        stationName = $station.Name
        payload = $payload
        file = $fileName
        mode = $mode
    })
}

$manifestPath = Join-Path $OutputDirectory 'manifest.json'
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($manifestPath, ($manifest | ConvertTo-Json -Depth 4), $utf8NoBom)

$readmePath = Join-Path $OutputDirectory 'README.md'
$modeNote = if ($mode -eq 'UNSIGNED_DEMO') {
    '> WARNING: QR is unsigned because PRESENCE_QR_HMAC_SECRET is missing. Use only for local/demo when BE also allows unsigned QR. Before printing or production, run again with the same secret configured on the deployed BE.'
} else {
    '> QR is signed with HMAC-SHA256. Do not change the BE secret after printing; changing it requires regenerating all QR files.'
}
$readme = @"
# QR check-in - $site

Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')

- Mode: **$mode**
- Static payload format: `siteCode:stationCode:0:signature`.
- `ST01`-`ST06` are the six Cu Chi stations.

$modeNote

Never commit the secret. `manifest.json` contains only signed payloads for internal verification.
"@
[System.IO.File]::WriteAllText($readmePath, $readme, $utf8NoBom)

Write-Host "Generated 6 QR PNG files in: $((Resolve-Path $OutputDirectory).Path)"
Write-Host "Mode: $mode"
