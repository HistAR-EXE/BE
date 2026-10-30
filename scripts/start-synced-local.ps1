[CmdletBinding()]
param([int]$Port = 8080)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
# Explicit localhost configuration: never load deployment credentials or mail.
$env:HISTAR_DOTENV_ENABLED = 'false'
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:5432/timelens'
$env:DB_USER = 'timelens'
$env:DB_PASSWORD = 'timelens_secret'
$env:JWT_SECRET = 'histar-local-only-development-secret-change-outside-local'
$env:MAIL_ENABLED = 'false'
$env:HISTAR_PASSPORT_MAIL_ENABLED = 'false'
$env:FIREBASE_ENABLED = 'false'
$env:MINIO_ENDPOINT = 'http://127.0.0.1:9000'
$env:MINIO_PUBLIC_URL = 'http://127.0.0.1:9000'
$env:MINIO_ROOT_USER = 'minioadmin'
$env:MINIO_ROOT_PASSWORD = 'minioadmin'
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:FLYWAY_ENABLED = 'true'
$env:SERVER_PORT = "$Port"
$env:CORS_ALLOWED_ORIGINS = 'http://127.0.0.1:5173,http://localhost:5173,http://127.0.0.1:5174'
& mvn -o spring-boot:run
exit $LASTEXITCODE
