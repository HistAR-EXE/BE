@echo off
REM Seed / update Postgres Render toi schema + seed moi nhat.
REM Script PowerShell ben duoi se:
REM   - chay bo core SQL da xac minh
REM   - tu dong nhat them SQL moi trong BE\docs\database
REM   - tu dong nhat them Flyway SQL trong src\main\resources\db\migration
REM
REM SQL moi (Quest + QuestStep, monetization, shedlock):
REM   2026-07-07_monetization_p0.sql       — b2c_subscriptions, subscriptions, usage_quotas
REM   2026-07-08_quest_steps_schema.sql    — bang quest_steps (entity QuestStep)
REM   2026-07-08_quest_steps_init.sql      — seed 2 chien dich Cu Chi + buoc quest
REM   2026-07-09_shedlock.sql              — bang shedlock (scheduled jobs)
REM   2026-07-10_visit_sessions_ai_fields.sql — cot AI tren visit_sessions
REM   Flyway V14__quest_steps.sql          — dong bo schema quest_steps khi BE boot
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
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-all-seed-render.ps1" ^
  -PgHost "%RENDER_DB_HOST%" ^
  -Password "%RENDER_DB_PASSWORD%" ^
  %*
endlocal
