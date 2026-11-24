package com.trendenden.backend.chat.controller;

import com.trendenden.backend.chat.service.ChatService;
import org.springframework.stereotype.Controller;
import com.trendenden.backend.chat.dto.SendMessageRequest;
import jakarta.validation.Valid;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller
public class ChatController{
    private final ChatService chatService;
    private static final Logger log  = LoggerFactory.getLogger(ChatController.class);
    public ChatController(ChatService chatService){
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageRequest request, Principal principal){
        UUID senderId = UUID.fromString(principal.getName());
        chatService.send(senderId, request);
    }

    @MessageExceptionHandler(IllegalStateException.class)
    @SendToUser("/queue/errors")
    public String handleClosed(IllegalStateException e) {
        return e.getMessage();
    }

    @MessageExceptionHandler(AccessDeniedException.class)
    @SendToUser("/queue/errors")
    public String handleDenied(AccessDeniedException e) {
        return "Not allowed";
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public String handleInvalid(MethodArgumentNotValidException e) {
        return "Message must be 1-2000 characters";
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public String handleOther(Exception e) {
        log.error("chat.send failed", e);
        return "Something went wrong";
    }
}