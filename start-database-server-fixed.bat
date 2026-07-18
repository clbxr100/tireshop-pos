@echo off
echo ============================================
echo    Tire Shop POS - Database Server (Fixed)
echo ============================================
echo.

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

REM First, make sure we have the H2 JAR
echo Checking for H2 database JAR...
if not exist "%USERPROFILE%\.m2\repository\com\h2database\h2\2.1.214\h2-2.1.214.jar" (
    echo Downloading H2 database dependency...
    mvn dependency:get -Dartifact=com.h2database:h2:2.1.214 -q
    if %errorlevel% neq 0 (
        echo Failed to download H2 database. Please check your internet connection.
        pause
        exit /b 1
    )
)

echo Starting H2 Server...
java -cp "%USERPROFILE%\.m2\repository\com\h2database\h2\2.1.214\h2-2.1.214.jar" org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort 9092 -baseDir . -ifNotExists

pause
