# Lab 2 — Messenger API

**Дисципліна:** Проєктування та документування ПЗ  
**Варіант:** 2 — Відстеження статусів повідомлень  
**Мова:** Java · без зовнішніх залежностей  

---

## Що реалізовано

REST API месенджера з повним циклом повідомлення:

```
sent  →  delivered  →  read
```

Стек: вбудований `com.sun.net.httpserver` + файловий storage у форматі JSON Lines.  
Зовнішніх бібліотек немає — тільки стандартна Java.

---

## Швидкий старт

```bash
# клонувати репозиторій
git clone https://github.com/IlliaPiven8539502/lab_2.git
cd lab_2/lab2

# зробити скрипт виконуваним і запустити сервер
chmod +x run.sh
./run.sh
```

Сервер стартує на **http://localhost:8080**  
Відкрий у браузері — побачиш інтерактивний тестовий інтерфейс.

### Запуск тестів

```bash
./run.sh test
```

```
Результат: 16 пройшло / 0 провалено
```

---

## Структура

```
lab2/
├── src/
│   ├── main/java/com/messenger/
│   │   ├── api/          Router.java         — HTTP маршрутизатор + UI
│   │   ├── models/       User, Message, Conversation
│   │   ├── services/     MessageService.java — бізнес-логіка
│   │   ├── storage/      Database.java       — JSON Lines файли
│   │   └── Main.java
│   └── test/java/com/messenger/
│       └── IntegrationTest.java              — 16 інтеграційних тестів
├── postman_collection.json
├── run.sh
└── run.bat
```

---

## API

| Метод | Маршрут | Опис |
|---|---|---|
| `POST` | `/users` | Створити користувача |
| `GET` | `/users` | Список користувачів |
| `POST` | `/conversations` | Створити розмову |
| `GET` | `/conversations` | Список розмов |
| `POST` | `/messages` | Надіслати повідомлення |
| `GET` | `/conversations/{id}/messages` | Історія розмови |
| `PATCH` | `/messages/{id}/status` | Оновити статус ← Варіант 2 |

### Приклад сценарію через curl

```bash
# 1. Створити користувача
curl -s -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Аліса"}'

# 2. Створити розмову
curl -s -X POST http://localhost:8080/conversations \
  -H "Content-Type: application/json" \
  -d '{"type":"direct"}'

# 3. Надіслати повідомлення
curl -s -X POST http://localhost:8080/messages \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"<convId>","senderId":"<userId>","text":"Привіт!"}'

# 4. Оновити статус
curl -s -X POST http://localhost:8080/messages/<msgId>/status \
  -H "Content-Type: application/json" \
  -H "X-HTTP-Method-Override: PATCH" \
  -d '{"status":"delivered"}'
```

---

## Тестування в Postman

1. **Import** → обрати `postman_collection.json`
2. Запускати запити по порядку — змінні (`userAId`, `convId`, `messageId`) зберігаються автоматично
3. Папки: `1. Користувачі` → `2. Розмови` → `3. Повідомлення` → `4. Помилки`

---

## Збереження даних

Дані зберігаються у `./data/` у форматі JSON Lines — кожен рядок є окремим записом:

```
data/users.jsonl
data/conversations.jsonl
data/messages.jsonl
```

Файли зберігаються між перезапусками сервера. Для скидання — видалити вміст папки `data/`.
