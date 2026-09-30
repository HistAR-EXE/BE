# Apply pending Flyway migrations to Render Postgres from your laptop (External URL).
# Use this when Render free tier times out before Spring Boot finishes migrate+boot.
#
# 1) Render → Postgres histar_postgre → Connect → External Database URL
# 2) Copy BE/.env.render-db.example → BE/.env.render-db and fill values
# 3) From BE/:
#      powershell -ExecutionPolicy Bypass -File .\scripts\flyway-migrate-render.ps1
#
# Requires Docker Desktop.

param(
  [string]$EnvFile = "",
  [string]$PgHost = "",
  [string]$Password = "",
  [string]$DbUser = "",
  [string]$DbName = "",
  [int]$Port = 5432,
  [ValidateSet("info", "migrate", "repair")]
  [string]$Command = "migrate"
)

$ErrorActionPreference = "Stop"
$beRoot = Split-Path $PSScriptRoot -Parent
if (-not $EnvFile) { $EnvFile = Join-Path $beRoot ".env.render-db" }

function Read-DotEnv([string]$Path) {
  $map = @{}
  if (-not (Test-Path $Path)) { return $map }
  Get-Content $Path -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) { return }
    $i = $line.IndexOf("=")
    if ($i -lt 1) { return }
    $k = $line.Substring(0, $i).Trim()
    $v = $line.Substring($i + 1).Trim().Trim('"').Trim("'")
    $map[$k] = $v
  }
  return $map
}

$envMap = Read-DotEnv $EnvFile
if (-not $PgHost) { $PgHost = $envMap["PGHOST"] }
if (-not $Password) { $Password = $envMap["PGPASSWORD"] }
if (-not $DbUser) { $DbUser = $(if ($envMap["PGUSER"]) { $envMap["PGUSER"] } else { "timelens" }) }
if (-not $DbName) { $DbName = $(if ($envMap["PGDATABASE"]) { $envMap["PGDATABASE"] } else { "histar_postgre" }) }
if ($envMap["PGPORT"]) { $Port = [int]$envMap["PGPORT"] }

# Allow full External URL: postgresql://user:pass@host:5432/db
$databaseUrl = $envMap["DATABASE_URL"]
if ($databaseUrl -and (-not $PgHost -or -not $Password)) {
  if ($databaseUrl -match '^(?:jdbc:)?postgres(?:ql)?://([^:]+):([^@]+)@([^:/]+):?(\d+)?/([^?]+)') {
    if (-not $DbUser) { $DbUser = [uri]::UnescapeDataString($Matches[1]) }
    if (-not $Password) { $Password = [uri]::UnescapeDataString($Matches[2]) }
    if (-not $PgHost) { $PgHost = $Matches[3] }
    if ($Matches[4]) { $Port = [int]$Matches[4] }
    if (-not $DbName) { $DbName = ($Matches[5] -split '\?')[0] }
  }
}

if (-not $PgHost -or -not $Password) {
  Write-Host @"
Missing PGHOST / PGPASSWORD.

1. Open Render → PostgreSQL → Connect → External Database URL
2. Copy BE\.env.render-db.example → BE\.env.render-db
3. Paste host + password (or full DATABASE_URL)
4. Re-run this script
"@ -ForegroundColor Yellow
  exit 1
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "Docker is required (postgres client + flyway images)."
}

$migrationDir = Join-Path $beRoot "src\main\resources\db\migration"
if (-not (Test-Path $migrationDir)) { throw "Migrations not found: $migrationDir" }

$jdbc = "jdbc:postgresql://${PgHost}:${Port}/${DbName}?sslmode=require"
Write-Host "Target: ${DbUser}@${PgHost}:${Port}/${DbName}"
Write-Host "Flyway $Command (baseline 13, locations=$migrationDir)"

# Probe connectivity first
Write-Host "`n=== Probe connection ==="
docker run --rm `
  -e "PGPASSWORD=$Password" `
  -e "PGSSLMODE=require" `
  postgres:16-alpine `
  psql -h $PgHost -p $Port -U $DbUser -d $DbName -tAc `
  "SELECT 'ok db='||current_database()||' flyway_max='||COALESCE((SELECT max(version)::text FROM flyway_schema_history), 'none')||' admin_notes='||EXISTS(SELECT 1 FROM information_schema.columns WHERE table_name='heritage_digitization_inquiries' AND column_name='admin_notes');"
if ($LASTEXITCODE -ne 0) { throw "Cannot connect to Render Postgres (check External host + password)." }

$migMount = ($migrationDir -replace '\\', '/')
# Docker Desktop on Windows: use path that docker accepts
$migVol = $migrationDir

Write-Host "`n=== Flyway $Command ==="
docker run --rm `
  -v "${migVol}:/flyway/sql:ro" `
  flyway/flyway:11-alpine `
  -url="$jdbc" `
  -user="$DbUser" `
  -password="$Password" `
  -locations="filesystem:/flyway/sql" `
  -baselineOnMigrate="true" `
  -baselineVersion="13" `
  -connectRetries="10" `
  $Command

if ($LASTEXITCODE -ne 0) { throw "Flyway $Command failed (exit $LASTEXITCODE)." }

Write-Host "`n=== Verify admin_notes ==="
docker run --rm `
  -e "PGPASSWORD=$Password" `
  -e "PGSSLMODE=require" `
  postgres:16-alpine `
  psql -h $PgHost -p $Port -U $DbUser -d $DbName -tAc `
  "SELECT column_name FROM information_schema.columns WHERE table_name='heritage_digitization_inquiries' AND column_name IN ('admin_notes','contacted_at') ORDER BY 1;"

Write-Host "`nDone. Redeploy BE on Render (Manual Deploy) — schema is ready, boot should pass validate." -ForegroundColor Green
