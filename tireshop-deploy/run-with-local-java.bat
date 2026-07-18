@echo off
echo Tire Shop POS - Running with local Java
echo ========================================
echo.

REM Set the path to our local Java installation
set JAVA_HOME=%~dp0..\jdk-21.0.9+10
set JAVA_CMD=%JAVA_HOME%\bin\java.exe
set JAVAC_CMD=%JAVA_HOME%\bin\javac.exe

echo Using Java from: %JAVA_HOME%
echo.

REM Verify Java is available
if not exist "%JAVA_CMD%" (
    echo ERROR: Java not found at %JAVA_CMD%
    echo Please make sure Java is installed correctly.
    pause
    exit /b 1
)

echo Java version:
"%JAVA_CMD%" -version
echo.

REM Check if target\classes exists
if not exist "target\classes" (
    echo Creating target\classes directory...
    mkdir target\classes
)

echo Compiling application...
"%JAVAC_CMD%" -d target\classes -cp "lib\*" src\main\java\com\tireshop\*.java src\main\java\com\tireshop\*\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo.
echo Starting Tire Shop POS...
echo.
"%JAVA_CMD%" -cp "target\classes;lib\*" com.tireshop.Main

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with error code %ERRORLEVEL%
)

echo.
pause
