package com.mouli.producer.dto;

import java.time.Instant;
import java.util.Map;

public class CommandMessage {

    private String messageId;
    private Map<String, Object> payload;
    private Instant createdAt;

    public CommandMessage() {
    }

    public CommandMessage(String messageId, Map<String, Object> payload) {
        this.messageId = messageId;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public String getMessageId() {
        return messageId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
