@echo off
echo Setting up Java environment and running E-Commerce application...
echo.

REM Try to find Java installation
set JAVA_HOME=
for /d %%i in ("C:\Program Files\Java\*") do (
    if exist "%%i\bin\java.exe" (
        set JAVA_HOME=%%i
        goto :found
    )
)

for /d %%i in ("C:\Program Files\Eclipse Adoptium\*") do (
    if exist "%%i\bin\java.exe" (
        set JAVA_HOME=%%i
        goto :found
    )
)

for /d %%i in ("C:\Program Files\Microsoft\jdk-*") do (
    if exist "%%i\bin\java.exe" (
        set JAVA_HOME=%%i
        goto :found
    )
)

echo Java installation not found in standard locations.
echo Please set JAVA_HOME manually or install Java.
echo.
echo Common Java installation paths:
echo - C:\Program Files\Java\jdk-XX
echo - C:\Program Files\Eclipse Adoptium\jdk-XX
echo - C:\Program Files\Microsoft\jdk-XX
pause
exit /b 1

:found
echo Found Java at: %JAVA_HOME%
echo.

REM Set PATH to include Java
set PATH=%JAVA_HOME%\bin;%PATH%

REM Verify Java
java -version
echo.

REM Try to run Maven
echo Running Maven build...
call mvnw clean compile

if %errorlevel% == 0 (
    echo.
    echo Build successful! Running application...
    call mvnw spring-boot:run
) else (
    echo.
    echo Build failed. Please check the errors above.
    pause
)
