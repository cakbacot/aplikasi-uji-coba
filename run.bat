@echo off
setlocal enabledelayedexpansion

echo =========================================================
echo              KUROFIN - MENJALANKAN APLIKASI
echo =========================================================

REM Deteksi Java Runtime (JRE 17+ / JDK 25)
set JAVA_BIN=
if exist "C:\Program Files\Java\jdk-25\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Java\jdk-25\bin\java.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
    )
)

if "%JAVA_BIN%"=="" (
    where java >nul 2>nul
    if %errorlevel% equ 0 (
        set "JAVA_BIN=java"
    ) else (
        echo [ERROR] java.exe tidak ditemukan.
        pause
        exit /b 1
    )
)

REM Jika folder bin belum ada atau kosong, lakukan kompilasi terlebih dahulu
if not exist "bin\kurofin\Main.class" (
    echo Binary belum dikompilasi. Menjalankan build.bat...
    call build.bat
    if %errorlevel% neq 0 exit /b %errorlevel%
)

echo Menjalankan KuroFin menggunakan: "%JAVA_BIN%"...
start "" "%JAVA_BIN%" --enable-native-access=ALL-UNNAMED -cp "bin;lib/*" kurofin.Main

endlocal
