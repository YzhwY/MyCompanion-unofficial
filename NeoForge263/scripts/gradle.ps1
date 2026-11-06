# Works inside the shared workspace and in a standalone repository clone.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$repositoryRoot = Split-Path -Parent $projectRoot
$tasks = if ($args.Count) { $args } else { @('build') }
& (Join-Path $repositoryRoot 'scripts/gradle.ps1') -Project $projectRoot -Tasks $tasks
exit $LASTEXITCODE
