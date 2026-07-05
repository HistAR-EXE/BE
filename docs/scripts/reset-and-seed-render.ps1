# Reset schema Render Postgres + seed day du (39 file SQL).
# Can chay khi DB bi loi (ddl-auto:update tao bang thieu cot, hoac profiles already exists).
#
#   powershell -ExecutionPolicy Bypass -File .\docs\scripts\reset-and-seed-render.ps1 `
#     -PgHost "dpg-d9517c7lk1mc73c1u8sg-a.singapore-postgres.render.com" `
#     -Password "YOUR_PASSWORD"

param(
  [Parameter(Mandatory = $true)]
  [string]$PgHost,

  [Parameter(Mandatory = $true)]
  [string]$Password,

  [string]$DbUser = "timelens",
  [string]$DbName = "timelens",
  [int]$Port = 5432,
  [int]$ConfirmSeconds = 5
)

$ErrorActionPreference = "Stop"
$here = Split-Path $MyInvocation.MyCommand.Path -Parent

Write-Host ""
Write-Host "[WARN] DROP SCHEMA public CASCADE tren ${PgHost}/${DbName}"
Write-Host "Ctrl+C trong $ConfirmSeconds giay de huy..."
Start-Sleep -Seconds $ConfirmSeconds

$env:PGPASSWORD = $Password
$env:PGSSLMODE = "require"

docker run --rm `
  -e "PGPASSWORD=$Password" `
  -e "PGSSLMODE=require" `
  postgres:16-alpine `
  psql -h $PgHost -p $Port -U $DbUser -d $DbName -v ON_ERROR_STOP=1 `
  -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO $DbUser; GRANT ALL ON SCHEMA public TO public;"

if ($LASTEXITCODE -ne 0) { throw "DROP SCHEMA failed (exit $LASTEXITCODE)" }

Write-Host "[OK] Schema reset. Bat dau seed..."
& "$here\run-all-seed-render.ps1" -PgHost $PgHost -Password $Password -DbUser $DbUser -DbName $DbName -Port $Port -IncludeSchema
