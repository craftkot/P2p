@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-local.ps1"
if errorlevel 1 (
    echo.
    echo OpenTogether build failed. Copy the errors from this window.
    pause
    exit /b 1
)
echo.
echo JAR was placed in build\libs
pause
