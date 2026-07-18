@echo off
echo Tire Shop POS System Installation
echo =================================
echo.
echo Final Installer Stage.
echo Current directory is: %CD%
set "INSTALL_DIR=C:\TireShopPOS"
echo INSTALL_DIR is set to: %INSTALL_DIR%
echo.

if not exist "src\main\java" (
    echo CRITICAL ERROR: src\main\java not found at %CD%. This script must be run from the tireshop-deploy directory.
    pause
    exit /b 1
)
echo Initial check for 'src\main\java' passed.
echo.

echo --- Preparing Installation Directory ---
echo Target Installation Directory: %INSTALL_DIR%
echo Deleting previous installation (if any)...
if exist "%INSTALL_DIR%" (
    echo Deleting %INSTALL_DIR%...
    rmdir /s /q "%INSTALL_DIR%"
    if errorlevel 1 (
        echo WARN: rmdir failed.
        del /s /q "%INSTALL_DIR%\*.*" >nul 2>&1
        rmdir /s /q "%INSTALL_DIR%" >nul 2>&1
    )
    if exist "%INSTALL_DIR%" (
        echo WARN: Could not fully delete old %INSTALL_DIR%.
    ) else (
        echo Old %INSTALL_DIR% deleted.
    )
) else (
    echo Old %INSTALL_DIR% does not exist.
)
echo.

echo Creating directory structure:
mkdir "%INSTALL_DIR%" 2>nul
mkdir "%INSTALL_DIR%\src" 2>nul
mkdir "%INSTALL_DIR%\lib" 2>nul
if not exist "%INSTALL_DIR%\lib" (
    echo ERROR: Failed to create %INSTALL_DIR%\lib.
    pause
    exit /b 1
)
echo Dir structure created.
echo.

echo --- Copying Application Files ---
xcopy "src" "%INSTALL_DIR%\src\" /E /I /Y /Q
if errorlevel 1 (
    echo ERROR: Failed to copy src.
    pause
    exit /b 1
)
dir "%INSTALL_DIR%\src\main\java\com\tireshop" /b >nul 2>nul
if errorlevel 1 (
    echo ERROR: src verification failed.
    pause
    exit /b 1
)
echo Src files copied and verified.
echo.

if exist "%CD%\lib" (
    xcopy "%CD%\lib" "%INSTALL_DIR%\lib\" /E /I /Y /Q
    echo Lib dir copied.
) else (
    echo No local lib dir.
)
echo.

copy "*.bat" "%INSTALL_DIR%\" /Y >nul
if exist "README.md" copy "README.md" "%INSTALL_DIR%\" /Y >nul
echo Utility scripts copied.
echo.

REM Create the actual run-tireshop.bat from the template
if not exist "%INSTALL_DIR%\run-tireshop.template.bat" (
    echo ERROR: Template file missing in install dir.
    pause
    exit /b 1
)
echo Template file found.
echo.

copy "%INSTALL_DIR%\run-tireshop.template.bat" "%INSTALL_DIR%\run-tireshop.bat" /Y >nul
if not exist "%INSTALL_DIR%\run-tireshop.bat" (
    echo ERROR: Failed to create run.bat from template.
    pause
    exit /b 1
)
echo run-tireshop.bat created.
echo.

if exist "%CD%\tireshopdb.mv.db" (
    copy "%CD%\tireshopdb.mv.db" "%INSTALL_DIR%\" /Y >nul
    echo DB file copied.
) else (
    echo No local DB file.
)
echo.

REM Create desktop shortcut
echo --- Creating Desktop Shortcut ---
set VBS_SCRIPT="%TEMP%\createshortcut_%RANDOM%.vbs"
(
    echo Set oWS = WScript.CreateObject("WScript.Shell")
    echo sDesktopPath = oWS.SpecialFolders("Desktop")
    echo sLinkFile = sDesktopPath ^& "\Tire Shop POS.lnk"
    echo Set oLink = oWS.CreateShortcut(sLinkFile)
    echo oLink.TargetPath = "%INSTALL_DIR%\run-tireshop.bat"
    echo oLink.WorkingDirectory = "%INSTALL_DIR%"
    echo oLink.IconLocation = "" REM Placeholder for actual icon path
    echo oLink.Description = "Tire Shop POS System"
    echo oLink.Save
    echo WScript.Echo "Shortcut target: " ^& oLink.TargetPath
) > %VBS_SCRIPT%

echo Executing VBScript to create shortcut: %VBS_SCRIPT%
cscript //nologo %VBS_SCRIPT%
if exist %VBS_SCRIPT% del %VBS_SCRIPT% >nul 2>&1
echo Desktop shortcut creation process completed.
echo.

echo --- Installation Finalized ---
echo Installation completed successfully!
echo.
echo NEXT STEPS:
echo 1. Open a new Command Prompt or navigate in this one.
echo 2. Go to the installation directory: cd %INSTALL_DIR%
echo 3. Run the application: .\run-tireshop.bat
echo.
echo IMPORTANT: The run-tireshop.bat script will attempt to download
echo necessary JavaFX and other dependencies if they are not found in the 'lib' folder.
echo Make sure all helper .bat scripts (download-dependencies.bat, etc.) are in %INSTALL_DIR%.

echo For multi-PC setup or other details, refer to README.md in %INSTALL_DIR%.
echo.
pause 