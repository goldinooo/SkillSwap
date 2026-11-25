package com.trendenden.backend.notification.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "type", nullable = false, length = 40)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {}

    public Notification(UUID userId, String type, String payload) {
        this.userId = userId;
        this.type = type;
        this.payload = payload;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}