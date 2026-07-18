@echo off
echo ============================================
echo    System Check - Tire Shop POS
echo ============================================
echo.

echo Checking system requirements...
echo.

REM Check Java
echo [1/5] Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo    [X] Java is not installed or not in PATH
    set error=1
) else (
    echo    [✓] Java is installed
)

REM Check Maven
echo [2/5] Checking Maven installation...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo    [X] Maven is not installed or not in PATH
    set error=1
) else (
    echo    [✓] Maven is installed
)

REM Check if built
echo [3/5] Checking if application is built...
if exist "target\tire-shop-pos-1.0-SNAPSHOT.jar" (
    echo    [✓] Application JAR found
) else (
    echo    [X] Application not built - run 'mvn package'
    set error=1
)

REM Check Node.js for TireRack service
echo [4/5] Checking Node.js installation...
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo    [!] Node.js not installed - TireRack service won't work
    echo        (Barcode lookups will be limited)
) else (
    echo    [✓] Node.js is installed
)

REM Check database
echo [5/5] Checking database...
if exist "tireshop_db.mv.db" (
    echo    [✓] Database file exists
) else (
    echo    [!] Database will be created on first run
)

echo.
echo Network Configuration:
echo ----------------------
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4 Address"') do (
    echo    IP Address:%%a
)

echo.
echo Port Status:
echo ------------
netstat -an | findstr :9092 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo    [✓] Database server is running on port 9092
) else (
    echo    [!] Database server is not running
)

netstat -an | findstr :3001 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo    [✓] TireRack service is running on port 3001
) else (
    echo    [!] TireRack service is not running
)

echo.
echo Backup Status:
echo --------------
if exist "backups" (
    echo    [✓] Backup folder exists
    dir backups\*.zip 2>nul | find "File(s)" >nul
    if %errorlevel% equ 0 (
        echo    [✓] Backups found
    ) else (
        echo    [!] No backups yet
    )
) else (
    echo    [!] Backup folder not created yet
)

echo.
if defined error (
    echo RESULT: Some issues found. Please fix before deployment.
) else (
    echo RESULT: System is ready for deployment!
)

echo.
pause 