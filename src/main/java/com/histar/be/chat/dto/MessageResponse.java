package com.histar.be.chat.dto;

import com.histar.be.message.entity.Message;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, String role, String content, Instant createdAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
