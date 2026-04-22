package com.messenger;

import com.messenger.api.Router;
import com.messenger.models.Conversation;
import com.messenger.models.Message;
import com.messenger.models.User;
import com.messenger.services.MessageService;
import com.messenger.storage.Database;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;

/**
 * Інтеграційний тест — перевіряє повний сценарій через реальний HTTP API.
 *
 * Сценарій:
 *   1. Створити користувача А
 *   2. Створити користувача Б
 *   3. Створити розмову
 *   4. Надіслати повідомлення від А до Б
 *   5. Отримати історію повідомлень
 *   6. Перевірити, що повідомлення існує зі статусом "sent"
 *   7. Оновити статус на "delivered"
 *   8. Перевірити оновлення статусу
 *   9. Перевірити обробку помилок (порожній текст, неіснуючий відправник)
 */
public class IntegrationTest {

    private static final int PORT     = 8765;
    private static final String BASE  = "http://localhost:" + PORT;
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        // Підготовка: тимчасова тека для тестових даних
        Path testDir = Files.createTempDirectory("messenger-test-");

        Database       db      = new Database(testDir.toString());
        MessageService service = new MessageService(db);
        Router         router  = new Router(service, PORT);
        router.start();

        System.out.println("=== Інтеграційний тест Messenger API ===\n");

        try {
            runTests();
        } finally {
            router.stop();
            // Видалити тимчасові файли
            Files.walk(testDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }

        System.out.println("\n─────────────────────────────────────────");
        System.out.printf("Результат: %d пройшло / %d провалено%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static void runTests() throws Exception {

        // ── Тест 1: Створити користувача А ────────────────────────────────────
        String resA = post("/users", "{\"name\":\"Аліса\"}");
        String userAId = extractField(resA, "id");
        assertNotBlank("Тест 1 — Створення користувача А: отримано id", userAId);
        assertContains("Тест 1 — Ім'я збережено", resA, "Аліса");

        // ── Тест 2: Створити користувача Б ────────────────────────────────────
        String resB = post("/users", "{\"name\":\"Богдан\"}");
        String userBId = extractField(resB, "id");
        assertNotBlank("Тест 2 — Створення користувача Б: отримано id", userBId);

        // ── Тест 3: Отримати список користувачів ──────────────────────────────
        String usersRes = get("/users");
        assertContains("Тест 3 — Список містить Аліса", usersRes, "Аліса");
        assertContains("Тест 3 — Список містить Богдан", usersRes, "Богдан");

        // ── Тест 4: Створити розмову ──────────────────────────────────────────
        String convRes = post("/conversations", "{\"type\":\"direct\"}");
        String convId  = extractField(convRes, "id");
        assertNotBlank("Тест 4 — Створення розмови: отримано id", convId);

        // ── Тест 5: Надіслати повідомлення ────────────────────────────────────
        String msgPayload = String.format(
            "{\"conversationId\":\"%s\",\"senderId\":\"%s\",\"text\":\"Привіт, Богдане!\"}",
            convId, userAId
        );
        String msgRes = post("/messages", msgPayload);
        String msgId  = extractField(msgRes, "id");
        assertNotBlank("Тест 5 — Надсилання повідомлення: отримано id", msgId);
        assertContains("Тест 5 — Початковий статус 'sent'", msgRes, "\"status\":\"sent\"");

        // ── Тест 6: Отримати історію ──────────────────────────────────────────
        String history = get("/conversations/" + convId + "/messages");
        assertContains("Тест 6 — Повідомлення є в історії", history, "Привіт, Богдане!");
        assertContains("Тест 6 — senderId відповідає", history, userAId);

        // ── Тест 7: Оновити статус на 'delivered' (Варіант 2) ─────────────────
        String patchRes = patch("/messages/" + msgId + "/status", "{\"status\":\"delivered\"}");
        assertContains("Тест 7 — Статус оновлено", patchRes, "\"updated\":true");
        assertContains("Тест 7 — Новий статус 'delivered'", patchRes, "\"status\":\"delivered\"");

        // ── Тест 8: Оновити статус на 'read' ─────────────────────────────────
        String patchRead = patch("/messages/" + msgId + "/status", "{\"status\":\"read\"}");
        assertContains("Тест 8 — Статус 'read' встановлено", patchRead, "\"status\":\"read\"");

        // ── Тест 9: Помилка — порожній текст ─────────────────────────────────
        String emptyMsg = postExpectError("/messages",
            String.format("{\"conversationId\":\"%s\",\"senderId\":\"%s\",\"text\":\"\"}",
                convId, userAId));
        assertContains("Тест 9 — Помилка при порожньому тексті", emptyMsg, "error");

        // ── Тест 10: Помилка — неіснуючий відправник ─────────────────────────
        String badSender = postExpectError("/messages",
            String.format("{\"conversationId\":\"%s\",\"senderId\":\"не-існує\",\"text\":\"Привіт\"}",
                convId));
        assertContains("Тест 10 — Помилка при неіснуючому відправнику", badSender, "error");

        // ── Тест 11: Помилка — неіснуюча розмова ─────────────────────────────
        String badConv = postExpectError("/messages",
            String.format("{\"conversationId\":\"не-існує\",\"senderId\":\"%s\",\"text\":\"Привіт\"}",
                userAId));
        assertContains("Тест 11 — Помилка при неіснуючій розмові", badConv, "error");
    }

    // ── HTTP-утиліти ──────────────────────────────────────────────────────────

    private static String post(String path, String body) throws IOException {
        return request("POST", path, body, 201);
    }

    private static String postExpectError(String path, String body) throws IOException {
        return request("POST", path, body, -1); // будь-який код
    }

    private static String get(String path) throws IOException {
        return request("GET", path, null, 200);
    }

    private static String patch(String path, String body) throws IOException {
        // HttpURLConnection не підтримує PATCH — використовуємо POST з Override-заголовком
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE + path).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        InputStream is = (code < 400) ? conn.getInputStream() : conn.getErrorStream();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String request(String method, String path, String body, int expectedCode)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE + path).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        if (body != null) {
            conn.setDoOutput(true);
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        InputStream is = (code < 400) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (expectedCode > 0 && code != expectedCode) {
            System.out.printf("  [УВАГА] %s %s → очікувано %d, отримано %d%n",
                method, path, expectedCode, code);
        }
        return response;
    }

    // ── Перевірки ─────────────────────────────────────────────────────────────

    private static void assertNotBlank(String testName, String value) {
        if (value != null && !value.isBlank()) {
            System.out.println("  ✓ " + testName);
            passed++;
        } else {
            System.out.println("  ✗ " + testName + " (значення порожнє)");
            failed++;
        }
    }

    private static void assertContains(String testName, String haystack, String needle) {
        if (haystack != null && haystack.contains(needle)) {
            System.out.println("  ✓ " + testName);
            passed++;
        } else {
            System.out.println("  ✗ " + testName);
            System.out.println("    Очікувалось містити: " + needle);
            System.out.println("    Відповідь: " + haystack);
            failed++;
        }
    }

    private static String extractField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? "" : json.substring(start, end);
    }
}
