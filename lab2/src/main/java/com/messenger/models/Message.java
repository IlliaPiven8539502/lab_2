package com.messenger.models;

public class Message {
    private String id;
    private String conversationId;
    private String senderId;
    private String text;
    private String createdAt;
    private String status; // sent | delivered | read

    public Message() {}

    public Message(String id, String conversationId, String senderId, String text, String createdAt) {
        this.id             = id;
        this.conversationId = conversationId;
        this.senderId       = senderId;
        this.text           = text;
        this.createdAt      = createdAt;
        this.status         = "sent";
    }

    public String getId()                     { return id; }
    public void   setId(String id)            { this.id = id; }
    public String getConversationId()                          { return conversationId; }
    public void   setConversationId(String c)                  { this.conversationId = c; }
    public String getSenderId()                  { return senderId; }
    public void   setSenderId(String s)          { this.senderId = s; }
    public String getText()                  { return text; }
    public void   setText(String t)          { this.text = t; }
    public String getCreatedAt()                  { return createdAt; }
    public void   setCreatedAt(String c)          { this.createdAt = c; }
    public String getStatus()                  { return status; }
    public void   setStatus(String s)          { this.status = s; }

    @Override
    public String toString() {
        return "{"
            + "\"id\":\"" + id + "\","
            + "\"conversationId\":\"" + conversationId + "\","
            + "\"senderId\":\"" + senderId + "\","
            + "\"text\":\"" + escape(text) + "\","
            + "\"createdAt\":\"" + createdAt + "\","
            + "\"status\":\"" + status + "\""
            + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
