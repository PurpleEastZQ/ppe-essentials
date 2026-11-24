<#
.SYNOPSIS
Previews or publishes the configured build matrix to Modrinth and CurseForge.

.DESCRIPTION
Use one UTF-8 Markdown file for the entire release. The default mode writes a
local preview under build/work for each target. To upload, use -Publish and set
MODRINTH_TOKEN and CURSEFORGE_TOKEN in the current environment.

.EXAMPLE
.\publish-all-versions.ps1 -Changelog .\release-notes.md

.EXAMPLE
.\publish-all-versions.ps1 -Changelog .\release-notes.md -Publish

.EXAMPLE
.\publish-all-versions.ps1 -Changelog .\release-notes.md -Publish -Platform curseforge -Loader fabric -MinecraftVersion 26.3
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string] $Changelog,
    [switch] $Publish,
    [ValidateNotNullOrEmpty()]
    [ValidateSet('modrinth', 'curseforge')]
    [string[]] $Platform = @('modrinth', 'curseforge'),
    [ValidateNotNullOrEmpty()]
    [ValidateSet('neoforge', 'fabric')]
    [string[]] $Loader = @('neoforge', 'fabric'),
    [string[]] $MinecraftVersion = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$changelogFile = Get-Item -LiteralPath $Changelog
if ($changelogFile -isnot [System.IO.FileInfo] -or
        [string]::IsNullOrWhiteSpace([System.IO.File]::ReadAllText($changelogFile.FullName))) {
    throw 'The changelog must be a nonempty Markdown file.'
}

$platforms = @($Platform | ForEach-Object { $_.ToLowerInvariant() } | Select-Object -Unique)
$buildScript = Join-Path $PSScriptRoot 'build-all-versions.ps1'
$targets = @{ Loader = $Loader; MinecraftVersion = $MinecraftVersion }
$mode = 'preview'
if ($Publish) {
    foreach ($destination in $platforms) {
        $tokenName = $destination.ToUpperInvariant() + '_TOKEN'
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($tokenName))) {
            throw "Set $tokenName before publishing to $destination."
        }
    }
    & $buildScript @targets
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    $mode = 'live'
}

$previousChangelog = $env:PPE_RELEASE_CHANGELOG
try {
    $env:PPE_RELEASE_CHANGELOG = $changelogFile.FullName
    $publishArgs = @('publishMods', "-Pppe_publish=$mode", "-Pppe_publish_platforms=$($platforms -join ',')")
    & $buildScript @targets -GradleArgs $publishArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    $env:PPE_RELEASE_CHANGELOG = $previousChangelog
}
