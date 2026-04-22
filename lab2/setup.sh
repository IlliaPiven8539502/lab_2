#!/bin/bash
# =============================================================
# Створює правильну структуру папок із плоского розташування файлів
# Запустити ОДИН РАЗ перед run.sh
# =============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Створення структури папок..."

mkdir -p "$PROJECT_DIR/src/main/java/com/messenger/models"
mkdir -p "$PROJECT_DIR/src/main/java/com/messenger/services"
mkdir -p "$PROJECT_DIR/src/main/java/com/messenger/storage"
mkdir -p "$PROJECT_DIR/src/main/java/com/messenger/api"
mkdir -p "$PROJECT_DIR/src/test/java/com/messenger"

# Переміщуємо файли у відповідні папки
move_if_exists() {
  local file="$PROJECT_DIR/$1"
  local dest="$2"
  if [ -f "$file" ]; then
    mv "$file" "$dest"
    echo "  ✓ $1 → $dest"
  else
    echo "  ! $1 не знайдено (пропускаємо)"
  fi
}

move_if_exists "User.java"             "$PROJECT_DIR/src/main/java/com/messenger/models/"
move_if_exists "Message.java"          "$PROJECT_DIR/src/main/java/com/messenger/models/"
move_if_exists "Conversation.java"     "$PROJECT_DIR/src/main/java/com/messenger/models/"
move_if_exists "Database.java"         "$PROJECT_DIR/src/main/java/com/messenger/storage/"
move_if_exists "MessageService.java"   "$PROJECT_DIR/src/main/java/com/messenger/services/"
move_if_exists "Router.java"           "$PROJECT_DIR/src/main/java/com/messenger/api/"
move_if_exists "Main.java"             "$PROJECT_DIR/src/main/java/com/messenger/"
move_if_exists "IntegrationTest.java"  "$PROJECT_DIR/src/test/java/com/messenger/"

echo ""
echo "Готово! Тепер запустіть:"
echo "  ./run.sh        — сервер"
echo "  ./run.sh test   — тести"
