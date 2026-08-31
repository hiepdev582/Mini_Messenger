package com.hiepnn.mini_messeger.controller;

import com.hiepnn.mini_messeger.model.ChatMessage;
import com.hiepnn.mini_messeger.service.ChatService;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.util.List;

@Controller
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService,
                          SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    // Handles STOMP frame SEND destined for `/app/chat.sendMessage`
    @MessageMapping("/chat.sendMessage")
    public void receiveMessage(@Payload ChatMessage message) {
        ChatMessage saved = chatService.sendMessage(message);
        // Reply back directly to the sender node via WebSocket destination queue so they can display the message as SENT
        messagingTemplate.convertAndSendToUser(
                saved.getSenderId().toString(),
                "/queue/messages",
                saved
        );
    }

    @GetMapping("/api/chat/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@RequestParam Long senderId, @RequestParam Long recipientId) {
        return ResponseEntity.ok(chatService.getChatHistory(senderId, recipientId));
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        List<String> userIdList = headers.getNativeHeader("userId");
        if (userIdList != null && !userIdList.isEmpty()) {
            Long userId = Long.valueOf(userIdList.get(0));
            if (headers.getSessionAttributes() != null) {
                headers.getSessionAttributes().put("userId", userId);
            }
            chatService.handleSessionConnect(userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        if (headers.getSessionAttributes() != null) {
            Long userId = (Long) headers.getSessionAttributes().get("userId");
            chatService.handleSessionDisconnect(userId);
        }
    }
}
