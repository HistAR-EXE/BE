$scriptPath = Join-Path $PSScriptRoot '..\generate-cu-chi-station-qrs.ps1'

Describe 'generate-cu-chi-station-qrs' {
    It 'loads the signing secret from an env file and never emits an unsigned production QR' {
        $output = Join-Path $TestDrive 'signed'
        $envFile = Join-Path $TestDrive '.env'
        Set-Content -LiteralPath $envFile -Encoding UTF8 -Value 'PRESENCE_QR_HMAC_SECRET=test-only-secret-with-at-least-32-characters'
        $oldSecret = $env:PRESENCE_QR_HMAC_SECRET
        try {
            $env:PRESENCE_QR_HMAC_SECRET = ''
            & $scriptPath -OutputDirectory $output -EnvFile $envFile -Size 120
            $manifest = Get-Content -Raw -Encoding UTF8 (Join-Path $output 'manifest.json') | ConvertFrom-Json
            $manifest.Count | Should Be 6
            $manifest[0].mode | Should Be 'SIGNED_STATIC'
            $manifest[0].payload | Should Match '^cu-chi:ST01:0:[0-9a-f]{64}$'
        } finally {
            $env:PRESENCE_QR_HMAC_SECRET = $oldSecret
        }
    }

    It 'fails closed when no signing secret is available' {
        $output = Join-Path $TestDrive 'unsigned'
        $oldSecret = $env:PRESENCE_QR_HMAC_SECRET
        try {
            $env:PRESENCE_QR_HMAC_SECRET = ''
            $threw = $false
            try {
                & $scriptPath -OutputDirectory $output -EnvFile (Join-Path $TestDrive 'missing.env') -Size 120
            } catch {
                $threw = $true
            }
            $threw | Should Be $true
        } finally {
            $env:PRESENCE_QR_HMAC_SECRET = $oldSecret
        }
    }
}
