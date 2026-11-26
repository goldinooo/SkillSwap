package com.trendenden.backend.notification.service;

import com.trendenden.backend.notification.dto.NotificationResponse;
import com.trendenden.backend.notification.entity.Notification;
import com.trendenden.backend.notification.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messaging;

    public NotificationService(NotificationRepository notificationRepository,
                               SimpMessagingTemplate messaging) {
        this.notificationRepository = notificationRepository;
        this.messaging = messaging;
    }

    public NotificationResponse create(UUID userId, String type, String payload) {
        Notification saved = notificationRepository.save(new Notification(userId, type, payload));
        NotificationResponse response = NotificationResponse.from(saved);

        messaging.convertAndSendToUser(userId.toString(), "/queue/notifications", response);
        return response;
    }
}