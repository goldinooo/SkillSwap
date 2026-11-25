package com.trendenden.backend.chat.controller;

import com.trendenden.backend.chat.dto.MessageResponse;
import com.trendenden.backend.chat.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class ChatHistoryController {

    private final ChatService chatService;

    public ChatHistoryController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/{sessionId}/messages")
    public Page<MessageResponse> history(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return chatService.history(userId, sessionId, page, size);
    }
}