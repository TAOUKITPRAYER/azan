@echo off
REM Lance RemoteBox. Construit la distribution si besoin, puis démarre l'app.
setlocal
cd /d "%~dp0"

if not exist "build\install\remotebox\bin\remotebox.bat" (
    echo Premiere construction...
    call gradlew.bat installDist || goto :err
)

start "" "build\install\remotebox\bin\remotebox.bat"
exit /b 0

:err
echo Echec de la construction.
pause
exit /b 1
