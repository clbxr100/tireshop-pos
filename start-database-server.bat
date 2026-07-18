@echo off
echo ============================================
echo    Tire Shop POS - Database Server
echo ============================================
echo.

REM Set up local Java path
set "JAVA_HOME=%~dp0jdk-21.0.9+10"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if not exist "%JAVA_EXE%" (
    echo ERROR: Java not found at %JAVA_EXE%
    pause
    exit /b 1
)

REM Check if H2 server is already running
netstat -an | findstr :9092 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo H2 Database Server is already running on port 9092!
    echo.
    echo If you need to restart it:
    echo 1. Close this window
    echo 2. Run stop-database-server.bat
    echo 3. Run this script again
    echo.
    pause
    exit /b 0
)

echo Starting H2 Database Server...
echo This window must remain open while the POS is in use.
echo.
echo Server will listen on port 9092
echo Other computers can connect using this computer's IP address
echo.

REM Start H2 in server mode using H2 from Maven repository
set "H2_JAR=%USERPROFILE%\.m2\repository\com\h2database\h2\2.1.214\h2-2.1.214.jar"

if not exist "%H2_JAR%" (
    echo ERROR: H2 JAR not found at %H2_JAR%
    echo Please run 'mvn compile' first to download dependencies.
    pause
    exit /b 1
)

"%JAVA_EXE%" -cp "%H2_JAR%" org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort 9092 -baseDir . -ifNotExists

pause