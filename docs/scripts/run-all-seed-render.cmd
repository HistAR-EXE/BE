@echo off
REM Wrapper — run from BE\docs\scripts or repo scripts\run-all-seed-render.cmd
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-all-seed-render.ps1" %*
