@echo off
echo ====================================
echo    Tire Shop POS System Launcher
echo ====================================
echo.

:: Start the TireRack service in a new window
echo [1/2] Starting TireRack API Service...
start "TireRack Service" /D "%~dp0tirerack-service" cmd /k "node index.js"

:: Wait for the service to start
echo      Waiting for service to initialize...
timeout /t 3 /nobreak > nul

:: Start the main POS application
echo [2/2] Starting POS Application...
echo.
cd /d "%~dp0"
mvn javafx:run

:: When the POS application closes
echo.
echo ====================================
echo POS Application has been closed.
echo.
echo The TireRack service is still running in the background.
echo Close the "TireRack Service" window manually when done.
echo ====================================
pause 