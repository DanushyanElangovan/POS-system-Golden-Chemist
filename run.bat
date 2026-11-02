@echo off
cd /d "%~dp0"
if not exist out call build.bat
start "" javaw -cp "out;lib/*" com.goldenchemist.pos.App
