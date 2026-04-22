package com.messenger.models;

public class Conversation {
    private String id;
    private String type; // direct | group

    public Conversation() {}

    public Conversation(String id, String type) {
        this.id   = id;
        this.type = type;
    }

    public String getId()              { return id; }
    public void   setId(String id)    { this.id = id; }
    public String getType()            { return type; }
    public void   setType(String type){ this.type = type; }

    @Override
    public String toString() {
        return "{\"id\":\"" + id + "\",\"type\":\"" + type + "\"}";
    }
}
