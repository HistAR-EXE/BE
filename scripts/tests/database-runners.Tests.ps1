$beRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$workspaceRoot = Split-Path $beRoot -Parent

Describe 'database bootstrap runners' {
    It 'validates the Render migration and seed runner through V88 without connecting' {
        $output = & (Join-Path $beRoot 'docs\scripts\run-all-seed-render.ps1') -ValidateOnly 2>&1
        ($output -join "`n") | Should Match 'latest migration: V88'
    }

    It 'validates the full local bootstrap runner through V88 without Docker' {
        $output = & (Join-Path $beRoot 'docs\scripts\bootstrap-local-db.ps1') -ValidateOnly 2>&1
        ($output -join "`n") | Should Match 'latest migration: V88'
    }

    It 'discovers V88 in the workspace local bootstrap runner instead of a hard-coded version' {
        $output = & (Join-Path $workspaceRoot 'scripts\bootstrap-local-db.ps1') -ValidateOnly 2>&1
        ($output -join "`n") | Should Match 'latest migration: V88'
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
