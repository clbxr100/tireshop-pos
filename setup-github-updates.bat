@echo off
echo.
echo =====================================
echo   GitHub Auto-Update Setup
echo =====================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed
    echo Please install Python 3.7+ from https://www.python.org/downloads/
    echo Make sure to check "Add Python to PATH" during installation
    pause
    exit /b 1
)

REM Install required Python packages
echo Installing required Python packages...
pip install requests >nul 2>&1
if errorlevel 1 (
    echo ERROR: Failed to install requests package
    echo Please run: pip install requests
    pause
    exit /b 1
)

REM Create config file if it doesn't exist
if not exist "updater-config.json" (
    echo Creating configuration file...
    python github-updater.py --setup
    echo.
    echo Configuration file created: updater-config.json
    echo.
    echo IMPORTANT: Please edit updater-config.json and set:
    echo   - Your GitHub username in "owner"
    echo   - Your repository name in "repo"
    echo   - Your GitHub token if repo is private
    echo.
    echo Example configuration:
    echo {
    echo   "github": {
    echo     "owner": "your-username",
    echo     "repo": "tireshop-pos",
    echo     "branch": "main",
    echo     "token": ""
    echo   }
    echo }
    echo.
    pause
    notepad updater-config.json
)

echo.
echo Setup complete! You can now use:
echo   - check-updates.bat     - Check for updates
echo   - update-now.bat        - Update immediately
echo   - start-auto-updates.bat - Start continuous monitoring
echo.
pause 