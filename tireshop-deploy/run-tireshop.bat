@echo off
echo Attempting to run the application directly...

REM Try several possible Java installations
set JAVA_PATH=java
set CLASS_PATH=target\classes

if not exist "%CLASS_PATH%" (
    echo Target classes directory not found, trying to compile...
    
    REM Try to locate javac in different potential locations
    set JAVAC_CMD=javac
    
    if exist "C:\Program Files\Java\jdk-17\bin\javac.exe" (
        set JAVAC_CMD=C:\Program Files\Java\jdk-17\bin\javac.exe
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\bin\javac.exe" (
        set JAVAC_CMD=C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\bin\javac.exe
    ) else if exist "C:\Program Files\Java\jdk-11\bin\javac.exe" (
        set JAVAC_CMD=C:\Program Files\Java\jdk-11\bin\javac.exe
    ) else if exist "C:\Program Files\Java\jdk1.8.0_301\bin\javac.exe" (
        set JAVAC_CMD=C:\Program Files\Java\jdk1.8.0_301\bin\javac.exe
    )
    
    echo Compiling with %JAVAC_CMD%...
    mkdir target\classes 2>nul
    %JAVAC_CMD% -d target\classes -cp "lib\*" src\main\java\com\tireshop\*.java src\main\java\com\tireshop\*\*.java
    
    if %ERRORLEVEL% NEQ 0 (
        echo Compilation failed!
        pause
        exit /b 1
    )
)

REM Try to run with Java from various potential locations
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set JAVA_PATH=C:\Program Files\Java\jdk-17\bin\java
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\bin\java.exe" (
    set JAVA_PATH=C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\bin\java
) else if exist "C:\Program Files\Java\jdk-11\bin\java.exe" (
    set JAVA_PATH=C:\Program Files\Java\jdk-11\bin\java
) else if exist "C:\Program Files\Java\jdk1.8.0_301\bin\java.exe" (
    set JAVA_PATH=C:\Program Files\Java\jdk1.8.0_301\bin\java
)

echo Running with %JAVA_PATH%...
%JAVA_PATH% -cp "%CLASS_PATH%;lib\*" com.tireshop.Main

if %ERRORLEVEL% NEQ 0 (
    echo Application execution failed with error code %ERRORLEVEL%
)

pause 