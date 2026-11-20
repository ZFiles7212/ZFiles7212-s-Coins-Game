@echo off
setlocal
cd /d "%~dp0"

set "JAVA_EXE=%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin\java.exe"

if not exist "%JAVA_EXE%" (
    echo Java 21 was not found at:
    echo "%JAVA_EXE%"
    pause
    exit /b 1
)

if not exist "CoinWorld.java" (
    echo Put CoinWorld.java in the same folder as Play.bat.
    pause
    exit /b 1
)

"%JAVA_EXE%" "CoinWorld.java"

if errorlevel 1 (
    echo The game could not run. Read the error above.
    pause
    exit /b 1
)