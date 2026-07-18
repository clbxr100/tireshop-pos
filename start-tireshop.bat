@echo off
title Tire Shop POS System
echo ============================================
echo    Tire Shop POS - Complete Startup
echo ============================================
echo.

:: Set up local Java and Maven paths
set "JAVA_HOME=%~dp0jdk-21.0.9+10"
set "MAVEN_HOME=%~dp0apache-maven-3.9.6"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

:: Verify Java exists
if not exist "%JAVA_EXE%" (
    echo ERROR: Java not found at %JAVA_EXE%
    echo Please ensure JDK is installed in the project folder.
    pause
    exit /b 1
)

echo [1/4] Starting Database Server...
echo ----------------------------------------

:: Check if H2 server is already running
netstat -an | findstr :9092 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo Database Server already running on port 9092
    goto DATABASE_READY
)

:: Find H2 JAR
set "H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\2.1.214\h2-2.1.214.jar"

if not exist "%H2_JAR%" (
    echo ERROR: H2 JAR not found. Running Maven to download dependencies...
    call mvn dependency:resolve -q
)

if exist "%H2_JAR%" (
    :: Start H2 in background (minimized)
    start "Database Server" /MIN "%JAVA_EXE%" -cp "%H2_JAR%" org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort 9092 -baseDir . -ifNotExists
    echo Database Server started in background
    timeout /t 2 /nobreak > nul
) else (
    echo WARNING: Could not start database server. Application may not work correctly.
)

:DATABASE_READY
echo.

:: Check if this computer should run the print server
set ENABLE_PRINT_SERVER=false

echo [2/4] Checking Print Server...
echo ----------------------------------------
if exist "enable-print-server.txt" (
    set ENABLE_PRINT_SERVER=true
    echo Print server enabled for this computer

    :: Check if Java compiler is available
    javac -version >nul 2>&1
    if %errorlevel% neq 0 (
        echo WARNING: Java compiler not found - print server skipped
        goto SKIP_PRINT_SERVER
    )

    :: Compile and start print server in background
    if not exist "temp-classes" mkdir temp-classes
    javac -d temp-classes src\main\java\com\tireshop\util\SimplePrintServer.java 2>nul

    if %errorlevel% equ 0 (
        start "Print Server" /MIN java -cp temp-classes com.tireshop.util.SimplePrintServer
        echo Print server started in background
    ) else (
        echo WARNING: Failed to compile print server
    )
) else (
    echo Print server disabled (create 'enable-print-server.txt' to enable)
)

:SKIP_PRINT_SERVER
echo.

echo [3/4] Starting TireRack API Service...
echo ----------------------------------------
:: Start the TireRack service in a new minimized window
if exist "%~dp0tirerack-service\index.js" (
    start "TireRack Service" /MIN /D "%~dp0tirerack-service" cmd /c "node index.js"
    echo TireRack API Service started in background
    timeout /t 3 /nobreak > nul

    :: Check if the TireRack service is running
    curl -s http://localhost:3001/health > nul 2>&1
    if %errorlevel% neq 0 (
        echo WARNING: TireRack service may not be responding
    ) else (
        echo TireRack service is healthy
    )
) else (
    echo TireRack service not found - skipping
)
echo.

echo [4/4] Starting POS Application...
echo ----------------------------------------
echo.
cd /d "%~dp0"
call mvn javafx:run -q

:: When the POS application closes, cleanup
echo.
echo ============================================
echo    POS Application Closed - Cleaning Up
echo ============================================
echo.

:: Kill the Database Server
echo Stopping Database Server...
taskkill /FI "WINDOWTITLE eq Database Server*" /F > nul 2>&1

:: Kill the TireRack service
echo Stopping TireRack Service...
taskkill /FI "WINDOWTITLE eq TireRack Service*" /F > nul 2>&1

:: Kill the Print Server if it was started
if "%ENABLE_PRINT_SERVER%"=="true" (
    echo Stopping Print Server...
    taskkill /FI "WINDOWTITLE eq Print Server*" /F > nul 2>&1
)

echo.
echo All services stopped. Goodbye!
timeout /t 2 /nobreak > nul
