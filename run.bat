@echo off
setlocal
cd /d "%~dp0"

echo Cleaning old builds...
if exist "target\classes" rmdir /s /q "target\classes"
mkdir "target\classes"

set "SQLITE_JAR=%~dp0lib\sqlite-jdbc-3.49.1.0.jar"

echo Compiling Java source files...
javac --add-modules jdk.httpserver -d "target\classes" -cp "%SQLITE_JAR%" App.java Loginframe.java RecyclingDatabase.java RecyclingHttpServer.java
if errorlevel 1 (
    echo Compilation Failed.
    pause
    exit /b 1
)

echo.
echo Starting the local recycling system...
echo.

java --add-modules jdk.httpserver --enable-native-access=ALL-UNNAMED -cp "target\classes;%SQLITE_JAR%" ug.dankk.App
if errorlevel 1 (
    echo The recycling system stopped with an error.
)
pause
endlocal
