@echo off
echo ============================================
echo    Tire Shop POS - Client Mode
echo ============================================
echo.

REM Configuration - CHANGE THIS TO YOUR SERVER'S IP
set SERVER_IP=192.168.1.100

echo Checking connection to database server at %SERVER_IP%...
ping -n 1 %SERVER_IP% >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Cannot reach database server at %SERVER_IP%!
    echo.
    echo Please check:
    echo 1. The server computer is turned on
    echo 2. The database server is running
    echo 3. Both computers are on the same network
    echo 4. The SERVER_IP in this file is correct
    echo.
    pause
    exit /b 1
)

echo Connection successful!
echo.

REM Check if database.properties exists
if not exist database.properties (
    echo Creating database.properties file...
    (
        echo # Network database configuration
        echo db.url=jdbc:h2:tcp://%SERVER_IP%:9092/./tireshop
        echo db.user=sa
        echo db.password=
    ) > database.properties
    echo Configuration file created.
)

REM Start the TireRack service locally
echo Starting TireRack API Service...
start "TireRack Service" /D "%~dp0tirerack-service" cmd /k "node index.js"

REM Wait a moment for the service to start
timeout /t 3 /nobreak > nul

REM Check if the TireRack service is running
curl -s http://localhost:3001/health > nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: TireRack service may not have started properly!
    echo Please check the TireRack Service window for errors.
    echo.
    pause
)

echo Starting Tire Shop POS...
mvn javafx:run

REM When the POS application closes, offer to close the TireRack service
echo.
echo POS Application has closed.
echo.
echo Press any key to stop the TireRack service and exit...
pause > nul

REM Kill the TireRack service
taskkill /FI "WINDOWTITLE eq TireRack Service*" /F > nul 2>&1

echo.
echo All services stopped. Goodbye!
timeout /t 2 /nobreak > nul 