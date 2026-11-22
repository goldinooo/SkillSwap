package com.trendenden.backend.chat.repository;

import com.trendenden.backend.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {}
