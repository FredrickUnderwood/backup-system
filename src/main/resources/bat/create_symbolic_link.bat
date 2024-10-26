@echo off
echo Destination Path: %1
echo Target: %2

set destinationPath=%1
set target=%2

REM 创建符号链接
mklink /D %destinationPath% %target%

REM 检查是否成功
if %errorlevel% equ 0 (
    echo Symbolic link created successfully.
    exit /b 0
) else (
    echo Failed to create symbolic link.
    exit /b 1
)