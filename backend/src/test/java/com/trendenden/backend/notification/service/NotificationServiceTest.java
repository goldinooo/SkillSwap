package com.trendenden.backend.notification.service;

import com.trendenden.backend.notification.dto.NotificationResponse;
import com.trendenden.backend.notification.entity.Notification;
import com.trendenden.backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private SimpMessagingTemplate messaging;
    private NotificationService notificationService;

    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        messaging = mock(SimpMessagingTemplate.class);
        notificationService = new NotificationService(notificationRepository, messaging);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createSavesAndPushesToTheUser() {
        NotificationResponse response =
                notificationService.create(user, "NEW_MESSAGE", "{\"sessionId\":31}");

        assertEquals("NEW_MESSAGE", response.type());
        assertEquals("{\"sessionId\":31}", response.payload());
        verify(notificationRepository).save(any(Notification.class));
        verify(messaging).convertAndSendToUser(user.toString(), "/queue/notifications", response);
    }

    @Test
    void payloadCanBeNull() {
        NotificationResponse response = notificationService.create(user, "SESSION_CONFIRMED", null);

        assertNull(response.payload());
        verify(messaging).convertAndSendToUser(user.toString(), "/queue/notifications", response);
    }
}