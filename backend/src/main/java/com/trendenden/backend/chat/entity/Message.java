package com.trendenden.backend.chat.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "messages")

public class Message{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private  Long sessionId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "content", nullable = false)
    private String content;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "read_at")
    private Instant readAt;

    protected Message(){}

    public Message(Long sessionId, UUID senderId, String content){
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.content = content;
    }

    public Long getId(){
        return id;
    }
    
    public Long getSessionId(){
        return sessionId;
    }

    public UUID getSenderId(){
        return senderId;
    }

    public String getContent(){
        return content;
    }
    
    public Instant getCreatedAt(){
        return createdAt;
    }

    public Instant getReadAt(){
        return readAt;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
    
}
