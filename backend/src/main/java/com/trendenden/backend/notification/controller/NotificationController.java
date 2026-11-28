package com.trendenden.backend.notification.controller;

import com.trendenden.backend.notification.dto.NotificationResponse;
import com.trendenden.backend.notification.service.NotificationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        return notificationService.list(userId(principal), page, size);
    }

    @GetMapping("/unread-count")
    public long unreadCount(Principal principal) {
        return notificationService.unreadCount(userId(principal));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Principal principal) {
        notificationService.markRead(userId(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Principal principal) {
        notificationService.markAllRead(userId(principal));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}