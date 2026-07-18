@echo off
setlocal enabledelayedexpansion
title Build Tire Shop POS Installer
echo ============================================
echo    Tire Shop POS - Portable Package Builder
echo ============================================
echo.

:: Set up bundled Java and Maven
set "JAVA_HOME=%~dp0jdk-21.0.9+10"
set "PATH=%JAVA_HOME%\bin;%~dp0apache-maven-3.9.6\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo ERROR: Bundled JDK not found at %JAVA_HOME%
    pause
    exit /b 1
)

:: 1. Build the shaded jar
echo [1/4] Building application (mvn clean package)...
call mvn clean package -q -DskipTests
if errorlevel 1 (
    echo Build failed!
    pause
    exit /b 1
)

:: Find the shaded jar (excludes original-*.jar)
set "SHADED_JAR="
for %%f in ("target\tire-shop-pos-*.jar") do set "SHADED_JAR=%%~nxf"
if not defined SHADED_JAR (
    echo ERROR: Shaded jar not found in target\
    pause
    exit /b 1
)

:: Extract version from jar name: tire-shop-pos-X.Y.Z.jar -> X.Y.Z
set "VERSION=%SHADED_JAR:tire-shop-pos-=%"
set "VERSION=%VERSION:.jar=%"
echo Built %SHADED_JAR% (version %VERSION%)

:: 2. Prepare jpackage input
echo [2/4] Preparing jpackage input...
if exist "target\jpackage-input" rmdir /s /q "target\jpackage-input"
mkdir "target\jpackage-input"
copy "target\%SHADED_JAR%" "target\jpackage-input\" > nul

:: 3. Create the app image
echo [3/4] Creating app image (jpackage)...
if exist "target\installer" rmdir /s /q "target\installer"
"%JAVA_HOME%\bin\jpackage.exe" --type app-image --name TireShopPOS ^
    --app-version %VERSION% ^
    --input "target\jpackage-input" --main-jar "%SHADED_JAR%" ^
    --main-class com.tireshop.Launcher ^
    --dest "target\installer" ^
    --vendor "Tire Shop" --description "Tire Shop Point of Sale System" ^
    --java-options "-Xms128m" --java-options "-Xmx512m"
if errorlevel 1 (
    echo jpackage failed!
    pause
    exit /b 1
)

:: 4. Add external database config and create the zip
echo [4/4] Adding config and creating zip...
copy "src\main\resources\database.properties" "target\installer\TireShopPOS\database.properties" > nul

set "ZIP_NAME=TireShopPOS-%VERSION%-portable.zip"
if exist "%ZIP_NAME%" del "%ZIP_NAME%"
powershell -NoProfile -Command "Compress-Archive -Path 'target\installer\TireShopPOS' -DestinationPath '%ZIP_NAME%' -CompressionLevel Optimal"
if errorlevel 1 (
    echo Zip creation failed!
    pause
    exit /b 1
)

echo.
echo ============================================
echo    DONE! Created %ZIP_NAME%
echo ============================================
echo.
echo To install on a computer:
echo   1. Extract the zip anywhere (e.g. Desktop or C:\TireShopPOS)
echo   2. Double-click TireShopPOS.exe inside the extracted folder
echo.
echo The database (tireshop_db.mv.db) and config.properties are
echo created automatically next to the exe on first run.
echo Edit database.properties first if connecting to a shared DB.
echo.
pause
