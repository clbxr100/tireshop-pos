@echo off
echo ========================================
echo    Simple Print Server (Java 8 Compatible)
echo ========================================
echo.
echo This script compiles and runs a simple print server
echo that works with Java 8 or higher.
echo.

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 8 or later
    pause
    exit /b 1
)

REM Check if javac is available
javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java compiler (javac) is not available
    echo Please install Java JDK (not just JRE)
    pause
    exit /b 1
)

echo Checking Java version...
java -version

echo.
echo Compiling SimplePrintServer.java...

REM Create temp directory for compiled classes
if not exist "temp-classes" mkdir temp-classes

REM Compile the Java file
javac -d temp-classes src\main\java\com\tireshop\util\SimplePrintServer.java

if %errorlevel% neq 0 (
    echo ERROR: Failed to compile SimplePrintServer.java
    echo Make sure the file exists and there are no syntax errors
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Starting Simple Print Server...
echo.
echo The server will listen on port 8080
echo Client computers can now send print jobs to this computer
echo.
echo To test: Open browser and go to http://localhost:8080/status
echo.
echo Press Ctrl+C to stop the server
echo.

REM Start the print server
java -cp temp-classes com.tireshop.util.SimplePrintServer

echo.
echo Print server stopped.
pause 