@echo off
echo.
echo =====================================
echo   Install Auto-Update Service
echo =====================================
echo.

REM Check if running as administrator
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: This script must be run as Administrator
    echo Right-click and select "Run as administrator"
    pause
    exit /b 1
)

REM Create service wrapper script
echo Creating service wrapper...
(
echo @echo off
echo cd /d "%~dp0"
echo python github-updater.py --service
) > update-service.bat

REM Install as Windows service using sc command
echo Installing Windows service...
sc create TireShopUpdater binPath= "\"%CD%\update-service.bat\"" start= auto DisplayName= "Tire Shop Auto-Updater"

if %errorlevel% equ 0 (
    echo.
    echo ✅ Service installed successfully!
    echo.
    echo The service will automatically start when Windows boots
    echo.
    echo To manage the service:
    echo   - Start: sc start TireShopUpdater
    echo   - Stop:  sc stop TireShopUpdater
    echo   - Remove: sc delete TireShopUpdater
    echo.
    echo Or use Windows Services manager (services.msc)
    echo.
) else (
    echo.
    echo ❌ Failed to install service
    echo Please check that you're running as Administrator
    echo.
)

pause 