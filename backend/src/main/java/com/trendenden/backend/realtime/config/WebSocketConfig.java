
package com.trendenden.backend.realtime.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import com.trendenden.backend.realtime.security.JwtChannelInterceptor;



@Configuration
@EnableWebSocketMessageBroker

public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
    private final JwtChannelInterceptor jwtChannelInterceptor;

    public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor){
        this.jwtChannelInterceptor = jwtChannelInterceptor;
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");// TODO: restrict to frontend origin from env
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry){
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration){
        registration.interceptors(jwtChannelInterceptor);
    }

} 