@echo off
echo Starting Tire Shop POS System...
echo ================================

:: Set up local Java and Maven paths
set "JAVA_HOME=%~dp0jdk-21.0.9+10"
set "MAVEN_HOME=%~dp0apache-maven-3.9.6"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

:: Check if this computer should run the print server
set ENABLE_PRINT_SERVER=false

if exist "enable-print-server.txt" (
    set ENABLE_PRINT_SERVER=true
    echo Print server enabled for this computer
    goto START_PRINT_SERVER
) else (
    echo Print server disabled (create 'enable-print-server.txt' to enable)
    goto SKIP_PRINT_SERVER
)

:START_PRINT_SERVER
echo.
echo Starting Print Server...
echo This computer will handle print jobs from client computers

:: Check if Java compiler is available
javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: Java compiler not found - cannot start print server
    echo Print server will be skipped
    echo.
    goto SKIP_PRINT_SERVER
)

:: Compile and start print server in background
if not exist "temp-classes" mkdir temp-classes
javac -d temp-classes src\main\java\com\tireshop\util\SimplePrintServer.java

if %errorlevel% equ 0 (
    echo Print server compiled successfully
    start "Print Server" /MIN java -cp temp-classes com.tireshop.util.SimplePrintServer
    echo Print server started in background (minimized window)
    echo Client computers can now send print jobs to this computer
    goto PRINT_SERVER_DONE
)

echo ERROR: Failed to compile print server
echo Print jobs will not work from remote computers

:PRINT_SERVER_DONE
echo.
goto CONTINUE_STARTUP

:SKIP_PRINT_SERVER
echo.
goto CONTINUE_STARTUP

:CONTINUE_STARTUP
:: Start the TireRack service in a new window
echo Starting TireRack API Service...
start "TireRack Service" /D "%~dp0tirerack-service" cmd /k "node index.js"

:: Wait a moment for the service to start
timeout /t 3 /nobreak > nul

:: Check if the TireRack service is running
curl -s http://localhost:3001/health > nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: TireRack service may not have started properly!
    echo Please check the TireRack Service window for errors.
    echo.
    pause
)

:: Start the main POS application
echo Starting POS Application...
cd /d "%~dp0"
call mvn javafx:run

:: When the POS application closes, offer to close services
echo.
echo POS Application has closed.
echo.
echo Press any key to stop all services and exit...
pause > nul

:: Kill the TireRack service
taskkill /FI "WINDOWTITLE eq TireRack Service*" /F > nul 2>&1

:: Kill the Print Server if it was started
if "%ENABLE_PRINT_SERVER%"=="true" (
    taskkill /FI "WINDOWTITLE eq Print Server*" /F > nul 2>&1
    echo Print server stopped.
)

echo.
echo All services stopped. Goodbye!
timeout /t 2 /nobreak > nul 