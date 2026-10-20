function Resolve-DockerPsqlDatabaseUrl {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$DatabaseUrl,

        [Parameter(Mandatory = $true)]
        [string]$HostName,

        [scriptblock]$Resolver = {
            param([string]$Name)
            [System.Net.Dns]::GetHostAddresses($Name)
        }
    )

    $addresses = @(& $Resolver $HostName | ForEach-Object {
        $parsed = $null
        if ([System.Net.IPAddress]::TryParse($_.ToString(), [ref]$parsed)) {
            $parsed
        }
    })

    $selected = $addresses |
        Where-Object AddressFamily -eq ([System.Net.Sockets.AddressFamily]::InterNetwork) |
        Select-Object -First 1
    if (-not $selected) {
        $selected = $addresses |
            Where-Object AddressFamily -eq ([System.Net.Sockets.AddressFamily]::InterNetworkV6) |
            Select-Object -First 1
    }
    if (-not $selected) {
        throw "Could not resolve database host '$HostName' from Windows. Verify the Render External Database URL and network connection."
    }

    $separator = if ($DatabaseUrl.Contains('?')) { '&' } else { '?' }
    return "$DatabaseUrl${separator}hostaddr=$($selected.IPAddressToString)"
}

function Get-DockerPsqlMode {
    [CmdletBinding()]
    param(
        [bool]$ContainerIsRunning,
        [string]$ContainerNetworksJson
    )

    if (-not $ContainerIsRunning -or [string]::IsNullOrWhiteSpace($ContainerNetworksJson)) {
        return 'ephemeral'
    }

    try {
        $networks = $ContainerNetworksJson | ConvertFrom-Json
        if (@($networks.PSObject.Properties).Count -eq 0) {
            return 'ephemeral'
        }
    } catch {
        return 'ephemeral'
    }

    return 'existing-container'
}

function ConvertFrom-PsqlScalar {
    [CmdletBinding()]
    param($Value)

    return ([string]$Value).Trim()
}
