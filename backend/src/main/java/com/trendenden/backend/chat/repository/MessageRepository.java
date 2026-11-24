package com.trendenden.backend.chat.repository;

import com.trendenden.backend.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
}
