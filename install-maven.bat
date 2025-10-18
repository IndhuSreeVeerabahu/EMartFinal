@echo off
echo Installing Maven for Windows...
echo.

REM Check if Maven is already installed
mvn --version >nul 2>&1
if %errorlevel% == 0 (
    echo Maven is already installed!
    mvn --version
    goto :end
)

echo Maven is not installed. Please follow these steps:
echo.
echo 1. Download Maven from: https://maven.apache.org/download.cgi
echo 2. Extract to C:\Program Files\Apache\maven
echo 3. Add C:\Program Files\Apache\maven\bin to your PATH environment variable
echo 4. Restart your command prompt
echo.
echo Alternative: Use Chocolatey package manager:
echo    choco install maven
echo.
echo Alternative: Use Scoop package manager:
echo    scoop install maven
echo.
echo After installation, run this script again to verify.

:end
pause
