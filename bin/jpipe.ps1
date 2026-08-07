#!/usr/bin/env pwsh
# jPipe launcher template (Windows / PowerShell).
#
# @@JAVA@@ and @@PREFIX@@ are substituted at package-install time by the
# packaging channel (see ADR-0021 and ADR-0025). Left unsubstituted, the script
# falls back to PATH and its own directory so it can be run straight from the
# repository during development.

$ErrorActionPreference = 'Stop'

$java = '@@JAVA@@'
if ($java -like '@@*@@') { $java = 'java' }

$prefix = '@@PREFIX@@'
if ($prefix -like '@@*@@') { $prefix = $PSScriptRoot }

if (-not (Get-Command $java -ErrorAction SilentlyContinue)) {
    Write-Host '[jpipe] Java not found'
    exit 1
}

if (-not (Get-Command 'dot' -ErrorAction SilentlyContinue)) {
    Write-Host '[jpipe] Graphviz not found'
    exit 1
}

& $java -jar (Join-Path $prefix 'jpipe.jar') @args
exit $LASTEXITCODE
