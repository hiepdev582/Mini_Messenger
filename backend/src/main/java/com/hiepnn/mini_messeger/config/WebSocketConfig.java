package com.hiepnn.mini_messeger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;
import com.hiepnn.mini_messeger.service.PresenceService;
import java.security.Principal;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final PresenceService presenceService;

    public WebSocketConfig(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

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
        // Register raw STOMP endpoint with CORS allowed
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null) {
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        List<String> userIdHeader = accessor.getNativeHeader("userId");
                        if (userIdHeader != null && !userIdHeader.isEmpty()) {
                            String userId = userIdHeader.get(0);
                            accessor.setUser(() -> userId);
                            try {
                                presenceService.setOnline(Long.valueOf(userId));
                            } catch (Exception ignored) {}
                        }
                    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        Principal user = accessor.getUser();
                        if (user != null && user.getName() != null) {
                            try {
                                presenceService.setOffline(Long.valueOf(user.getName()));
                            } catch (Exception ignored) {}
                        }
                    }
                }
                return message;
            }
        });
    }
}
