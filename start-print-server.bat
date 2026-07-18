@echo off
echo ========================================
echo        TireShop POS Print Server
echo ========================================
echo.
echo This script starts the print server on the POS computer
echo to handle print requests from client computers.
echo.
echo Make sure this runs on the computer with the printer!
echo.

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 11 or later
    pause
    exit /b 1
)

REM Compile the Java files if needed
if not exist "target\classes" (
    echo Compiling Java files...
    call mvn compile
    if %errorlevel% neq 0 (
        echo ERROR: Failed to compile Java files
        pause
        exit /b 1
    )
)

echo Starting Print Server...
echo.
echo The print server will listen on port 8080
echo Client computers can now send print jobs to this computer
echo.
echo Press Ctrl+C to stop the server
echo.

REM Start the print server
java -cp "target/classes;target/dependency/*" com.tireshop.util.RemotePrintServer

pause 