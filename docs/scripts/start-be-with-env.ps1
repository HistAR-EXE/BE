# Load .env into process env and start Spring Boot (for local dev on Windows)
$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$envFile = Join-Path $root ".env"
if (-not (Test-Path $envFile)) { throw "Missing .env at $envFile" }

Get-Content $envFile -Encoding UTF8 | ForEach-Object {
  $line = $_.Trim()
  if ($line -eq '' -or $line.StartsWith('#')) { return }
  $eq = $line.IndexOf('=')
  if ($eq -lt 1) { return }
  $name = $line.Substring(0, $eq).Trim()
  $value = $line.Substring($eq + 1).Trim()
  if ($name) { Set-Item -Path "env:$name" -Value $value }
}

if (-not $env:GEMINI_API_KEY) {
  Write-Warning "GEMINI_API_KEY is empty - save .env and use a Google AI Studio key (AIza...)"
} else {
  Write-Host "Loaded GEMINI_API_KEY from .env"
}

Set-Location $root
& .\mvnw.cmd spring-boot:run @args
