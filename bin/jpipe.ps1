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

$jar = Join-Path $prefix 'jpipe.jar'
$aot = Join-Path $prefix 'jpipe.aot'

# An AOT cache of the classes a call loads makes it start about 30% faster
# (#216). It only works for the Java build and the JAR path that created it,
# so it is created here, on this machine: each install channel runs
# `jpipe --create-aot-cache` once, and a user can run it again after a Java
# upgrade. A stale cache is ignored without a word.
# It is the launcher's own command, which the JAR does not know: it takes no
# other argument, and anywhere but first it would reach the JAR as an unknown
# option.
if (($args -contains '--create-aot-cache') -and -not ($args.Count -eq 1 -and $args[0] -eq '--create-aot-cache')) {
    [Console]::Error.WriteLine("[jpipe] --create-aot-cache takes no other option: run 'jpipe --create-aot-cache'")
    exit 2
}

if ($args.Count -ge 1 -and $args[0] -eq '--create-aot-cache') {
    # Windows PowerShell turns a native command's stderr into errors, which
    # 'Stop' would make fatal: the training run's output is only kept, to be
    # shown if it fails.
    $ErrorActionPreference = 'Continue'
    $tmp = "$aot.tmp"
    Remove-Item -Force -ErrorAction SilentlyContinue $tmp
    $log = & $java "-XX:AOTCacheOutput=$tmp" '-Xlog:aot=off' -jar $jar `
        --headless process -i (Join-Path $prefix 'aot-training.jd') `
        -m trained -f python 2>&1
    if ($LASTEXITCODE -eq 0 -and (Test-Path $tmp)) {
        try {
            Move-Item -Force $tmp $aot -ErrorAction Stop
            exit 0
        } catch {
            $log = @($log) + "$_"
        }
    }
    # What the training run printed is the only clue to why it failed.
    $log | ForEach-Object { [Console]::Error.WriteLine("$_") }
    Remove-Item -Force -ErrorAction SilentlyContinue $tmp
    [Console]::Error.WriteLine('[jpipe] could not create the AOT cache; jPipe runs without it')
    exit 1
}

if (Test-Path $aot) {
    & $java "-XX:AOTCache=$aot" '-Xlog:aot=off' -jar $jar @args
} else {
    & $java -jar $jar @args
}
exit $LASTEXITCODE
