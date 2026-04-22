#!/bin/bash
# =============================================================
# Скрипт збірки та запуску Messenger API
# =============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$PROJECT_DIR/out"

echo "================================================"
echo "  Messenger API — Збірка та запуск"
echo "================================================"

if ! command -v javac &> /dev/null; then
  echo "ПОМИЛКА: javac не знайдено. Встановіть JDK 17+"
  exit 1
fi

echo "Java: $(java -version 2>&1 | head -1)"
echo ""
echo "Компіляція..."
mkdir -p "$OUT_DIR"

# Знаходимо всі .java файли — і в корені, і в підпапках
SOURCES=$(find "$PROJECT_DIR" -name "*.java" ! -path "$OUT_DIR/*")

echo "$SOURCES" | tr '\n' ' '
echo ""

javac -encoding UTF-8 -d "$OUT_DIR" $SOURCES
echo "  ✓ Скомпільовано"

MODE="${1:-server}"

if [ "$MODE" = "test" ]; then
  echo ""
  echo "Запуск інтеграційних тестів..."
  echo "------------------------------------------------"
  java -cp "$OUT_DIR" com.messenger.IntegrationTest
else
  echo ""
  echo "Запуск сервера на порту 8080..."
  echo "Зупинити: Ctrl+C"
  echo "------------------------------------------------"
  mkdir -p "$PROJECT_DIR/data"
  java -cp "$OUT_DIR" com.messenger.Main 8080 "$PROJECT_DIR/data"
fi
