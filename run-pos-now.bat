@echo off
echo Starting Tire Shop POS System
echo ================================
echo.

set "JAVA_HOME=%~dp0jdk-21.0.9+10"
set "MAVEN_HOME=%~dp0apache-maven-3.9.6"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo Java Home: %JAVA_HOME%
echo Maven Home: %MAVEN_HOME%
echo.

cd /d "%~dp0"
mvn javafx:run
