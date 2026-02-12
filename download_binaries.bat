@echo off
setlocal enabledelayedexpansion

set VERSION=2.1.8
set BASE_URL=https://github.com/9seconds/mtg/releases/download/v%VERSION%

echo === Downloading mtg v%VERSION% binaries for Android ===
echo.

if not exist "app\src\main\jniLibs\arm64-v8a" mkdir app\src\main\jniLibs\arm64-v8a
if not exist "app\src\main\jniLibs\armeabi-v7a" mkdir app\src\main\jniLibs\armeabi-v7a
if not exist "app\src\main\jniLibs\x86_64" mkdir app\src\main\jniLibs\x86_64
if not exist "app\src\main\jniLibs\x86" mkdir app\src\main\jniLibs\x86
if not exist "temp_downloads" mkdir temp_downloads

call :download_and_extract "arm64-v8a" "mtg-%VERSION%-linux-arm64.tar.gz" "arm64-v8a"
call :download_and_extract "armeabi-v7a" "mtg-%VERSION%-linux-armv7.tar.gz" "armeabi-v7a"
call :download_and_extract "x86_64" "mtg-%VERSION%-linux-amd64.tar.gz" "x86_64"
call :download_and_extract "x86" "mtg-%VERSION%-linux-386.tar.gz" "x86"

echo Cleaning up...
rmdir /s /q temp_downloads 2>nul

echo.
echo === Download complete! ===
echo Binaries are in app\src\main\jniLibs\
echo.
dir /s app\src\main\jniLibs\libmtg.so
echo.
pause
exit /b

:download_and_extract
set ARCH=%~1
set FILE=%~2
set TARGET_DIR=%~3

echo Downloading %FILE%...

if exist "temp_downloads\%FILE%" (
    echo   Already downloaded, skipping...
) else (
    curl -L -o "temp_downloads\%FILE%" "%BASE_URL%/%FILE%"
    if errorlevel 1 (
        echo   Error downloading %FILE%
        exit /b 1
    )
)

echo Extracting to %TARGET_DIR%...

del /q temp_downloads\mtg 2>nul
for /d %%i in (temp_downloads\mtg-*) do rmdir /s /q "%%i"

tar -xzf "temp_downloads\%FILE%" -C temp_downloads --strip-components=1

if errorlevel 1 (
    echo Error: tar not found or extraction failed
    echo Install Git for Windows or enable tar in Windows features
    exit /b 1
)

copy /y "temp_downloads\mtg" "app\src\main\jniLibs\%TARGET_DIR%\libmtg.so" >nul
del "temp_downloads\mtg"

echo [OK] %ARCH% done
echo.
exit /b 0
