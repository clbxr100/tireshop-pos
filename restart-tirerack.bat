@echo off
echo Restarting TireRack service...

REM Kill any existing node processes
taskkill /F /IM node.exe 2>nul

REM Wait a moment
timeout /t 2 /nobreak >nul

REM Start the service
cd tirerack-service
start "TireRack Service" cmd /k "node index.js"

echo TireRack service restarted!
pause 