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


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private SimpMessagingTemplate messaging;
    private NotificationService notificationService;

    private final UUID user = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();

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

        @Test
    void listReturnsDtosAndCapsSize() {
        Notification n = new Notification(user, "NEW_MESSAGE", null);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponse> page = notificationService.list(user, 0, 1000);

        assertEquals("NEW_MESSAGE", page.getContent().get(0).type());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(user, PageRequest.of(0, 50));
    }

    @Test
    void ownerCanMarkRead() {
        Notification n = new Notification(user, "NEW_MESSAGE", null);
        when(notificationRepository.findById(7L)).thenReturn(Optional.of(n));

        notificationService.markRead(user, 7L);

        assertNotNull(n.getReadAt());
        verify(notificationRepository).save(n);
    }

    @Test
    void strangerCannotMarkRead() {
        Notification n = new Notification(user, "NEW_MESSAGE", null);
        when(notificationRepository.findById(7L)).thenReturn(Optional.of(n));

        assertThrows(AccessDeniedException.class,
                () -> notificationService.markRead(stranger, 7L));

        assertNull(n.getReadAt());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void unknownNotificationIsRefused() {
        when(notificationRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> notificationService.markRead(user, 7L));
    }

    @Test
    void markAllReadUsesTheUser() {
        when(notificationRepository.markAllRead(eq(user), any(Instant.class))).thenReturn(3);

        int changed = notificationService.markAllRead(user);

        assertEquals(3, changed);
        verify(notificationRepository).markAllRead(eq(user), any(Instant.class));
    }
}