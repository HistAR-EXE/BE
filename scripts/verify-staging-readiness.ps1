[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^https?://')]
    [string]$BaseUrl,
    [string]$MonitoringBearerToken,
    [ValidateRange(5, 120)]
    [int]$TimeoutSeconds = 20,
    [switch]$RequireBusinessMetrics
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

$businessMetrics = @(
    'histar_chat_latency_seconds',
    'histar_chat_fallback_total',
    'histar_rag_citations_total',
    'histar_quest_funnel_total',
    'histar_minigame_attempts_total'
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
