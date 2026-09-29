@echo off
setlocal enabledelayedexpansion

REM Deteksi Java Runtime (JRE 17+ / JDK 25)
set JAVA_BIN=
if exist "C:\Program Files\Java\jdk-25\bin\java.exe" (
    set "JAVA_BIN=C:\Program Files\Java\jdk-25\bin\java.exe"
) else (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        if exist "%%D\bin\java.exe" (
            set "JAVA_BIN=%%D\bin\java.exe"
        )
    )
)
if "%JAVA_BIN%"=="" (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\java.exe" (
            set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
        )
    )
)
if "%JAVA_BIN%"=="" set "JAVA_BIN=java"

if not exist "bin\kurofin\test\KuroFinTestSuite.class" (
    echo Menjalankan kompilasi test...
    call build.bat
)

"%JAVA_BIN%" --enable-native-access=ALL-UNNAMED -cp "bin;lib/*" kurofin.test.KuroFinTestSuite
pause

endlocal
