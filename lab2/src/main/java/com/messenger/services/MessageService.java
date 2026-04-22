package com.messenger.services;

import com.messenger.models.Conversation;
import com.messenger.models.Message;
import com.messenger.models.User;
import com.messenger.storage.Database;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервіс повідомлень — містить всю бізнес-логіку.
 * Відповідає за: створення користувачів, розмов, надсилання повідомлень,
 * отримання історії та оновлення статусів (Варіант 2 — відстеження статусів).
 */
public class MessageService {

    private final Database db;

    public MessageService(Database db) {
        this.db = db;
    }

    // ── Користувачі ───────────────────────────────────────────────────────────

    public User createUser(String name) throws IOException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ім'я користувача не може бути порожнім");
        }
        User user = new User(UUID.randomUUID().toString(), name.trim());
        db.saveUser(user);
        return user;
    }

    public List<User> getAllUsers() throws IOException {
        return db.loadUsers();
    }

    public Optional<User> getUserById(String id) throws IOException {
        return db.findUserById(id);
    }

    // ── Розмови ───────────────────────────────────────────────────────────────

    public Conversation createConversation(String type) throws IOException {
        if (!type.equals("direct") && !type.equals("group")) {
            throw new IllegalArgumentException("Тип розмови має бути 'direct' або 'group'");
        }
        Conversation conv = new Conversation(UUID.randomUUID().toString(), type);
        db.saveConversation(conv);
        return conv;
    }

    public List<Conversation> getAllConversations() throws IOException {
        return db.loadConversations();
    }

    // ── Повідомлення ──────────────────────────────────────────────────────────

    /**
     * Надсилає повідомлення.
     * Перевіряє: існування відправника, існування розмови, непорожній текст.
     */
    public Message sendMessage(String conversationId, String senderId, String text)
            throws IOException {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Текст повідомлення не може бути порожнім");
        }

        Optional<User> sender = db.findUserById(senderId);
        if (sender.isEmpty()) {
            throw new IllegalArgumentException("Користувача з id='" + senderId + "' не знайдено");
        }

        Optional<Conversation> conv = db.findConversationById(conversationId);
        if (conv.isEmpty()) {
            throw new IllegalArgumentException("Розмову з id='" + conversationId + "' не знайдено");
        }

        Message msg = new Message(
            UUID.randomUUID().toString(),
            conversationId,
            senderId,
            text.trim(),
            Instant.now().toString()
        );
        db.saveMessage(msg);
        return msg;
    }

    /**
     * Повертає історію повідомлень конкретної розмови.
     */
    public List<Message> getMessages(String conversationId) throws IOException {
        Optional<Conversation> conv = db.findConversationById(conversationId);
        if (conv.isEmpty()) {
            throw new IllegalArgumentException("Розмову з id='" + conversationId + "' не знайдено");
        }
        return db.loadMessagesByConversation(conversationId);
    }

    /**
     * Оновлює статус повідомлення (Варіант 2).
     * Дозволені переходи: sent → delivered → read
     */
    public boolean updateStatus(String messageId, String newStatus) throws IOException {
        if (!List.of("sent", "delivered", "read").contains(newStatus)) {
            throw new IllegalArgumentException(
                "Невідомий статус '" + newStatus + "'. Допустимі: sent, delivered, read"
            );
        }
        return db.updateMessageStatus(messageId, newStatus);
    }
}
