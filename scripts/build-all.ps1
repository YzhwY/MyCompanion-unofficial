# Build both maintained editions using their own JDK/cache wrappers.
[CmdletBinding()]
param([switch]$Offline, [switch]$Clean, [switch]$Standalone, [switch]$NoDaemon, [ValidateSet('All','Fabric1201','NeoForge263')][string]$Edition = 'All')
$ErrorActionPreference = 'Stop'
$developmentRoot = Split-Path -Parent $PSScriptRoot
$powerShellExe = (Get-Process -Id $PID).Path
$gradleTasks = @()
if ($Clean) { $gradleTasks += 'clean' }
$gradleTasks += 'build'
if ($Offline) { $gradleTasks += '--offline' }
if ($NoDaemon) { $gradleTasks += '--no-daemon' }
$failures = @()
$editions = if ($Edition -eq 'All') { @('Fabric1201', 'NeoForge263') } else { @($Edition) }
$previousMode = $env:MYCOMPANION_STANDALONE
try {
if ($Standalone) { $env:MYCOMPANION_STANDALONE = '1' }
foreach ($currentEdition in $editions) {
    Write-Host "Building $currentEdition"
    $launcher = Join-Path $developmentRoot "$currentEdition/scripts/gradle.ps1"
    & $powerShellExe -NoProfile -ExecutionPolicy Bypass -File $launcher @gradleTasks
    if ($LASTEXITCODE -ne 0) { $failures += "$currentEdition (exit $LASTEXITCODE)" }
}
} finally { $env:MYCOMPANION_STANDALONE = $previousMode }
if ($failures.Count -gt 0) { throw "Build failed: $($failures -join ', ')" }
$verificationArguments = @((Join-Path $PSScriptRoot 'verify-releases.py'))
if ($Edition -ne 'All') { $verificationArguments += @('--edition', $Edition) }
& python @verificationArguments
if ($LASTEXITCODE -ne 0) { throw 'Release verification failed.' }
Write-Host 'Selected editions built and release archives verified.'
