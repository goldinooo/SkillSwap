package com.trendenden.backend.chat.service;

import com.trendenden.backend.chat.dto.MessageResponse;
import com.trendenden.backend.chat.dto.SendMessageRequest;
import com.trendenden.backend.chat.entity.Message;
import com.trendenden.backend.chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
   
class ChatServiceTest {

    private MessageRepository messageRepository;
    private SessionLookup sessionLookup;
    private ChatService chatService;
    private SimpMessagingTemplate messaging;

    private final UUID teacher = UUID.randomUUID();
    private final UUID learner = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        sessionLookup = mock(SessionLookup.class);
        messaging = mock(SimpMessagingTemplate.class);
        chatService = new ChatService(messageRepository, sessionLookup, messaging);
    }

    @Test
    void teacherCanSendMessage() {
        // 1. ARRANGE: Initiat
        when(sessionLookup.find(31L))
                .thenReturn(Optional.of(new ChatSession(31L, teacher, learner, "CONFIRMED")));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 2. ACT: call the methos test
        MessageResponse response = chatService.send(teacher, new SendMessageRequest(31L, "salam"));

        // 3. ASSERT: check the result
        assertEquals("salam", response.content());
        assertEquals(teacher, response.senderId());
        verify(messageRepository).save(any(Message.class));

        verify(messaging).convertAndSendToUser(learner.toString(), "/queue/messages", response);
        verify(messaging).convertAndSendToUser(teacher.toString(), "/queue/messages", response);
    }

    @Test
    void unknownSessionIsRefused() {
        // ARRANGE: session 42 ma kaynach
        when(sessionLookup.find(31L)).thenReturn(Optional.empty());

        // ACT + ASSERT: send khassha t'throwi
        assertThrows(AccessDeniedException.class,
                () -> chatService.send(teacher, new SendMessageRequest(31L, "salam")));

        // nothing saved
        verify(messageRepository, never()).save(any(Message.class));
        verify(messaging, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
    }

    @Test
    void strangerIsRefused() {
        // ARRANGE: session exists, but the sender is not in it
        when(sessionLookup.find(31L))
                .thenReturn(Optional.of(new ChatSession(31L, teacher, learner, "CONFIRMED")));

        // ACT + ASSERT
        assertThrows(AccessDeniedException.class,
                () -> chatService.send(stranger, new SendMessageRequest(31L, "salam")));

        verify(messageRepository, never()).save(any(Message.class));
        verify(messaging, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
    }

    @Test
    void closedSessionIsRefused() {
        // ARRANGE: the teacher is in the session, but it is COMPLETED
        when(sessionLookup.find(31L))
                .thenReturn(Optional.of(new ChatSession(31L, teacher, learner, "COMPLETED")));

        // ACT + ASSERT
        assertThrows(IllegalStateException.class,
                () -> chatService.send(teacher, new SendMessageRequest(31L, "salam")));

        verify(messageRepository, never()).save(any(Message.class));
            verify(messaging, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
    }

    @Test
    void learnerCanReadHistory() {
        when(sessionLookup.find(31L))
                .thenReturn(Optional.of(new ChatSession(31L, teacher, learner, "CONFIRMED")));
        Message m = new Message(31L, teacher, "salam");
        when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(31L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(m)));

        Page<MessageResponse> page = chatService.history(learner, 31L, 0, 20);

        assertEquals(1, page.getContent().size());
        assertEquals("salam", page.getContent().get(0).content());
    }

    @Test
    void strangerCannotReadHistory() {
        when(sessionLookup.find(31L))
                .thenReturn(Optional.of(new ChatSession(31L, teacher, learner, "CONFIRMED")));

        assertThrows(AccessDeniedException.class,
                () -> chatService.history(stranger, 31L, 0, 20));

        verify(messageRepository, never()).findBySessionIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class));
    }

    @Test
    void historySizeIsCappedAt50() {
        when(sessionLookup.find(31L))
                .thenReturn(Optional.of(new ChatSession(31L, teacher, learner, "CONFIRMED")));
        when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(31L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        chatService.history(teacher, 31L, 0, 1000);

        verify(messageRepository).findBySessionIdOrderByCreatedAtDesc(31L, PageRequest.of(0, 50));
    }
}