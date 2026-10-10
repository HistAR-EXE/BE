[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^https?://')]
    [string]$BaseUrl,
    [string]$MonitoringBearerToken,
    [ValidateRange(5, 120)]
    [int]$TimeoutSeconds = 20,
    [switch]$RequireBusinessMetrics,
    [switch]$RequireProtectedMetrics
)

$ErrorActionPreference = 'Stop'

function Invoke-StagingGet {
    param([Parameter(Mandatory)][string]$Path)

    $uri = "$($BaseUrl.TrimEnd('/'))$Path"
    try {
        return Invoke-WebRequest -Uri $uri -Headers $headers -UseBasicParsing -TimeoutSec $TimeoutSeconds
    } catch {
        throw "Staging request failed for ${uri}: $($_.Exception.Message)"
    }
}

$headers = @{}
if (-not [string]::IsNullOrWhiteSpace($MonitoringBearerToken)) {
    $headers.Authorization = "Bearer $MonitoringBearerToken"
}

$readiness = Invoke-StagingGet '/api/health/ready'
if ($readiness.StatusCode -ne 200) {
    throw "Readiness endpoint returned HTTP $($readiness.StatusCode)."
}
$readinessBody = $readiness.Content | ConvertFrom-Json
if ($readinessBody.status -ne 'UP' -or $readinessBody.database -ne 'UP') {
    throw "Readiness endpoint is not fully UP: $($readiness.Content)"
}
Write-Host 'Readiness passed: application and database are UP.'

$prometheus = Invoke-StagingGet '/actuator/prometheus'
if ($prometheus.StatusCode -ne 200 -or [string]::IsNullOrWhiteSpace($prometheus.Content)) {
    throw 'Prometheus endpoint did not return a scrape payload.'
}
if ($prometheus.Content -notmatch '(?m)^jvm_info') {
    throw 'Prometheus endpoint response does not contain the expected JVM metric.'
}
Write-Host 'Prometheus endpoint passed: scrape payload is available.'

if ($RequireProtectedMetrics) {
    if ([string]::IsNullOrWhiteSpace($MonitoringBearerToken)) {
        throw 'MonitoringBearerToken is required when RequireProtectedMetrics is set.'
    }
    try {
        $unauthenticated = Invoke-WebRequest -Uri "$($BaseUrl.TrimEnd('/'))/actuator/prometheus" -UseBasicParsing -TimeoutSec $TimeoutSeconds
        throw "Prometheus endpoint is public (HTTP $($unauthenticated.StatusCode)); it must reject an unauthenticated scrape."
    } catch {
        # Windows PowerShell exposes a WebException.Response, whereas PowerShell 7's
        # Invoke-WebRequest normally throws HttpRequestException with StatusCode.
        # Treat both shapes identically so this gate verifies the backend rather than
        # depending on the shell used by CI or an operator.
        $code = $null
        if ($null -ne $_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
        } elseif ($null -ne $_.Exception.StatusCode) {
            $code = [int]$_.Exception.StatusCode
        } elseif ($null -ne $_.Exception.InnerException -and $null -ne $_.Exception.InnerException.Response) {
            $code = [int]$_.Exception.InnerException.Response.StatusCode
        }

        if ($code -notin 401,403) {
            throw "Unexpected unauthenticated Prometheus response: $($_.Exception.Message)"
        }
        Write-Host "Prometheus access control passed: unauthenticated request rejected with HTTP $code."
    }
}

$businessMetrics = @(
    'histar_chat_latency_seconds',
    'histar_chat_fallback_total',
    'histar_rag_citations_total',
    'histar_quest_funnel_total',
    'histar_minigame_attempts_total',
    'histar_chat_stream_total',
    'histar_chat_provider_latency_seconds',
    'histar_chat_grounded_answers_total',
    'histar_chat_first_token_latency_seconds',
    'histar_chat_safety_blocks_total',
    'histar_chat_memory_opt_out_total',
    'histar_chat_voice_fallback_total',
    'histar_teacher_ai_draft_evaluation_total'
)
$missingMetrics = @($businessMetrics | Where-Object { $prometheus.Content -notmatch [regex]::Escape($_) })
if ($RequireBusinessMetrics -and $missingMetrics.Count -gt 0) {
    throw "Missing business metrics after smoke traffic: $($missingMetrics -join ', ')."
}
if ($missingMetrics.Count -gt 0) {
    Write-Warning "Business metrics have not all been emitted yet: $($missingMetrics -join ', '). Trigger the documented smoke traffic, then rerun with -RequireBusinessMetrics."
} else {
    Write-Host 'Business metrics passed: all dashboard metric families are present.'
}
