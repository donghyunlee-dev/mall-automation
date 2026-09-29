@echo off
setlocal
cd /d %~dp0
echo [start-backend] Using global Gradle home (C:\Users\%USERNAME%\.gradle)
echo [start-backend] ^> gradlew.bat bootRun --no-daemon
call gradlew.bat bootRun --no-daemon 1> server.log 2>&1
type server.log
set EXITCODE=%ERRORLEVEL%
if not %EXITCODE%==0 (
  echo [start-backend] Gradle wrapper failed (exit %EXITCODE%).
  echo [start-backend] If this is due to missing wrapper distribution or dependencies:
  echo   - Temporarily allow network to download inside %%GRADLE_USER_HOME%%
)
exit /b %EXITCODE%
