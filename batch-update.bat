@echo off
echo Starting Batch Update Tool...
echo.

REM Check if TireRack service is running
echo Checking TireRack service...
curl -s http://localhost:3001/health >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: TireRack service is not running on port 3001
    echo The batch update tool will work but won't be able to fetch tire data from TireRack.
    echo.
    echo To start the TireRack service, run: cd tirerack-service ^&^& npm start
    echo.
    pause
)

REM Run the batch update tool
java -cp "target/tireshop-1.0-SNAPSHOT.jar;lib/*" com.tireshop.util.BatchUpdateTool

pause 