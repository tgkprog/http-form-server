@echo off
setlocal enabledelayedexpansion

if "%1"=="" (
  set PORT=8080
) else (
  set PORT=%1
)

for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%PORT%" ^| findstr LISTENING') do (
  set PID=%%a
  goto :found
)

echo No process listening on port %PORT%
goto :end

:found
taskkill /F /PID %PID%
echo Killed process %PID% on port %PORT%

:end
