@echo off
setlocal enabledelayedexpansion

echo =========================================================
echo       KUROFIN - PACKAGING STANDALONE JAR ^& EXE
echo =========================================================

REM 1. Deteksi Java Tools (jar.exe dan jpackage.exe)
set "JAR_BIN="
set "JPACKAGE_BIN="

if exist "C:\Program Files\Java\jdk-25\bin\jar.exe" (
    set "JAR_BIN=C:\Program Files\Java\jdk-25\bin\jar.exe"
    set "JPACKAGE_BIN=C:\Program Files\Java\jdk-25\bin\jpackage.exe"
) else (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        if exist "%%D\bin\jar.exe" (
            set "JAR_BIN=%%D\bin\jar.exe"
            set "JPACKAGE_BIN=%%D\bin\jpackage.exe"
        )
    )
)

if "%JAR_BIN%"=="" (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\jar.exe" (
            set "JAR_BIN=%JAVA_HOME%\bin\jar.exe"
            set "JPACKAGE_BIN=%JAVA_HOME%\bin\jpackage.exe"
        )
    )
)

if "%JAR_BIN%"=="" (
    echo [ERROR] jar.exe tidak ditemukan. Pastikan JDK 17+ terpasang.
    exit /b 1
)

REM 2. Pastikan file class terbaru telah dikompilasi
echo [1/3] Mengompilasi kode sumber terbaru...
call build.bat
if %errorlevel% neq 0 (
    echo [ERROR] Kompilasi gagal.
    exit /b %errorlevel%
)

REM 3. Buat Executable Fat/Uber JAR (KuroFin.jar)
echo [2/3] Mengemas seluruh pustaka ke KuroFin.jar...
if exist "build_jar" rd /s /q "build_jar"
mkdir "build_jar"

pushd "build_jar"
for %%F in ("..\lib\*.jar") do (
    "%JAR_BIN%" xf "%%F"
)
if exist "META-INF\*.SF" del /f /q "META-INF\*.SF"
if exist "META-INF\*.DSA" del /f /q "META-INF\*.DSA"
if exist "META-INF\*.RSA" del /f /q "META-INF\*.RSA"
popd

xcopy /s /e /y "bin\*" "build_jar\" >nul

(
echo Manifest-Version: 1.0
echo Main-Class: kurofin.Main
echo Implementation-Title: KuroFin
echo Implementation-Version: 1.0.0
) > manifest.txt

"%JAR_BIN%" --create --file "KuroFin.jar" --manifest "manifest.txt" -C "build_jar" .
rd /s /q "build_jar"
if exist "manifest.txt" del /f /q "manifest.txt"

echo [SUKSES] KuroFin.jar berhasil dibuat!

REM 4. Buat Native Windows Executable (KuroFin.exe) via jpackage jika tersedia
if exist "%JPACKAGE_BIN%" (
    echo [3/3] Membangun native Windows Executable KuroFin.exe via jpackage...
    if exist "dist" rd /s /q "dist"
    if exist "release_input" rd /s /q "release_input"
    mkdir "release_input"
    copy /y "KuroFin.jar" "release_input\" >nul

    "%JPACKAGE_BIN%" --name "KuroFin" --input "release_input" --main-jar "KuroFin.jar" --main-class "kurofin.Main" --type app-image --dest "dist" --java-options "--enable-native-access=ALL-UNNAMED"

    rd /s /q "release_input"
    echo [SUKSES] Aplikasi native Windows selesai dibuat pada direktori: dist\KuroFin\KuroFin.exe
) else (
    echo [INFO] jpackage tidak ditemukan, executable JAR tetap siap digunakan di KuroFin.jar.
)

echo =========================================================
echo SELESAI:
echo  - Standalone JAR : KuroFin.jar (Cukup klik ganda / java -jar)
echo  - Native Windows : dist\KuroFin\KuroFin.exe (Tanpa perlu instalasi Java)
echo =========================================================

endlocal
