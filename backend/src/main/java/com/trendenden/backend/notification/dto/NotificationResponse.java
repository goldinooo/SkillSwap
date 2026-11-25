package com.trendenden.backend.notification.dto;

import com.trendenden.backend.notification.entity.Notification;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String payload,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getPayload(), n.getCreatedAt(), n.getReadAt());
    }
}