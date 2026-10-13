<#
.SYNOPSIS
Starts HistAR BE against the versioned Python RAG, without resetting any database.
.EXAMPLE
powershell -NoProfile -ExecutionPolicy Bypass -File .\docs\scripts\start-local-rag.ps1 -StartAi -SyncCorpus
#>
[CmdletBinding()]
param(
    [string]$AiDirectory = (Join-Path $PSScriptRoot '..\..\..\AI'),
    [ValidateRange(1024, 65535)][int]$BackendPort = 8080,
    [ValidateRange(1024, 65535)][int]$AiPort = 8100,
    [switch]$StartAi,
    [switch]$SyncCorpus,
    [switch]$PrintConfig,
    [switch]$CheckOnly
)
$ErrorActionPreference = 'Stop'
$backendDirectory = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
$resolvedAi = (Resolve-Path -LiteralPath $AiDirectory).Path
$aiUrl = "http://127.0.0.1:$AiPort"
$backendArguments = @(
    '--spring.profiles.active=local',
    "--server.port=$BackendPort",
    '--rag.enabled=false',
    "--chat.rag-ai.base-url=$aiUrl",
    '--chat.rag-ai.timeout-seconds=100'
)
$config = [ordered]@{
    backendDirectory = $backendDirectory
    envFile = (Join-Path $backendDirectory '.env')
    aiDirectory = $resolvedAi
    aiEnvFile = (Join-Path $resolvedAi '.env')
    profile = 'local'
    ragEngine = 'python-chroma'
    backendPort = $BackendPort
    aiUrl = $aiUrl
    backendArguments = $backendArguments
}
if ($PrintConfig) { $config | ConvertTo-Json -Depth 4; return }
if ($BackendPort -eq $AiPort) { throw 'BE and AI must use different ports.' }
$python = Join-Path $resolvedAi '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $python)) {
    throw 'AI virtualenv missing. Follow AI README to install the locked Python dependencies first.'
}

Push-Location $resolvedAi
try {
    if ($SyncCorpus) {
        & $python -m scripts.ingest --sync
        if ($LASTEXITCODE -ne 0) { throw 'Safe corpus synchronization failed; existing index was not reset.' }
    }
    $indexJson = & $python -m scripts.ingest --check
    if ($LASTEXITCODE -ne 0) { throw 'Local RAG index missing/stale. Run this launcher with -SyncCorpus.' }
    $expected = ($indexJson | Out-String) | ConvertFrom-Json
} finally { Pop-Location }

$listener = Get-NetTCPConnection -LocalPort $AiPort -State Listen -ErrorAction SilentlyContinue
if ($StartAi -and -not $listener) {
    $logs = Join-Path $backendDirectory 'target\local-rag'
    [void](New-Item -ItemType Directory -Force -Path $logs)
    $aiProcess = Start-Process -FilePath $python -WorkingDirectory $resolvedAi -WindowStyle Hidden -PassThru `
        -ArgumentList @('-m', 'uvicorn', 'app.main:app', '--host', '127.0.0.1', '--port', "$AiPort") `
        -RedirectStandardOutput (Join-Path $logs 'ai.stdout.log') `
        -RedirectStandardError (Join-Path $logs 'ai.stderr.log')
    Write-Host "Started AI PID $($aiProcess.Id). Logs: $logs"
}
$ready = $null
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    try { $ready = Invoke-RestMethod -Uri "$aiUrl/ai/ready" -TimeoutSec 5; break }
    catch {
        if (-not $StartAi -or $attempt -eq 59) {
            throw 'AI is not ready. Check its logs, sync the corpus, then restart AI. No existing process was killed.'
        }
        if ($aiProcess -and $aiProcess.HasExited) { throw 'AI process exited; check target/local-rag logs.' }
        Start-Sleep -Seconds 1
    }
}
if ($ready.status -ne 'UP' -or $ready.corpusFingerprint -ne $expected.corpusFingerprint -or
    $ready.embeddingVersion -ne $expected.embeddingVersion -or
    $ready.configFingerprint -ne $expected.configFingerprint) {
    throw 'Running AI does not match this checkout/index/config. Restart it from the intended AI directory.'
}
$config | ConvertTo-Json -Depth 4 | Write-Host
Write-Host "RAG ready. Corpus: $($ready.corpusFingerprint)"
if ($CheckOnly) { return }
if (Get-NetTCPConnection -LocalPort $BackendPort -State Listen -ErrorAction SilentlyContinue) {
    throw "Port $BackendPort is occupied. Choose -BackendPort or stop that backend yourself."
}

# EnvFileLoader runs before Spring; make its profile selection explicit too.
$previousProfile = $env:SPRING_PROFILES_ACTIVE
try {
    $env:SPRING_PROFILES_ACTIVE = 'local'
    Push-Location $backendDirectory
    try {
        & (Join-Path $backendDirectory 'mvnw.cmd') spring-boot:run "-Dspring-boot.run.arguments=$($backendArguments -join ' ')"
        if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    $env:SPRING_PROFILES_ACTIVE = $previousProfile
}
