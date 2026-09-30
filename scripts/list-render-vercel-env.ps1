# Prints env var names to copy from BE/.env.production → Render, and FE/.env → Vercel.
# Does NOT print secret values.
$fe = Resolve-Path (Join-Path (Split-Path $PSScriptRoot -Parent) "..\FE\.env") -ErrorAction SilentlyContinue

Write-Host "=== Render (BE) — paste from BE/.env.production ===" -ForegroundColor Cyan
@(
  "SPRING_PROFILES_ACTIVE", "FRONTEND_URL",
  "MAIL_ENABLED", "MAIL_HOST", "MAIL_PORT", "MAIL_SMTP_AUTH", "MAIL_SMTP_STARTTLS",
  "MAIL_USERNAME", "MAIL_PASSWORD", "MAIL_FROM",
  "FIREBASE_ENABLED", "FIREBASE_SERVICE_ACCOUNT_JSON",
  "JWT_SECRET", "GEMINI_API_KEY", "CORS_ALLOWED_ORIGINS", "SEPAY_*"
) | ForEach-Object { Write-Host "  $_" }

Write-Host "`n=== Vercel (FE) — paste from FE/.env (+ set VITE_API_URL=Render) ===" -ForegroundColor Cyan
@(
  "VITE_API_URL", "VITE_MEDIA_BASE_URL",
  "VITE_FIREBASE_API_KEY", "VITE_FIREBASE_AUTH_DOMAIN",
  "VITE_FIREBASE_PROJECT_ID", "VITE_FIREBASE_APP_ID"
) | ForEach-Object { Write-Host "  $_" }

if ($fe -and (Test-Path $fe)) {
  $missing = @()
  Get-Content $fe | ForEach-Object {
    if ($_ -match '^VITE_FIREBASE_(API_KEY|APP_ID)=$') { $missing += $_.Split('=')[0] }
  }
  if ($missing.Count -gt 0) {
    Write-Host "`n[WARN] FE missing Firebase web config: $($missing -join ', ')" -ForegroundColor Yellow
    Write-Host "  Firebase Console → Project histar-a08c1 → Project settings → Your apps → Web"
  }
}
