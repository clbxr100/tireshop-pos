@echo off
echo Simple Tire Shop POS Runner
echo ==========================
echo.

set WORK_DIR=%CD%
echo Working directory: %WORK_DIR%

REM Create target directories
if not exist "target" mkdir target
if not exist "target\classes" mkdir target\classes

REM Find Java
echo Looking for Java...
where java
if %ERRORLEVEL% EQU 0 (
    set JAVA_CMD=java
    set JAVAC_CMD=javac
) else (
    echo Java not found in PATH. Looking in common locations...
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot\bin\java.exe" (
        set JAVA_CMD="C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot\bin\java.exe"
        set JAVAC_CMD="C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot\bin\javac.exe"
        echo Found Java in Eclipse Adoptium
    ) else (
        echo Java not found! Please install Java and try again.
        pause
        exit /b 1
    )
)

echo Using Java: %JAVA_CMD%
echo Using Javac: %JAVAC_CMD%

REM Compile core classes
echo Compiling Main class...
%JAVAC_CMD% -d target\classes src\main\java\com\tireshop\Main.java

REM Run the application
echo Running application...
%JAVA_CMD% -cp "target\classes" com.tireshop.Main

echo.
echo Application exited with code %ERRORLEVEL%
pause 