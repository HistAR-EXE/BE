$ErrorActionPreference = 'Stop'
$runner = Join-Path $PSScriptRoot 'start-local-rag.ps1'
if (-not (Test-Path -LiteralPath $runner)) { throw 'Missing RAG launcher' }
$raw = & $runner -PrintConfig -BackendPort 8097
if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw 'Launcher failed' }
$config = ($raw | Out-String) | ConvertFrom-Json
if ($config.backendPort -ne 8097) { throw 'Port override ignored' }
if ($config.profile -ne 'local') { throw 'Explicit local profile required' }
if ($config.ragEngine -ne 'python-chroma') { throw 'Unexpected RAG engine' }
if ($config.backendArguments -notcontains '--rag.enabled=false') { throw 'pgvector fallback must be explicit/off' }
if ($config.backendArguments -contains '--spring.flyway.enabled=false') { throw 'Normal launch must not disable Flyway' }
if ($config.backendArguments -notcontains '--chat.rag-ai.timeout-seconds=100') { throw 'Incorrect RAG budget' }
if ($raw -match 'PASSWORD|API_KEY|SECRET=') { throw 'Launcher output contains sensitive configuration names' }
Write-Host 'RAG launcher configuration tests passed.'
