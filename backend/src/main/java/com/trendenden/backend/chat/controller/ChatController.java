package com.trendenden.backend.chat.controller;

import com.trendenden.backend.chat.service.ChatService;
import org.springframework.stereotype.Controller;
import com.trendenden.backend.chat.dto.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import java.security.Principal;
import java.util.UUID;


@Controller
public class ChatController{
    private final ChatService chatService;

    public ChatController(ChatService chatService){
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageRequest request, Principal principal){
        UUID senderId = UUID.fromString(principal.getName());
        chatService.send(senderId, request);
    }
}