@echo off
rem Loads KEY=VALUE pairs from .env (comments and blank lines skipped), then starts the app.
cd /d "%~dp0"
if not exist ".env" (
  echo .env not found in %CD%
  exit /b 1
)
for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do set "%%A=%%B"
call mvnw.cmd spring-boot:run -DskipTests
