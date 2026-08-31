package com.hiepnn.mini_messeger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Destination prefix to route messages from server to subscribed clients
        config.enableSimpleBroker("/topic", "/queue");
        // Destination prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // User prefix for one-to-one messaging
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register raw STOMP endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
