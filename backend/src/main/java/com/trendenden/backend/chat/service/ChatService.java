package com.trendenden.backend.chat.service;

import com.trendenden.backend.chat.repository.MessageRepository;
import org.springframework.stereotype.Service;
import com.trendenden.backend.chat.dto.MessageResponse;
import com.trendenden.backend.chat.dto.SendMessageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.trendenden.backend.chat.entity.Message;
import java.util.UUID;
import java.util.Set;


@Service
public class ChatService{
    private final MessageRepository messageRepository;
    private final SessionLookup sessionLookup; ///need to check DB after, from rretahi  | this is to know who is the teacher and the learner, and status
    private final SimpMessagingTemplate messaging;
    private static final Set<String> CHAT_OPEN = Set.of("REQUESTED", "CONFIRMED");

    public ChatService(MessageRepository messageRepository, SessionLookup sessionLookup, SimpMessagingTemplate messaging){
        this.messageRepository = messageRepository;
        this.sessionLookup = sessionLookup;
        this.messaging = messaging;
    }

    public MessageResponse send(UUID senderId, SendMessageRequest request){
        ChatSession session = sessionLookup.find(request.sessionId())
        .orElseThrow(() -> new AccessDeniedException("Not allowed"));

        boolean isTeacher = senderId.equals(session.teacherId());
        boolean isLearner = senderId.equals(session.learnerId());
            if(!isLearner && !isTeacher){
                throw new AccessDeniedException("Not allowed");
            }

        if(!CHAT_OPEN.contains(session.status())){
            throw new IllegalStateException("Chat is closed for this session !");
        }


        Message message = new Message(request.sessionId(), senderId, request.content());
        Message saved = messageRepository.save(message);
        MessageResponse response = MessageResponse.from(saved);

        UUID recipient = isTeacher ? session.learnerId() : session.teacherId();
        messaging.convertAndSendToUser(recipient.toString(), "/queue/messages", response);
        messaging.convertAndSendToUser(senderId.toString(), "/queue/messages", response);
       
        return response;
    }


}