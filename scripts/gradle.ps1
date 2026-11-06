# Portable launcher. Reuse the local shared workspace when available.
[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$Project, [string[]]$Tasks = @('build'), [switch]$Standalone)
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$projectRoot = (Resolve-Path -LiteralPath $Project).Path
if (-not $Standalone -and $env:MYCOMPANION_STANDALONE -ne '1') {
    $ancestor = Split-Path -Parent $repositoryRoot
    while ($ancestor) {
        $sharedLauncher = Join-Path $ancestor 'tools/gradle.ps1'
        if (Test-Path -LiteralPath $sharedLauncher) {
            & $sharedLauncher -Project $projectRoot -Tasks $Tasks
            exit $LASTEXITCODE
        }
        $ancestor = Split-Path -Parent $ancestor
    }
}
$properties = Get-Content -LiteralPath (Join-Path $projectRoot 'gradle.properties') -Raw
$isNeoForge = $properties -match '(?m)^neo_version='
$major = if ($isNeoForge) { 25 } else { 17 }
$environmentNames = @('JAVA_HOME','GRADLE_USER_HOME','JAVA_OPTS','TEMP','TMP','CODEX_JDK21','CODEX_JDK25')
$savedEnvironment = @{}
foreach ($name in $environmentNames) { $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$candidates = @()
foreach ($name in @('JAVA_HOME','JAVA_HOME_17_X64','JAVA_HOME_21_X64','JAVA_HOME_25_X64','CODEX_JDK21','CODEX_JDK25')) {
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    if ($value) { $candidates += $value }
}
$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if ($javaCommand) { $candidates += Split-Path -Parent (Split-Path -Parent $javaCommand.Source) }
function Find-Jdk([int]$requestedMajor) {
    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        $release = Join-Path $candidate 'release'
        if (-not (Test-Path -LiteralPath $release)) { continue }
        if (-not (Test-Path -LiteralPath (Join-Path $candidate 'bin/javac')) -and
            -not (Test-Path -LiteralPath (Join-Path $candidate 'bin/javac.exe'))) { continue }
        $versionLine = Get-Content -LiteralPath $release | Where-Object { $_ -match '^JAVA_VERSION=' }
        $match = [regex]::Match([string]$versionLine, '^JAVA_VERSION="(\d+)(?:\.|\+|")')
        if ($match.Success -and [int]$match.Groups[1].Value -eq $requestedMajor -and $versionLine -notmatch '-ea') { return $candidate }
    }
    throw "JDK $requestedMajor not found. Set JAVA_HOME_${requestedMajor}_X64 to its installation directory. See docs/BUILDING.md."
}
$exitCode = 1
try {
    $env:JAVA_HOME = Find-Jdk $major
    if ($isNeoForge) {
        $env:CODEX_JDK21 = Find-Jdk 21
        $env:CODEX_JDK25 = $env:JAVA_HOME
    }
    if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $repositoryRoot '.gradle-user-home' }
    $temporaryRoot = Join-Path $projectRoot '.gradle-temp'
    New-Item -ItemType Directory -Path $temporaryRoot -Force | Out-Null
    $env:TEMP = $temporaryRoot
    $env:TMP = $temporaryRoot
    $env:JAVA_OPTS = "$($savedEnvironment['JAVA_OPTS']) -Dfile.encoding=UTF-8"
    $proxyAddress = if ($env:HTTPS_PROXY) { $env:HTTPS_PROXY } else { $env:HTTP_PROXY }
    if ($proxyAddress) {
        $proxyUri = [Uri]$proxyAddress
        if ($proxyUri.Scheme -ne 'http') { throw 'Expected an HTTP proxy URL.' }
        $env:JAVA_OPTS += " -Dhttps.proxyHost=$($proxyUri.Host) -Dhttps.proxyPort=$($proxyUri.Port) -Dhttp.proxyHost=$($proxyUri.Host) -Dhttp.proxyPort=$($proxyUri.Port) `"-Dhttp.nonProxyHosts=localhost|127.*|[::1]`""
    }
    Push-Location -LiteralPath $projectRoot
    try {
        if ($IsWindows) { & (Join-Path $projectRoot 'gradlew.bat') --console=plain @Tasks }
        else { & bash (Join-Path $projectRoot 'gradlew') --console=plain @Tasks }
        $exitCode = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    foreach ($name in $savedEnvironment.Keys) { [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process') }
}
exit $exitCode
