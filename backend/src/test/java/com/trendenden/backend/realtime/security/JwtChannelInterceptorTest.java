package com.trendenden.backend.realtime.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class JwtChannelInterceptorTest {

    private final JwtChannelInterceptor interceptor =
            new JwtChannelInterceptor(new FakeTokenService());

    // Helper: builds a STOMP frame (CONNECT, SEND...) with an optional Authorization header
    private Message<byte[]> frame(StompCommand command, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connect_withValidToken_setsUserId() {
        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, "Bearer test-user-1"), null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertEquals("1", accessor.getUser().getName());
    }

    @Test
    void connect_withoutToken_isRefused() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null), null));
    }

    @Test
    void connect_withBadToken_isRefused() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer nope"), null));
    }

    @Test
    void connect_withoutBearerPrefix_isRefused() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, "test-user-1"), null));
    }

    @Test
    void otherFrames_passWithoutToken() {
        Message<byte[]> send = frame(StompCommand.SEND, null);

        Message<?> result = interceptor.preSend(send, null);

        assertSame(send, result);
        assertNull(StompHeaderAccessor.wrap(result).getUser());
    }
}
