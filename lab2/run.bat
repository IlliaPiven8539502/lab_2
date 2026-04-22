@echo off
REM ============================================================
REM Скрипт збірки та запуску Messenger API (Windows)
REM Вимоги: Java 17+ (JDK)
REM ============================================================

set PROJECT_DIR=%~dp0
set OUT_DIR=%PROJECT_DIR%out
set SRC_MAIN=%PROJECT_DIR%src\main\java
set SRC_TEST=%PROJECT_DIR%src\test\java

echo ================================================
echo   Messenger API — Збірка та запуск
echo ================================================

where javac >nul 2>&1
if errorlevel 1 (
    echo ПОМИЛКА: javac не знайдено. Встановіть JDK 17+
    exit /b 1
)

echo Компіляція...
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

for /r "%SRC_MAIN%" %%f in (*.java) do (
    javac -encoding UTF-8 -d "%OUT_DIR%" "%%f"
)
echo   OK Основний код скомпільовано

for /r "%SRC_TEST%" %%f in (*.java) do (
    javac -encoding UTF-8 -cp "%OUT_DIR%" -d "%OUT_DIR%" "%%f"
)
echo   OK Тести скомпільовано

if "%1"=="test" (
    echo.
    echo Запуск інтеграційних тестів...
    echo ------------------------------------------------
    java -cp "%OUT_DIR%" com.messenger.IntegrationTest
) else (
    echo.
    echo Запуск сервера на порту 8080...
    echo Зупинити: Ctrl+C
    echo ------------------------------------------------
    if not exist "%PROJECT_DIR%data" mkdir "%PROJECT_DIR%data"
    java -cp "%OUT_DIR%" com.messenger.Main 8080 "%PROJECT_DIR%data"
)
