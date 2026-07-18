@echo off
echo ============================================
echo    Stopping H2 Database Server
echo ============================================
echo.

REM Check if H2 server is running
netstat -an | findstr :9092 | findstr LISTENING >nul 2>&1
if %errorlevel% neq 0 (
    echo H2 Database Server is not running.
    pause
    exit /b 0
)

echo Stopping H2 Database Server...

REM Find and kill the Java process using port 9092
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9092 ^| findstr LISTENING') do (
    echo Stopping process ID: %%a
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel% equ 0 (
        echo H2 Database Server stopped successfully.
    ) else (
        echo Failed to stop H2 Database Server.
        echo You may need to run this as Administrator.
    )
)

echo.
pause 