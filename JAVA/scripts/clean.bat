@echo off
setlocal enabledelayedexpansion

echo [INFO] Starting cleanup process for Agentbot project...

::: Current directory is Agentbot/JAVA
set "BASE_DIR=%~dp0..\"


::: 1. Clean Backend (Maven)
if exist "%BASE_DIR%target" (
    echo [CLEAN] Removing backend target directory: %BASE_DIR%target
    rd /s /q "%BASE_DIR%target"
)

::: 2. Clean Frontend (Vite/Vue)
if exist "%BASE_DIR%frontend\dist" (
    echo [CLEAN] Removing frontend build directory: %BASE_DIR%frontend\dist
    rd /s /q "%BASE_DIR%frontend\dist"
)

::: 2.1 Clean top-level dist (if exists)
if exist "%BASE_DIR%dist" (
    echo [CLEAN] Removing dist directory: %BASE_DIR%dist
    rd /s /q "%BASE_DIR%dist"
)

:: 2.2 Clean top-level build (if exists)
if exist "%BASE_DIR%build" (
    echo [CLEAN] Removing build directory: %BASE_DIR%build
    rd /s /q "%BASE_DIR%build"
)

::: 2.3 Clean static resources files (keep directory)
if exist "%BASE_DIR%src\main\resources\static" (
    echo [CLEAN] Removing static files under: %BASE_DIR%src\main\resources\static
    del /f /q "%BASE_DIR%src\main\resources\static\*" >nul 2>nul
)

:::: 2.4 Clean static assets files (keep directory)
if exist "%BASE_DIR%src\main\resources\static\assets" (
    echo [CLEAN] Removing static assets files under: %BASE_DIR%src\main\resources\static\assets
    del /f /q "%BASE_DIR%src\main\resources\static\assets\*" >nul 2>nul
)




::: 3. Clean Log Files (HS Error logs and build logs)
echo [CLEAN] Removing log files...
if exist "%BASE_DIR%build_log.txt" del /f /q "%BASE_DIR%build_log.txt"
::: hs_err logs are in the root directory (moltbot-main)
if exist "%BASE_DIR%..\..\hs_err_pid*.log" del /f /q "%BASE_DIR%..\..\hs_err_pid*.log"
:::: agentbot log directory (config\log)
if exist "%BASE_DIR%log" (
    echo [CLEAN] Removing agentbot log directory: %BASE_DIR%log
    rd /s /q "%BASE_DIR%log"
)

:::: remove config\node.yml
if exist "%BASE_DIR%config\node.yml" (
    echo [CLEAN] Removing config node.yml: %BASE_DIR%config\node.yml
    del /f /q "%BASE_DIR%config\node.yml"
)


::: 4. Clean Workspace Sessions data (all agents)
echo [CLEAN] Cleaning agent sessions...
if exist "%BASE_DIR%workspace\agents" (
    for /d %%A in ("%BASE_DIR%workspace\agents\*") do (
        if exist "%%A\sessions" (
            echo [CLEAN] Removing sessions for agent: %%~nxA
            rd /s /q "%%A\sessions"
            mkdir "%%A\sessions"
        )
    )
)

::: Clean old sessions directory (if exists from pre-migration)
if exist "%BASE_DIR%workspace\sessions" (
    echo [CLEAN] Removing old workspace sessions directory
    rd /s /q "%BASE_DIR%workspace\sessions"
)

echo [INFO] Cleanup completed successfully.
pause

