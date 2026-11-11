@echo off
setlocal EnableExtensions
cd /d "%~dp0"
call "%~dp0mvnw.cmd" -B -ntp spring-boot:run
set "BAKERY_EXIT_CODE=%ERRORLEVEL%"
if not "%BAKERY_EXIT_CODE%"=="0" (
  echo.
  echo The build or start command stopped. Read the message above.
  echo Java 21 is required. The first build needs internet access.
  pause
)
exit /b %BAKERY_EXIT_CODE%
