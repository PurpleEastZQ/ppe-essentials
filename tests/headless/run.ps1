[CmdletBinding()]
param(
    [ValidateSet('fabric', 'neoforge')]
    [string[]] $Loader = @('fabric', 'neoforge'),
    [ValidateRange(1024, 65535)]
    [int] $Port = 25578,
    [Parameter(Mandatory)]
    [switch] $AcceptEula
)

$ErrorActionPreference = 'Stop'
if (!$AcceptEula) { throw 'Accept the Minecraft EULA before running the test servers.' }
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$java = (Get-Command java -CommandType Application | Select-Object -First 1).Source
$go = (Get-Command go -CommandType Application).Source
$properties = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $repo 'gradle.properties') -Raw)
$work = Join-Path $repo "runs\headless\$([Guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $work -Force | Out-Null
Write-Host "Test files and logs: $work"

Push-Location $PSScriptRoot
try {
    $clientPath = Join-Path $work 'ppe-headless.exe'
    & $go build -mod=readonly -o $clientPath .
    if ($LASTEXITCODE) { throw 'Headless client build failed.' }
} finally {
    Pop-Location
}

foreach ($target in $Loader | Select-Object -Unique) {
    $artifact = Join-Path $repo "build\libs\ppe_essentials-$target-26.3-$($properties.mod_version).jar"
    if (!(Test-Path -LiteralPath $artifact -PathType Leaf)) {
        throw "Build the 26.3 $target JAR first: $artifact"
    }
    $server = Join-Path $work $target
    $mods = New-Item -ItemType Directory -Path (Join-Path $server 'mods') -Force
    Copy-Item -LiteralPath $artifact -Destination $mods.FullName
    New-Item -ItemType Directory -Path (Join-Path $server 'config') | Out-Null
    $configPath = Join-Path $server 'config/ppe_essentials-common.toml'
    @(
        'allowSelfTeleportRequests = false',
        'rtpMinDistance = 20',
        'rtpMaxDistance = 28',
        '',
        '[commands.spawn]',
        'enabled = false'
    ) | Set-Content -LiteralPath $configPath
    $legacyPath = Join-Path $server 'world/data/ppe_essentials_player_data.dat'
    New-Item -ItemType Directory -Path (Split-Path -Parent $legacyPath) | Out-Null
    & $clientPath -seed-legacy-data $legacyPath
    if ($LASTEXITCODE) { throw 'Failed to seed legacy player data.' }
    $legacyHash = (Get-FileHash -LiteralPath $legacyPath).Hash
    'eula=true' | Set-Content -LiteralPath "$server\eula.txt"
    @(
        'server-ip=127.0.0.1', "server-port=$Port", 'online-mode=false',
        'enforce-secure-profile=false', 'white-list=false', 'level-type=minecraft:flat',
        'view-distance=3', 'simulation-distance=3', 'spawn-protection=0',
        'gamemode=creative', 'difficulty=peaceful'
    ) | Set-Content -LiteralPath "$server\server.properties"
    '[{"uuid":"11079b6e-f264-3a24-9aba-832694e0063f","name":"PPE_TestA","level":4,"bypassesPlayerLimit":false},{"uuid":"54486276-0ea2-34fb-86bf-10acdacf32f1","name":"PPE_TestB","level":2,"bypassesPlayerLimit":false}]' |
        Set-Content -LiteralPath "$server\ops.json"

    if ($target -eq 'fabric') {
        $fabric = $properties.mc_26_3_fabric_loader_version
        $api = $properties.mc_26_3_fabric_api_version
        Invoke-WebRequest -UseBasicParsing -TimeoutSec 60 -Uri "https://meta.fabricmc.net/v2/versions/loader/26.3/$fabric/1.1.1/server/jar" -OutFile "$server\fabric-server.jar"
        Invoke-WebRequest -UseBasicParsing -TimeoutSec 60 -Uri "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$api/fabric-api-$api.jar" -OutFile "$server\mods\fabric-api.jar"
        $launchArguments = '-Xmx2G -jar fabric-server.jar nogui'
    } else {
        $neo = $properties.mc_26_3_neo_version
        Invoke-WebRequest -UseBasicParsing -TimeoutSec 60 -Uri "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neo/neoforge-$neo-installer.jar" -OutFile "$server\installer.jar"
        Push-Location $server
        try {
            & $java -jar installer.jar --installServer
            if ($LASTEXITCODE) { throw 'NeoForge test server installation failed.' }
        } finally {
            Pop-Location
        }
        $launchArguments = "-Xmx2G @libraries/net/neoforged/neoforge/$neo/win_args.txt nogui"
    }

    Write-Host "Testing Minecraft 26.3 on $target"
    $start = [Diagnostics.ProcessStartInfo]::new($java, $launchArguments)
    $start.WorkingDirectory = $server
    $start.UseShellExecute = $false
    $start.RedirectStandardInput = $true
    $process = [Diagnostics.Process]::Start($start)
    try {
        $log = Join-Path $server 'logs\latest.log'
        $deadline = [DateTime]::UtcNow.AddMinutes(3)
        while (!(Test-Path -LiteralPath $log) -or !(Select-String -LiteralPath $log -Pattern 'Done \(' -Quiet)) {
            if ($process.HasExited -or [DateTime]::UtcNow -gt $deadline) {
                throw "$target test server did not start. See $log."
            }
            Start-Sleep -Milliseconds 400
        }
        $process.StandardInput.WriteLine('list')
        while (!(Select-String -LiteralPath $log -Pattern 'There are .* players online' -Quiet)) {
            if ($process.HasExited -or [DateTime]::UtcNow -gt $deadline) {
                throw "$target test server did not begin processing commands. See $log."
            }
            Start-Sleep -Milliseconds 400
        }
        & $clientPath -address "127.0.0.1:$Port" -config $configPath
        if ($LASTEXITCODE) { throw "$target headless test failed. See $log." }
    } finally {
        if (!$process.HasExited) {
            try { $process.StandardInput.WriteLine('stop') }
            finally { if (!$process.WaitForExit(15000)) { $process.Kill(); $process.WaitForExit() } }
        }
        $process.Dispose()
    }
    $currentPath = Join-Path $server 'world/data/ppe_essentials/player_data.dat'
    if (!(Test-Path -LiteralPath $currentPath -PathType Leaf) -or
            (Get-FileHash -LiteralPath $legacyPath).Hash -ne $legacyHash) {
        throw "$target did not persist migrated data while preserving the legacy file."
    }
}
