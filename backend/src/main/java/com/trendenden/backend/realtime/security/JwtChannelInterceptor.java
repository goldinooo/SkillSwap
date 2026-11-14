package com.trendenden.backend.realtime.security;

import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor{
    private final TokenService tokenService;

    public  JwtChannelInterceptor(TokenService tokenService){
        this.tokenService = tokenService;
    }

    @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel){
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // check CONNECT
        if ( accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())){
            return message;
        }

        // get the header "Authorization"
        String header = accessor.getFirstNativeHeader("Authorization");
        
        // check for bearer
        if(header == null || !header.startsWith("Bearer ")){
            throw new MessageDeliveryException("Missing or invalid Authorization header");
        }
        // remove Bearer
        String token = header.substring(7);
        // token and tokenSevice checking
        AuthenticatedUser user = tokenService.parse(token).orElseThrow(() -> new MessageDeliveryException("Invalid token"));
        // user and his id
        String userId = String.valueOf(user.id());
        accessor.setUser(()-> userId);

        return message;
        }
}

