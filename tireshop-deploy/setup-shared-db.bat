@echo off
echo Tire Shop POS - Shared Database Setup
echo ====================================
echo.
echo This script will configure your Tire Shop POS system to use a shared database
echo across multiple PCs.
echo.

REM Ask for shared database location
set /p SHARED_DB_PATH=Enter the UNC path for shared database (e.g. \\SERVER\Share\TireShopDB): 

REM Validate input
if "%SHARED_DB_PATH%"=="" (
    echo No path entered. Setup cancelled.
    pause
    exit /b 1
)

REM Create the shared directory if it doesn't exist
if not exist "%SHARED_DB_PATH%" (
    echo Creating shared directory...
    mkdir "%SHARED_DB_PATH%"
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to create shared directory.
        echo Please make sure the network path is accessible and you have write permissions.
        pause
        exit /b 1
    )
)

REM Check if we have an existing database to copy
set INSTALL_DIR=C:\TireShopPOS
if exist "%INSTALL_DIR%\tireshopdb.mv.db" (
    echo Copying existing database to shared location...
    copy "%INSTALL_DIR%\tireshopdb.mv.db" "%SHARED_DB_PATH%\"
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to copy database file.
        echo Please make sure the network path is accessible and you have write permissions.
        pause
        exit /b 1
    )
)

REM Update the launcher script to use the shared database
echo Updating launcher script...
echo @echo off > "%INSTALL_DIR%\run-tireshop.bat"
echo echo Starting Tire Shop POS System with shared database... >> "%INSTALL_DIR%\run-tireshop.bat"
echo. >> "%INSTALL_DIR%\run-tireshop.bat"
echo java -jar tire-shop-pos-1.0-SNAPSHOT.jar -Ddb.url="jdbc:h2:%SHARED_DB_PATH%\tireshopdb" >> "%INSTALL_DIR%\run-tireshop.bat"
echo. >> "%INSTALL_DIR%\run-tireshop.bat"
echo if %%ERRORLEVEL%% NEQ 0 ( >> "%INSTALL_DIR%\run-tireshop.bat"
echo    echo Application failed to start. >> "%INSTALL_DIR%\run-tireshop.bat"
echo    pause >> "%INSTALL_DIR%\run-tireshop.bat"
echo ) >> "%INSTALL_DIR%\run-tireshop.bat"

echo.
echo Shared database setup complete!
echo.
echo This PC is now configured to use the shared database at:
echo %SHARED_DB_PATH%
echo.
echo Please run this script on all other PCs where you want to use
echo the same shared database.
echo.
pause 