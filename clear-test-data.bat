@echo off
echo ============================================
echo    Clear Test Data - Tire Shop POS
echo ============================================
echo.
echo WARNING: This will delete ALL test data including:
echo - Test customers
echo - Test sales transactions
echo - Test appointments
echo - Test employees (except admin)
echo.
echo Inventory items will be preserved.
echo.
echo THIS ACTION CANNOT BE UNDONE!
echo.

set /p confirm="Are you sure you want to clear all test data? (type YES to confirm): "

if /i not "%confirm%"=="YES" (
    echo.
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo Creating backup before clearing data...
call backup-now.bat

echo.
echo Clearing test data...
echo [This feature requires implementation in the application]
echo.
echo For now, please manually clear test data through the application.
echo.
pause 