@echo off
setlocal enabledelayedexpansion

echo [INFO] Starting cleanup process for Agentbot project...

:: Current directory is Agentbot/JAVA
set "BASE_DIR=%~dp0"

:: 1. Clean Backend (Maven)
if exist "%BASE_DIR%target" (
    echo [CLEAN] Removing backend target directory: %BASE_DIR%target
    rd /s /q "%BASE_DIR%target"
)

:: 2. Clean Frontend (Vite/Vue)
if exist "%BASE_DIR%frontend\dist" (
    echo [CLEAN] Removing frontend build directory: %BASE_DIR%frontend\dist
    rd /s /q "%BASE_DIR%frontend\dist"
)

:: 3. Clean Log Files (HS Error logs and build logs)
echo [CLEAN] Removing log files...
if exist "%BASE_DIR%build_log.txt" del /f /q "%BASE_DIR%build_log.txt"
:: hs_err logs are in the root directory (moltbot-main)
if exist "%BASE_DIR%..\..\hs_err_pid*.log" del /f /q "%BASE_DIR%..\..\hs_err_pid*.log"

:: 4. Clean Workspace/Temp data
if exist "%BASE_DIR%workspace" (
    echo [CLEAN] Removing workspace temporary files...
    rd /s /q "%BASE_DIR%workspace"
    mkdir "%BASE_DIR%workspace"
)

echo [INFO] Cleanup completed successfully.
pause

