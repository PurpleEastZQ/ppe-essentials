<#
.SYNOPSIS
Previews or publishes the configured build matrix to Modrinth and CurseForge.

.DESCRIPTION
Use one UTF-8 Markdown file for the entire release. The default mode writes a
local preview under build/work for each target. To upload, use -Publish.
Each shared JAR is uploaded once with all of its supported Minecraft versions.
For each selected platform, the script uses MODRINTH_TOKEN or CURSEFORGE_TOKEN
from the current environment, falling back to NAME=value entries in
.env.publish.local beside this script.

.EXAMPLE
.\publish-all-versions.ps1 -Changelog .\build\release-notes.md

.EXAMPLE
.\publish-all-versions.ps1 -Changelog .\build\release-notes.md -Publish

.EXAMPLE
.\publish-all-versions.ps1 -Changelog .\build\release-notes.md -Publish -Platform curseforge -Loader fabric -MinecraftVersion 26.3
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
$localTokens = @{}
if ($Publish) {
    $tokenFile = Join-Path $PSScriptRoot '.env.publish.local'
    $tokenFileContents = $null
    foreach ($destination in $platforms) {
        $tokenName = $destination.ToUpperInvariant() + '_TOKEN'
        if (![string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($tokenName))) {
            continue
        }
        if ($null -eq $tokenFileContents) {
            $tokenFileContents = if ([IO.File]::Exists($tokenFile)) { [IO.File]::ReadAllText($tokenFile) } else { '' }
        }

        $entries = [regex]::Matches($tokenFileContents, "(?m)^[ \t]*$tokenName[ \t]*=([^\r\n]*)")
        if ($entries.Count -gt 1) {
            throw "Define $tokenName only once in .env.publish.local."
        }
        if ($entries.Count -eq 0 -or $entries[0].Groups[1].Value.Trim() -notmatch '^\S+$') {
            throw "Set $tokenName in the environment or .env.publish.local before publishing to $destination."
        }
        $localTokens[$tokenName] = $entries[0].Groups[1].Value.Trim()
    }
    & $buildScript @targets
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    $mode = 'live'
}

$previousChangelog = $env:PPE_RELEASE_CHANGELOG
$previousTokens = @{}
try {
    foreach ($tokenName in $localTokens.Keys) {
        $previousTokens[$tokenName] = [Environment]::GetEnvironmentVariable($tokenName, 'Process')
        [Environment]::SetEnvironmentVariable($tokenName, $localTokens[$tokenName], 'Process')
    }
    $env:PPE_RELEASE_CHANGELOG = $changelogFile.FullName
    $publishArgs = @('publishMods', "-Pppe_publish=$mode", "-Pppe_publish_platforms=$($platforms -join ',')")
    & $buildScript @targets -GradleArgs $publishArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    $env:PPE_RELEASE_CHANGELOG = $previousChangelog
    foreach ($tokenName in $previousTokens.Keys) {
        [Environment]::SetEnvironmentVariable($tokenName, $previousTokens[$tokenName], 'Process')
    }
}
