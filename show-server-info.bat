@echo off
echo ============================================
echo    Tire Shop POS - Server Information
echo ============================================
echo.
echo Use this information to configure client computers:
echo.

REM Get IP addresses
echo Your computer's IP addresses:
echo -----------------------------
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4 Address"') do echo %%a

echo.
echo Database Server Port: 9092
echo.
echo On client computers, edit start-pos-client.bat and change:
echo   set SERVER_IP=xxx.xxx.xxx.xxx
echo.
echo Replace xxx.xxx.xxx.xxx with one of the IP addresses shown above.
echo (Usually the one starting with 192.168)
echo.
pause 