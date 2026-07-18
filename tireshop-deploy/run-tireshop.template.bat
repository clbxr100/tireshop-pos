@echo off
setlocal EnableDelayedExpansion
echo Starting Tire Shop POS System...
echo.

REM Set up directories
set "INSTALL_DIR=%CD%"
set "TARGET_DIR=!INSTALL_DIR!\target\classes"
if not exist "!TARGET_DIR!" mkdir "!TARGET_DIR!"
echo.

REM Check for dependencies
set DEPS_COUNT=0
for %%F in ("!INSTALL_DIR!\lib\hibernate-core*.jar") do set /a DEPS_COUNT+=1
if !DEPS_COUNT! EQU 0 (
    echo WARNING: Required application dependencies not found in lib directory!
    echo You need to run download-dependencies.bat first.
    echo.
    if exist "!INSTALL_DIR!\download-dependencies.bat" (
        echo Running download-dependencies.bat now...
        call "!INSTALL_DIR!\download-dependencies.bat"
        REM Re-check after download attempt
        set DEPS_COUNT=0
        for %%F in ("!INSTALL_DIR!\lib\hibernate-core*.jar") do set /a DEPS_COUNT+=1
        if !DEPS_COUNT! EQU 0 (
            echo Still no core dependencies found after running download-dependencies.bat.
            echo Please check download-dependencies.bat output and lib folder.
            pause
            exit /b 1
        )
    ) else (
        echo download-dependencies.bat not found in !INSTALL_DIR!!
        echo Please copy it from the source and run it, or download dependencies manually.
        pause
        exit /b 1
    )
)
echo.

REM Find Java
set JAVA_CMD=java
set JAVAC_CMD=javac
set JAVA_FOUND_PATH=
set JAVAC_FOUND_PATH=

echo Looking for Java installation...
echo.

REM First, check if java is in PATH
where java >nul 2>&1
if not !ERRORLEVEL! EQU 0 goto check_common_java_locations
where javac >nul 2>&1
if not !ERRORLEVEL! EQU 0 goto check_common_java_locations
echo Found Java and Javac in PATH
set JAVA_FOUND_PATH=java
set JAVAC_FOUND_PATH=javac
goto java_located

:check_common_java_locations
echo Checking common installation directories...
set "JDK_PATHS_TO_CHECK="C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot" "C:\Program Files\Java\jdk-17" "C:\Program Files\Eclipse Adoptium\jdk-17" "C:\Program Files\Java\jdk-24" "C:\Program Files\Java\jdk-11" "C:\Program Files\Eclipse Adoptium\jdk-11" "C:\Program Files\Java\jdk" "C:\Program Files\Eclipse Adoptium\jdk""

for %%P in (!JDK_PATHS_TO_CHECK!) do (
    set "CURRENT_JDK_PATH=%%~P"
    if exist "!CURRENT_JDK_PATH!\bin\java.exe" (
        if exist "!CURRENT_JDK_PATH!\bin\javac.exe" (
            echo Found Java and Javac in !CURRENT_JDK_PATH!
            set "JAVA_CMD=!CURRENT_JDK_PATH!\bin\java.exe"
            set "JAVAC_CMD=!CURRENT_JDK_PATH!\bin\javac.exe"
            set JAVA_FOUND_PATH="!JAVA_CMD!"
            set JAVAC_FOUND_PATH="!JAVAC_CMD!"
            goto java_located
        )
    )
)

echo Looking for any Java installation in Program Files more broadly...
for /d %%I in ("C:\Program Files\Java\*") do (
    if exist "%%I\bin\java.exe" (
        if exist "%%I\bin\javac.exe" (
            echo Found Java and Javac in %%I
            set "JAVA_CMD=%%I\bin\java.exe"
            set "JAVAC_CMD=%%I\bin\javac.exe"
            set JAVA_FOUND_PATH="!JAVA_CMD!"
            set JAVAC_FOUND_PATH="!JAVAC_CMD!"
            goto java_located
        )
    )
)
for /d %%I in ("C:\Program Files\Eclipse Adoptium\*") do (
    if exist "%%I\bin\java.exe" (
        if exist "%%I\bin\javac.exe" (
            echo Found Java and Javac in %%I
            set "JAVA_CMD=%%I\bin\java.exe"
            set "JAVAC_CMD=%%I\bin\javac.exe"
            set JAVA_FOUND_PATH="!JAVA_CMD!"
            set JAVAC_FOUND_PATH="!JAVAC_CMD!"
            goto java_located
        )
    )
)

:java_not_located
if not defined JAVA_FOUND_PATH (
    echo Java executables (java.exe and javac.exe) not found in PATH or common locations.
    echo.
    echo DIAGNOSTIC INFORMATION:
    echo ---------------------
    echo Checking for java.exe:
    where java
    echo.
    echo Checking for javac.exe:
    where javac
    echo.
    echo System Path:
    echo %PATH% 
    echo.
    echo Please install Java JDK 11 or higher (JDK 17 or 21 recommended) from https://adoptium.net/temurin/releases/
    echo Ensure both java.exe and javac.exe from the JDK's bin directory are in your system PATH,
    echo or install to a standard location like 'C:\Program Files\Java\jdk-XX'.
    pause
    exit /b 1
)

:java_located
echo Using Java: !JAVA_CMD!
echo Using Javac: !JAVAC_CMD!
echo.

REM Compile the source code
echo Compiling Java source files...
set SOURCES_LIST=

