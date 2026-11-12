@echo off
REM One-click build & run for Windows (works with Java 8+)
setlocal
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (echo. & echo Compile failed. Check that JDK is installed. & pause & exit /b 1)
xcopy /E /I /Y /Q src\main\resources out >nul

set CP=out
for %%f in (mysql-connector-j-*.jar) do set CP=out;%%f

if "%CP%"=="out" (
  echo [INFO] No MySQL driver jar found - running with in-memory storage.
) else (
  echo [INFO] MySQL driver found: %CP%
  if "%DB_USER%"=="" set DB_USER=root
  if "%DB_PASS%"=="" set /p DB_PASS=Enter your MySQL password for user %DB_USER%: 
)
java -cp "%CP%" com.heartpredict.Main
pause
