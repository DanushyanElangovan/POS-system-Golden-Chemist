@echo off
rem Compiles the application and pre-compiles the Jasper report templates (JDK 8+ required).
setlocal enabledelayedexpansion
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
if exist out\sources.txt del out\sources.txt
for /r src %%f in (*.java) do (
  set "p=%%f"
  set "p=!p:\=/!"
  echo "!p!" >> out\sources.txt
)
javac -encoding UTF-8 -nowarn -cp "lib/*" -d out @out\sources.txt
if errorlevel 1 exit /b 1
java -cp "out;lib/*" com.goldenchemist.pos.report.CompileReports
echo Build finished. Start the program with run.bat
