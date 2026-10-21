[CmdletBinding()]
param([string]$Database = 'timelens_ar_release')

$ErrorActionPreference = 'Stop'
if ($Database -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') { throw 'Invalid local database name.' }
Set-Location (Split-Path $PSScriptRoot -Parent)
# Isolated local configuration. Never load a production .env for this launcher.
$env:HISTAR_DOTENV_ENABLED = 'false'
$env:DB_URL = "jdbc:postgresql://127.0.0.1:5432/$Database"
$env:DB_USER = 'timelens'
$env:DB_PASSWORD = 'timelens_secret'
$env:JWT_SECRET = 'histar-local-ar-only-development-secret-2026'
$env:MAIL_ENABLED = 'false'
$env:HISTAR_PASSPORT_MAIL_ENABLED = 'false'
$env:FIREBASE_ENABLED = 'false'
$env:HISTAR_TEST_HOOKS_ENABLED = 'false'
$env:DEMO_ENABLED = 'false'
$env:SPRING_PROFILES_ACTIVE = 'default'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
$env:SPRING_JPA_SHOW_SQL = 'false'
$env:SPRING_DATA_REDIS_REPOSITORIES_ENABLED = 'false'
$env:SERVER_PORT = '8080'
$env:CORS_ALLOWED_ORIGINS = 'http://127.0.0.1:5175,http://localhost:5175,http://127.0.0.1:5173,http://localhost:5173'
& .\mvnw.cmd '-Dmaven.test.skip=true' spring-boot:run
exit $LASTEXITCODE
