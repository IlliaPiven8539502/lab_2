package com.messenger.storage;

import com.messenger.models.Conversation;
import com.messenger.models.Message;
import com.messenger.models.User;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Простий файловий storage на основі JSON-рядків.
 * Кожен рядок у файлі — окремий JSON-об'єкт (формат JSON Lines).
 */
public class Database {

    private final Path dataDir;
    private final Path usersFile;
    private final Path conversationsFile;
    private final Path messagesFile;

    public Database(String dataDirectory) throws IOException {
        this.dataDir           = Paths.get(dataDirectory);
        this.usersFile         = dataDir.resolve("users.jsonl");
        this.conversationsFile = dataDir.resolve("conversations.jsonl");
        this.messagesFile      = dataDir.resolve("messages.jsonl");
        Files.createDirectories(dataDir);
        for (Path p : List.of(usersFile, conversationsFile, messagesFile)) {
            if (!Files.exists(p)) Files.createFile(p);
        }
    }

    // ── Users ────────────────────────────────────────────────────────────────

    public void saveUser(User user) throws IOException {
        appendLine(usersFile, user.toString());
    }

    public List<User> loadUsers() throws IOException {
        List<User> users = new ArrayList<>();
        for (String line : Files.readAllLines(usersFile)) {
            if (line.isBlank()) continue;
            users.add(parseUser(line));
        }
        return users;
    }

    public Optional<User> findUserById(String id) throws IOException {
        return loadUsers().stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    // ── Conversations ─────────────────────────────────────────────────────────

    public void saveConversation(Conversation conv) throws IOException {
        appendLine(conversationsFile, conv.toString());
    }

    public List<Conversation> loadConversations() throws IOException {
        List<Conversation> list = new ArrayList<>();
        for (String line : Files.readAllLines(conversationsFile)) {
            if (line.isBlank()) continue;
            list.add(parseConversation(line));
        }
        return list;
    }

    public Optional<Conversation> findConversationById(String id) throws IOException {
        return loadConversations().stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    public void saveMessage(Message msg) throws IOException {
        appendLine(messagesFile, msg.toString());
    }

    public List<Message> loadMessagesByConversation(String conversationId) throws IOException {
        List<Message> list = new ArrayList<>();
        for (String line : Files.readAllLines(messagesFile)) {
            if (line.isBlank()) continue;
            Message m = parseMessage(line);
            if (conversationId.equals(m.getConversationId())) list.add(m);
        }
        return list;
    }

    public List<Message> loadAllMessages() throws IOException {
        List<Message> list = new ArrayList<>();
        for (String line : Files.readAllLines(messagesFile)) {
            if (line.isBlank()) continue;
            list.add(parseMessage(line));
        }
        return list;
    }

    /** Оновлює статус повідомлення (перезаписує весь файл). */
    public boolean updateMessageStatus(String messageId, String newStatus) throws IOException {
        List<String> lines = Files.readAllLines(messagesFile);
        List<String> updated = new ArrayList<>();
        boolean found = false;
        for (String line : lines) {
            if (line.isBlank()) continue;
            Message m = parseMessage(line);
            if (m.getId().equals(messageId)) {
                m.setStatus(newStatus);
                updated.add(m.toString());
                found = true;
            } else {
                updated.add(line);
            }
        }
        if (found) Files.write(messagesFile, updated);
        return found;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendLine(Path file, String content) throws IOException {
        Files.writeString(file, content + "\n", StandardOpenOption.APPEND);
    }

    /** Мінімальний JSON-парсер для відомих структур (без зовнішніх бібліотек). */
    private String extractField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? "" : json.substring(start, end);
    }

    private User parseUser(String json) {
        return new User(extractField(json, "id"), extractField(json, "name"));
    }

    private Conversation parseConversation(String json) {
        return new Conversation(extractField(json, "id"), extractField(json, "type"));
    }

    private Message parseMessage(String json) {
        Message m = new Message(
            extractField(json, "id"),
            extractField(json, "conversationId"),
            extractField(json, "senderId"),
            extractField(json, "text"),
            extractField(json, "createdAt")
        );
        m.setStatus(extractField(json, "status"));
        return m;
    }
}
