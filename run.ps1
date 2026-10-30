<#
.SYNOPSIS
    WasteConnect 1-Click Launch and Test Script for Windows PowerShell
#>
param(
    [ValidateSet("run", "test", "compile")]
    [string]$Action = "run"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Green
Write-Host "   WasteConnect - Smart Waste Collection Management    " -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Write-Host ""

# Detect JAVA_HOME
if (-not $env:JAVA_HOME) {
    if (Test-Path "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr") {
        $env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr"
    }
}

if ($env:JAVA_HOME) {
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Write-Host "[OK] Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Cyan
}

# Detect Maven
$mvnCmd = "mvn"
$ideaMvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd"
if (Test-Path $ideaMvn) {
    $mvnCmd = $ideaMvn
}
Write-Host "[OK] Using Maven: $mvnCmd" -ForegroundColor Cyan
Write-Host ""

switch ($Action) {
    "test" {
        Write-Host "Running 49 comprehensive tests..." -ForegroundColor Yellow
        & $mvnCmd test
    }
    "compile" {
        Write-Host "Compiling WasteConnect..." -ForegroundColor Yellow
        & $mvnCmd clean compile
    }
    Default {
        Write-Host "Launching WasteConnect JavaFX Application..." -ForegroundColor Green
        & $mvnCmd javafx:run
    }
}
