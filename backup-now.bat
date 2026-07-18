@echo off
echo ============================================
echo    Manual Backup - Tire Shop POS
echo ============================================
echo.

echo Creating manual backup...
echo.

REM Run the backup through Java
java -cp "target/tire-shop-pos-1.0-SNAPSHOT.jar" -Dfile.encoding=UTF-8 com.tireshop.util.ManualBackup

echo.
echo Backup process completed.
echo Check the 'backups' folder for the new backup file.
echo.
pause 