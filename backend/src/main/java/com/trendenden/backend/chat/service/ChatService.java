package com.trendenden.backend.chat.service;

import com.trendenden.backend.chat.repository.MessageRepository;
import org.springframework.stereotype.Service;
import com.trendenden.backend.chat.dto.MessageResponse;
import com.trendenden.backend.chat.dto.SendMessageRequest;
import org.springframework.security.access.AccessDeniedException;
import com.trendenden.backend.chat.entity.Message;
import java.util.UUID;
import java.util.Set;


@Service
public class ChatService{
    private final MessageRepository messageRepository;
    private final SessionLookup sessionLookup; ///need to check after from rretahi db | this is to know who is the teacher and the learner, and status
    private static final Set<String> CHAT_OPEN = Set.of("REQUESTED", "CONFIRMED");

    public ChatService(MessageRepository messageRepository, SessionLookup sessionLookup){
        this.messageRepository = messageRepository;
        this.sessionLookup = sessionLookup;
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
    
    return MessageResponse.from(saved);
    }
}