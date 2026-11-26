@echo off
chcp 65001 >nul
cd /d "%~dp0"
where pythonw >nul 2>nul
pause
if %errorlevel%==0 (
    start "" pythonw app.py %*
    pause
    exit /b 0
)
where python >nul 2>nul
if %errorlevel%==0 (
    python app.py %*
    pause
    if %errorlevel% neq 0 pause
    exit /b %errorlevel%
)
echo [错误] 未检测到 Python，请先安装 Python 3.10+ 并安装 pillow、numpy、opencv-python。
pause
