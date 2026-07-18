@echo off
echo Verifying JavaFX Installation
echo ============================
echo.

if not exist "lib" (
    echo ERROR: lib directory not found!
    echo Please run download-javafx.bat first.
    pause
    exit /b 1
)

echo Checking for JavaFX JAR files:
echo -----------------------------
set JAR_COUNT=0
for %%F in (lib\javafx*.jar) do (
    echo Found: %%F
    set /a JAR_COUNT+=1
)

echo.
echo Found %JAR_COUNT% JavaFX JAR files.
if %JAR_COUNT% LSS 8 (
    echo WARNING: Expected at least 8 JavaFX JAR files, but only found %JAR_COUNT%.
    echo This might indicate an incomplete installation.
)

echo.
echo Checking for native DLL files:
echo ----------------------------
set DLL_COUNT=0
for %%F in (lib\*.dll) do (
    echo Found: %%F
    set /a DLL_COUNT+=1
)

echo.
echo Found %DLL_COUNT% DLL files.
if %DLL_COUNT% EQU 0 (
    echo WARNING: No DLL files found! JavaFX applications may not run correctly.
    echo Please run the updated download-javafx.bat script again.
) else (
    echo Native libraries appear to be properly installed.
)

echo.
echo Verification complete.
echo.
pause 