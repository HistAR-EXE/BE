@echo off
REM Seed Postgres Render — chay tu CMD hoac tu PowerShell:
REM   PowerShell: $env:RENDER_DB_PASSWORD="..." ; cmd /c docs\scripts\run-all-seed-render.cmd -IncludeSchema
REM   CMD:        set RENDER_DB_PASSWORD=... && docs\scripts\run-all-seed-render.cmd -IncludeSchema
REM Luu y: trong PowerShell KHONG dung "set" — dung $env:RENDER_DB_PASSWORD="..."
setlocal
if "%RENDER_DB_PASSWORD%"=="" (
  echo [ERROR] Dat bien: set RENDER_DB_PASSWORD=...
  exit /b 1
)
if "%RENDER_DB_HOST%"=="" (
  set "RENDER_DB_HOST=dpg-d9517c7lk1mc73c1u8sg-a.singapore-postgres.render.com"
)
cd /d "%~dp0..\.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-all-seed-render.ps1" ^
  -PgHost "%RENDER_DB_HOST%" ^
  -Password "%RENDER_DB_PASSWORD%" ^
  %*
endlocal
