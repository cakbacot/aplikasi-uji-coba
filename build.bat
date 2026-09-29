@echo off
setlocal enabledelayedexpansion

echo =========================================================
echo             KUROFIN - KOMPILASI APLIKASI
echo =========================================================

REM Deteksi compiler Java 17+ / 25
set JAVAC_BIN=
if exist "C:\Program Files\Java\jdk-25\bin\javac.exe" (
    set "JAVAC_BIN=C:\Program Files\Java\jdk-25\bin\javac.exe"
) else (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        if exist "%%D\bin\javac.exe" (
            set "JAVAC_BIN=%%D\bin\javac.exe"
        )
    )
)
if "%JAVAC_BIN%"=="" (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\javac.exe" (
            set "JAVAC_BIN=%JAVA_HOME%\bin\javac.exe"
        )
    )
)

if "%JAVAC_BIN%"=="" (
    where javac >nul 2>nul
    if %errorlevel% equ 0 (
        set "JAVAC_BIN=javac"
    ) else (
        echo [ERROR] Compiler javac tidak ditemukan. Pastikan JDK 17+ telah terpasang.
        exit /b 1
    )
)

echo Menggunakan compiler: "%JAVAC_BIN%"

REM Buat direktori output
if not exist "bin" mkdir "bin"

echo Mengompilasi kode sumber KuroFin (Target Java 17+)...
"%JAVAC_BIN%" -encoding UTF-8 --release 17 -cp "lib/*" -d bin src/kurofin/*.java src/kurofin/model/*.java src/kurofin/dao/*.java src/kurofin/controller/*.java src/kurofin/util/*.java src/kurofin/view/*.java src/kurofin/view/dialog/*.java src/kurofin/view/chart/*.java src/kurofin/test/*.java

if %errorlevel% equ 0 (
    echo [SUKSES] Kompilasi berhasil! Seluruh class tersimpan di folder bin/
) else (
    echo [GAGAL] Kompilasi mengalami error.
    exit /b %errorlevel%
)

endlocal
