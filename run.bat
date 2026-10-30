@echo off
setlocal enabledelayedexpansion
title WasteConnect - Smart Waste Collection Management System

echo ========================================================
echo    WasteConnect - Smart Waste Collection Management
echo ========================================================
echo.

:: Detect Java JDK
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr" (
        set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr"
    )
)
if not "%JAVA_HOME%"=="" (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo [OK] Using JAVA_HOME: %JAVA_HOME%
) else (
    echo [INFO] Using system default Java
)

:: Detect Maven
set "MVN_CMD=mvn"
if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd" (
    set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd"
)
echo [OK] Using Maven: %MVN_CMD%
echo.

echo Choose an action:
echo  1. Launch WasteConnect Desktop Application (Default)
echo  2. Run Full Test Suite (49 automated tests)
echo  3. Clean and Recompile
echo  4. Exit
echo.

set /p choice="Enter choice [1-4] (default=1): "
if "%choice%"=="" set choice=1

if "%choice%"=="1" (
    echo.
    echo Launching JavaFX desktop interface...
    call "%MVN_CMD%" javafx:run
) else if "%choice%"=="2" (
    echo.
    echo Executing test suite...
    call "%MVN_CMD%" test
) else if "%choice%"=="3" (
    echo.
    echo Cleaning and recompiling...
    call "%MVN_CMD%" clean compile
) else (
    echo Exiting.
    exit /b 0
)

pause
