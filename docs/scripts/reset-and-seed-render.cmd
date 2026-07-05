@echo off
REM Reset + seed Render Postgres. PowerShell (khuyen nghi):
REM   powershell -ExecutionPolicy Bypass -File docs\scripts\reset-and-seed-render.ps1 -PgHost "HOST" -Password "PASS"
REM CMD (can set env truoc):
REM   set RENDER_DB_PASSWORD=... && docs\scripts\reset-and-seed-render.cmd
setlocal
if "%RENDER_DB_PASSWORD%"=="" (
  echo [ERROR] Dat bien: set RENDER_DB_PASSWORD=...
  exit /b 1
)
if "%RENDER_DB_HOST%"=="" (
  set "RENDER_DB_HOST=dpg-d9517c7lk1mc73c1u8sg-a.singapore-postgres.render.com"
)
cd /d "%~dp0..\.."
echo.
echo [WARN] DROP SCHEMA public CASCADE tren %RENDER_DB_HOST%
echo Ctrl+C trong 5 giay de huy...
timeout /t 5 /nobreak >nul
docker run --rm -e "PGPASSWORD=%RENDER_DB_PASSWORD%" -e "PGSSLMODE=require" postgres:16-alpine psql -h %RENDER_DB_HOST% -p 5432 -U timelens -d timelens -v ON_ERROR_STOP=1 -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO timelens; GRANT ALL ON SCHEMA public TO public;"
if errorlevel 1 exit /b 1
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-all-seed-render.ps1" -PgHost "%RENDER_DB_HOST%" -Password "%RENDER_DB_PASSWORD%" -IncludeSchema
endlocal
