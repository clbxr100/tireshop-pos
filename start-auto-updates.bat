@echo off
echo.
echo =====================================
echo   Starting Auto-Update Service
echo =====================================
echo.

echo This will continuously monitor for GitHub updates
echo and automatically update and restart the application
echo.
echo Press Ctrl+C to stop the service
echo.
pause

python github-updater.py --service 