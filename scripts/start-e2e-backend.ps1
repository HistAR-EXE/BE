[CmdletBinding()]
param(
    # Matches docker-compose.yml's default host mapping. Override explicitly
    # only when a separate test database is mapped to another port.
    [string]$DatabaseUrl = 'jdbc:postgresql://localhost:5432/timelens',
    [string]$DatabaseUser = 'timelens',
    [string]$DatabasePassword = 'timelens_secret'
)

$ErrorActionPreference = 'Stop'

# This is intentionally a local-only harness. Values below are fake and must never be
# copied to staging or production configuration.
$env:HISTAR_DOTENV_ENABLED = 'false'
$env:DB_URL = $DatabaseUrl
$env:DB_USER = $DatabaseUser
$env:DB_PASSWORD = $DatabasePassword
$env:DEMO_ENABLED = 'true'
$env:DEMO_SECRET = 'dev-test-demo-secret'
$env:MAIL_ENABLED = 'false'
$env:HISTAR_TEST_HOOKS_ENABLED = 'true'
$env:HISTAR_TEST_HOOKS_SECRET = 'dev-test-hook-secret'
# The browser E2E suite exercises every Phase 2/3 capability.  These flags are
# intentionally local-harness-only; production defaults remain false until the
# staged release gates and content review have passed.
$env:PILOT_CHAT_STREAMING = 'true'
$env:PILOT_GUIDED_MODES = 'true'
$env:PILOT_JOURNEY_MEMORY = 'true'
$env:PILOT_TEACHER_AI_DRAFTS = 'true'
$env:PILOT_NARRATIVE_INVESTIGATION = 'true'
$env:PILOT_QUEST_CAMPAIGNS = 'true'
$env:SEPAY_ENABLED = 'true'
$env:SEPAY_BANK_CODE = 'MB'
$env:SEPAY_ACCOUNT_NUMBER = '0000000000'
$env:SEPAY_ACCOUNT_NAME = 'HistAR E2E Mock'
$env:SEPAY_WEBHOOK_SECRET = 'whsec_d4bUZa4YPpiArwc3LluZarMQRCh4vQNV'
$env:SEPAY_WEBHOOK_AUTH_MODE = 'hmac'

& .\mvnw.cmd spring-boot:run
