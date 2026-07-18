@echo off
echo Downloading and bundling JavaFX libraries
echo =======================================
echo.

REM Create lib directory if it doesn't exist
if not exist "lib" mkdir lib

REM Set JavaFX version and download URL
set JAVAFX_VERSION=17.0.8
set JAVAFX_URL=https://download2.gluonhq.com/openjfx/%JAVAFX_VERSION%/openjfx-%JAVAFX_VERSION%_windows-x64_bin-sdk.zip
set JAVAFX_FILE=javafx.zip

echo Downloading JavaFX %JAVAFX_VERSION% SDK...
echo From: %JAVAFX_URL%
curl -L -o %JAVAFX_FILE% %JAVAFX_URL%

if not exist %JAVAFX_FILE% (
    echo Failed to download JavaFX SDK.
    echo Please check your internet connection and try again.
    echo You can also download it manually from https://gluonhq.com/products/javafx/
    pause
    exit /b 1
)

echo Download complete.
echo Extracting JavaFX libraries...

REM Create temp directory
if not exist "temp-javafx" mkdir temp-javafx

REM Extract files
powershell -command "Expand-Archive -Path '%JAVAFX_FILE%' -DestinationPath 'temp-javafx' -Force"

echo.
echo Copying JavaFX libraries to lib directory...
copy "temp-javafx\javafx-sdk-%JAVAFX_VERSION%\lib\javafx*.jar" "lib\"

echo.
echo Copying JavaFX native libraries (DLL files)...

REM List files in directories to help with diagnostics
echo.
echo Checking for DLL files in possible locations:
echo -------------------------------------------

REM Check for files in the primary bin directory (this is the correct location)
echo Checking temp-javafx\javafx-sdk-%JAVAFX_VERSION%\bin:
dir /b "temp-javafx\javafx-sdk-%JAVAFX_VERSION%\bin\*.dll" 2>nul
if exist "temp-javafx\javafx-sdk-%JAVAFX_VERSION%\bin\*.dll" (
    echo Found DLLs in bin directory - copying...
    copy "temp-javafx\javafx-sdk-%JAVAFX_VERSION%\bin\*.dll" "lib\"
    echo DLL files copied successfully from bin directory.
)

echo.
echo Checking lib directory:
dir /b "temp-javafx\javafx-sdk-%JAVAFX_VERSION%\lib\*.dll" 2>nul
if exist "temp-javafx\javafx-sdk-%JAVAFX_VERSION%\lib\*.dll" (
    echo Found DLLs in lib directory - copying...
    copy "temp-javafx\javafx-sdk-%JAVAFX_VERSION%\lib\*.dll" "lib\"
)

echo.
echo Contents of the extracted JavaFX package (for diagnostic purposes):
dir /s /b "temp-javafx\javafx-sdk-%JAVAFX_VERSION%\bin"

echo.
echo Cleaning up...
del %JAVAFX_FILE%
rmdir /s /q temp-javafx

echo.
echo JavaFX libraries have been successfully included in the project.
echo Running verification to confirm installation...
call verify-javafx.bat

echo.
pause 