REM Change to the source directory to simplify for /R path
pushd "!INSTALL_DIR!\src\main\java"
if !ERRORLEVEL! EQU 0 (
    echo Searching for .java files in %CD%...
    for /R . %%f in (*.java) do set "SOURCES_LIST=!SOURCES_LIST! "%%f""
    popd
) else (
    echo ERROR: Could not change to directory !INSTALL_DIR!\src\main\java
    popd 2>nul
)

echo DEBUG: SOURCES_LIST is [!SOURCES_LIST!]

if defined SOURCES_LIST (
    "!JAVAC_CMD!" -encoding UTF-8 -d "!TARGET_DIR!" -cp "!INSTALL_DIR!\lib\*" !SOURCES_LIST!
    if !ERRORLEVEL! NEQ 0 (
        echo Compilation failed! Error code: !ERRORLEVEL!
        pause
        exit /b 1
    )
) else (
    echo No Java source files found to compile in !INSTALL_DIR!\src\main\java.
)
echo.

REM Copy resources
if exist "!INSTALL_DIR!\src\main\resources" (
    echo Copying resource files...
    xcopy /E /Y /I "!INSTALL_DIR!\src\main\resources" "!TARGET_DIR!" >nul
)
echo.

REM Check for JavaFX libraries
echo Checking for JavaFX libraries...
set JAVAFX_PRESENT=false
set "JAVAFX_PATH=!INSTALL_DIR!\lib"
if exist "!JAVAFX_PATH!\javafx.base.jar" if exist "!JAVAFX_PATH!\javafx.controls.jar" if exist "!JAVAFX_PATH!\javafx.fxml.jar" (
    echo Found bundled JavaFX in !JAVAFX_PATH!
    set JAVAFX_PRESENT=true
)
echo.

REM Count JAR files and DLL files for JavaFX
echo Checking JavaFX installation integrity...
set JAVAFX_JAR_COUNT=0
set JAVAFX_DLL_COUNT=0
for %%F in ("!JAVAFX_PATH!\javafx*.jar") do set /a JAVAFX_JAR_COUNT+=1
for %%F in ("!JAVAFX_PATH!\*.dll") do set /a JAVAFX_DLL_COUNT+=1
echo Found !JAVAFX_JAR_COUNT! JavaFX JAR files and !JAVAFX_DLL_COUNT! DLL files in !JAVAFX_PATH!.
if "!JAVAFX_JAR_COUNT!" LSS "8" echo WARNING: Expected at least 8 JavaFX JAR files.
if "!JAVAFX_DLL_COUNT!" EQU "0" echo WARNING: No DLL files found in !JAVAFX_PATH!. Native components may not work correctly.
echo.

if "!JAVAFX_PRESENT!"=="false" goto handle_javafx_missing
goto run_application

:handle_javafx_missing
echo ERROR: Essential JavaFX libraries (javafx.base.jar, javafx.controls.jar, javafx.fxml.jar) not found in !JAVAFX_PATH!!
echo Please run download-javafx.bat from the !INSTALL_DIR! directory.
echo.
if not exist "!INSTALL_DIR!\download-javafx.bat" goto download_javafx_script_missing
    echo Running download-javafx.bat now...
    call "!INSTALL_DIR!\download-javafx.bat"
    REM Re-check after download attempt
    if exist "!JAVAFX_PATH!\javafx.controls.jar" (
        set JAVAFX_PRESENT=true
        echo JavaFX downloaded. Please re-run run-tireshop.bat.
    ) else (
        echo JavaFX download failed or did not place files in !JAVAFX_PATH!.
    )
    goto end_javafx_handling
:download_javafx_script_missing
echo download-javafx.bat not found in !INSTALL_DIR!!
echo Please copy it from the source and run it, or download JavaFX manually.
:end_javafx_handling
pause
exit /b 1

:run_application
REM Run the application
echo Running application...
echo Application output will be logged to app.log
echo Using JavaFX from: !JAVAFX_PATH!
echo.

"!JAVA_CMD!" --add-opens javafx.graphics/com.sun.glass.utils=ALL-UNNAMED --enable-native-access=ALL-UNNAMED -Dprism.verbose=true -Djavafx.verbose=true -Dprism.allowSW=true -Dprism.forceGPU=false -Dprism.order=sw --module-path "!JAVAFX_PATH!" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "!TARGET_DIR!;!INSTALL_DIR!\lib\*" com.tireshop.Main > "!INSTALL_DIR!\app.log" 2>&1
set APP_EXIT_CODE=!ERRORLEVEL!
echo.
echo Application exited with code !APP_EXIT_CODE!.
echo.
echo Please check the file 'app.log' in this directory (!INSTALL_DIR!) for application messages and any errors.
echo.

if "!APP_EXIT_CODE!" NEQ "0" goto runApp_handleError
goto runApp_handleSuccess

:runApp_handleError
echo The application reported an error (exit code !APP_EXIT_CODE!).
echo Displaying the last 20 lines of app.log:
powershell -Command "if (Test-Path '!INSTALL_DIR!\app.log') { Get-Content '!INSTALL_DIR!\app.log' -Tail 20 } else { echo app.log not found. }"
goto runApp_afterLogDisplay

:runApp_handleSuccess
echo If the application window did not appear or closed unexpectedly, app.log may contain important diagnostic information.
goto runApp_afterLogDisplay

:runApp_afterLogDisplay
echo.
echo Press any key to exit...
pause
exit /b !APP_EXIT_CODE! 