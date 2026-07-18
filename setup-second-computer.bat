@echo off
echo.
echo =====================================
echo   Second Computer Setup
echo =====================================
echo.

echo This script will help set up the second computer for the POS system
echo.

REM Create directories
echo Creating directories...
mkdir backups 2>nul
mkdir temp 2>nul
mkdir logs 2>nul

REM Check if Java is installed
echo Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed
    echo Please install Java 11+ from https://adoptium.net/
    pause
    exit /b 1
)

REM Test database connection
echo.
echo Testing database connection to main computer...
set /p MAIN_IP="Enter the main computer's IP address (default: 192.168.1.100): "
if "%MAIN_IP%"=="" set MAIN_IP=192.168.1.100

echo Testing connection to %MAIN_IP%:9092...
telnet %MAIN_IP% 9092 >nul 2>&1
if errorlevel 1 (
    echo WARNING: Cannot connect to database server
    echo Please ensure:
    echo   1. Main computer is running and POS application is started
    echo   2. Windows Firewall on main computer allows port 9092
    echo   3. Both computers are on the same network
    echo.
    echo You can continue setup, but test the connection later
    pause
)

REM Create database.properties file
echo Creating database configuration...
(
echo # Network database configuration - connects to main computer
echo db.url=jdbc:h2:tcp://%MAIN_IP%:9092/./tireshop
echo db.user=sa
echo db.password=
) > database.properties

echo Database configuration created: database.properties
echo.

REM Create run script
echo Creating run script...
(
echo @echo off
echo echo Starting Tire Shop POS ^(Client Mode^)...
echo echo Connecting to database server at %MAIN_IP%:9092
echo echo.
echo java -jar tireshop-pos.jar
echo pause
) > run-pos.bat

echo Run script created: run-pos.bat
echo.

REM Check if main JAR file exists
if not exist "tireshop-pos.jar" (
    echo WARNING: tireshop-pos.jar not found
    echo Please copy the JAR file from the main computer or use the update system
    echo.
)

echo.
echo =====================================
echo   Setup Complete!
echo =====================================
echo.
echo Next steps:
echo   1. Copy tireshop-pos.jar from main computer (if not already done)
echo   2. Run setup-github-updates.bat to configure automatic updates
echo   3. Double-click run-pos.bat to start the application
echo.
echo The application will connect to the database on %MAIN_IP%
echo Both computers will share the same data in real-time
echo.
pause 