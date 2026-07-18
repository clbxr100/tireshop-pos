@echo off
echo Testing JavaFX Installation
echo ==========================
echo.

REM Create a temporary directory
if not exist "temp-test" mkdir temp-test
cd temp-test

REM Create a simple JavaFX test application
echo package com.test; > TestFX.java
echo. >> TestFX.java
echo import javafx.application.Application; >> TestFX.java
echo import javafx.scene.Scene; >> TestFX.java
echo import javafx.scene.control.Label; >> TestFX.java
echo import javafx.stage.Stage; >> TestFX.java
echo. >> TestFX.java
echo public class TestFX extends Application { >> TestFX.java
echo     @Override >> TestFX.java
echo     public void start(Stage stage) { >> TestFX.java
echo         Label label = new Label("JavaFX is working!"); >> TestFX.java
echo         Scene scene = new Scene(label, 300, 100); >> TestFX.java
echo         stage.setScene(scene); >> TestFX.java
echo         stage.setTitle("JavaFX Test"); >> TestFX.java
echo         stage.show(); >> TestFX.java
echo     } >> TestFX.java
echo. >> TestFX.java
echo     public static void main(String[] args) { >> TestFX.java
echo         System.out.println("Starting JavaFX test application..."); >> TestFX.java
echo         launch(args); >> TestFX.java
echo     } >> TestFX.java
echo } >> TestFX.java

echo Compiling test application...
set JAVAFX_PATH=..\lib

REM Find Java
set JAVA_CMD=java
set JAVAC_CMD=javac

REM Use Java from PATH if available
where java >nul 2>&1
if not ERRORLEVEL 1 (
    echo Using Java from PATH
    set JAVA_CMD=java
    set JAVAC_CMD=javac
    goto :compile
)

REM Check common Java installations
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    echo Using Java from C:\Program Files\Java\jdk-17
    set JAVA_CMD="C:\Program Files\Java\jdk-17\bin\java.exe"
    set JAVAC_CMD="C:\Program Files\Java\jdk-17\bin\javac.exe"
    goto :compile
)

if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\bin\java.exe" (
    echo Using Java from C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot
    set JAVA_CMD="C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\bin\java.exe"
    set JAVAC_CMD="C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\bin\javac.exe"
    goto :compile
)

REM Try to find any Java installation
for /d %%i in ("C:\Program Files\Java\*") do (
    if exist "%%i\bin\java.exe" (
        echo Using Java from %%i
        set JAVA_CMD="%%i\bin\java.exe"
        set JAVAC_CMD="%%i\bin\javac.exe"
        goto :compile
    )
)

for /d %%i in ("C:\Program Files\Eclipse Adoptium\*") do (
    if exist "%%i\bin\java.exe" (
        echo Using Java from %%i
        set JAVA_CMD="%%i\bin\java.exe"
        set JAVAC_CMD="%%i\bin\javac.exe"
        goto :compile
    )
)

echo Java not found! Please install Java 11 or higher.
goto :cleanup

:compile
echo Checking for JavaFX libraries...
if not exist "..\lib\javafx.controls.jar" (
    echo JavaFX libraries not found in ..\lib directory!
    echo Please run download-javafx.bat first.
    goto :cleanup
)

echo Using JavaFX from %JAVAFX_PATH%
echo.

REM Create directories for the package structure
mkdir com\test

echo Compiling test application...
%JAVAC_CMD% -d . -cp ".;%JAVAFX_PATH%\*" --module-path "%JAVAFX_PATH%" --add-modules javafx.controls TestFX.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed! JavaFX may not be properly installed.
    goto :cleanup
)

REM Verify the class file exists
if not exist "com\test\TestFX.class" (
    echo Compiled class file not found! Check for compilation errors.
    goto :cleanup
)

echo Running JavaFX test application...
echo A window should appear saying "JavaFX is working!"
echo If no window appears, there may be an issue with the JavaFX installation.
echo.
echo Press Ctrl+C to exit the application when done testing.
echo.

REM Copy the DLL files to the current directory to ensure they're found
echo Copying native libraries for testing...
copy "..\lib\*.dll" "." 2>nul

REM Run with special flags to enable native access
%JAVA_CMD% --add-opens javafx.graphics/com.sun.glass.utils=ALL-UNNAMED --enable-native-access=ALL-UNNAMED -Dprism.forceGPU=false -Dprism.order=sw -cp ".;%JAVAFX_PATH%\*" --module-path "%JAVAFX_PATH%" --add-modules javafx.controls com.test.TestFX

:cleanup
cd ..
rmdir /s /q temp-test 2>nul
echo.
echo Test completed.
pause 