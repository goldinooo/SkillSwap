package com.trendenden.backend.notification.repository;

import com.trendenden.backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}