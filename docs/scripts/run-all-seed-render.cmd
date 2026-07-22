@echo off
REM Seed / update Postgres Render toi schema + seed moi nhat.
REM Script PowerShell ben duoi se:
REM   - (tuy chon) sinh lai SQL tour 360 tu manifest
REM   - chay bo core SQL da xac minh
REM   - tu dong nhat them SQL moi trong BE\docs\database
REM   - tu dong nhat them Flyway SQL trong src\main\resources\db\migration
REM
REM SQL / migration moi (Tour 360 multi-scene, 2026-07-10):
REM   2026-07-10_cu_chi_multi_panoramas.sql  — 12 panorama + scene links (tu manifest)
REM   Flyway V15__tour360_multi_scene.sql     — area_slug, marker_style, den t1-t6, yaw calibrated
REM
REM SQL truoc do (Quest + monetization + shedlock):
REM   2026-07-07_monetization_p0.sql
REM   2026-07-08_quest_steps_schema.sql / 2026-07-08_quest_steps_init.sql
REM   2026-07-09_shedlock.sql
REM   2026-07-10_visit_sessions_ai_fields.sql
REM   Flyway V14__quest_steps.sql
REM
REM Truoc khi seed (neu vua sua docs\cu-chi-tour-manifest.json):
REM   python docs\scripts\cu_chi_tour_pipeline.py sql
REM   (script nay tu go buoc tren neu co Python)
REM
REM DB Render DA CO data -> upgrade (KHONG -IncludeSchema):
REM   cmd /c scripts\run-all-seed-render.cmd
REM DB Render TRONG -> them -IncludeSchema (hoac reset-and-seed-render.ps1 neu schema loi)
REM
REM Chay tu thu muc BE:
REM   PowerShell: $env:RENDER_DB_PASSWORD="..." ; cmd /c "docs\scripts\run-all-seed-render.cmd"
REM
REM Chay tu thu muc goc repo (HistAR):
REM   PowerShell: $env:RENDER_DB_PASSWORD="..." ; cmd /c "scripts\run-all-seed-render.cmd"
REM
REM Luu y PowerShell:
REM   - KHONG dung "set RENDER_DB_PASSWORD=..." (chi co trong CMD)
REM   - KHONG go truc tiep docs\scripts\... (can cmd /c hoac .\scripts\...)
setlocal
if "%RENDER_DB_PASSWORD%"=="" (
  echo [ERROR] Dat bien: set RENDER_DB_PASSWORD=...
  exit /b 1
)
if "%RENDER_DB_HOST%"=="" (
  set "RENDER_DB_HOST=dpg-d9517c7lk1mc73c1u8sg-a.singapore-postgres.render.com"
)
cd /d "%~dp0..\.."
where python >nul 2>&1
if %ERRORLEVEL%==0 (
  echo [INFO] Regenerating cu-chi tour SQL from manifest...
  python "%~dp0cu_chi_tour_pipeline.py" sql
  if errorlevel 1 (
    echo [WARN] Pipeline sql failed — continuing with existing SQL files.
  )
) else (
  echo [WARN] Python not found — skip pipeline sql; using committed SQL files.
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-all-seed-render.ps1" ^
  -PgHost "%RENDER_DB_HOST%" ^
  -Password "%RENDER_DB_PASSWORD%" ^
  %*
endlocal
