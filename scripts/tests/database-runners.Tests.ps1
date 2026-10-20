$beRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$workspaceRoot = Split-Path $beRoot -Parent
$latestVersion = (Get-ChildItem (Join-Path $beRoot 'src/main/resources/db/migration') -Filter 'V*__*.sql' | ForEach-Object { [int]([regex]::Match($_.Name, '^V(\d+)__').Groups[1].Value) } | Measure-Object -Maximum).Maximum

Describe 'database bootstrap runners' {
    It 'validates the Render migration and seed runner through the latest migration without connecting' {
        $output = & (Join-Path $beRoot 'docs\scripts\run-all-seed-render.ps1') -ValidateOnly 2>&1
        ($output -join "`n") | Should Match "latest migration: V$latestVersion"
    }

    It 'validates the full local bootstrap runner through the latest migration without Docker' {
        $output = & (Join-Path $beRoot 'docs\scripts\bootstrap-local-db.ps1') -ValidateOnly 2>&1
        ($output -join "`n") | Should Match "latest migration: V$latestVersion"
    }

    # The workspace convenience runner is not part of a standalone BE checkout.
    It 'discovers the latest migration in the workspace local bootstrap runner instead of a hard-coded version' -Skip:(-not (Test-Path (Join-Path $workspaceRoot 'scripts\bootstrap-local-db.ps1'))) {
        $output = & (Join-Path $workspaceRoot 'scripts\bootstrap-local-db.ps1') -ValidateOnly 2>&1
        ($output -join "`n") | Should Match "latest migration: V$latestVersion"
    }

    It 'adds a host-side DNS address to the Docker psql connection URI' {
        . (Join-Path $beRoot 'docs\scripts\Resolve-DockerPsqlHost.ps1')

        $psqlUrl = Resolve-DockerPsqlDatabaseUrl `
            -DatabaseUrl 'postgresql://db.example.test:5432/timelens?sslmode=require' `
            -HostName 'db.example.test' `
            -Resolver { param($name) @('2001:db8::10', '203.0.113.10') }

        $psqlUrl | Should Be 'postgresql://db.example.test:5432/timelens?sslmode=require&hostaddr=203.0.113.10'
    }

    It 'uses an ephemeral psql client when the configured container has no network' {
        . (Join-Path $beRoot 'docs\scripts\Resolve-DockerPsqlHost.ps1')

        Get-DockerPsqlMode -ContainerIsRunning $true -ContainerNetworksJson '{}' | Should Be 'ephemeral'
    }

    It 'normalizes an empty psql scalar result without throwing' {
        . (Join-Path $beRoot 'docs\scripts\Resolve-DockerPsqlHost.ps1')

        ConvertFrom-PsqlScalar -Value $null | Should Be ''
    }
}
