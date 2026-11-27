package com.trendenden.backend.notification.service;

import com.trendenden.backend.notification.repository.NotificationRepository;
import com.trendenden.backend.notification.dto.NotificationResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import com.trendenden.backend.notification.entity.Notification;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import java.time.Instant;
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

        public Page<NotificationResponse> list(UUID userId, int page, int size) {
        int safeSize = Math.min(size, 50);
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, safeSize))
                .map(NotificationResponse::from);
    }

    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    public void markRead(UUID userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AccessDeniedException("Not allowed"));
        if (!userId.equals(n.getUserId())) {
            throw new AccessDeniedException("Not allowed");
        }
        n.markRead();
        notificationRepository.save(n);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId, Instant.now());
    }
}