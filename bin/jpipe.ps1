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

# Graphviz is deliberately not checked here: only the image formats need it,
# the JAR reports a missing `dot` when one of them is asked for, and
# `jpipe doctor`, which diagnoses exactly that, must be able to run.
if (-not (Get-Command $java -ErrorAction SilentlyContinue)) {
    [Console]::Error.WriteLine('[jpipe] Java not found')
    exit 1
}

& $java -jar (Join-Path $prefix 'jpipe.jar') @args
exit $LASTEXITCODE
