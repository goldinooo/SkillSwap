package com.trendenden.backend.chat.dto;

import com.trendenden.backend.chat.entity.Message;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(Long id, Long sessionId, UUID senderId, String content, Instant createdAt, Instant readAt){
    public static MessageResponse from (Message m){
        return new MessageResponse(m.getId(),
                                    m.getSessionId(),
                                    m.getSenderId(),
                                    m.getContent(),
                                    m.getCreatedAt(),
                                    m.getReadAt()
                                );
    }
}