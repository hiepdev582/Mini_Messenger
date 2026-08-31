package com.hiepnn.mini_messeger.controller;

import com.hiepnn.mini_messeger.model.ChatMessage;
import com.hiepnn.mini_messeger.repository.ChatMessageRepository;
import com.hiepnn.mini_messeger.service.ChatService;
import com.hiepnn.mini_messeger.service.PresenceService;
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
    private final ChatMessageRepository messageRepository;
    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService,
                          ChatMessageRepository messageRepository,
                          PresenceService presenceService,
                          SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messageRepository = messageRepository;
        this.presenceService = presenceService;
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
        List<ChatMessage> history = messageRepository.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByIdAsc(
                senderId, recipientId, recipientId, senderId
        );
        return ResponseEntity.ok(history);
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        List<String> userIdList = headers.getNativeHeader("userId");
        if (userIdList != null && !userIdList.isEmpty()) {
            Long userId = Long.valueOf(userIdList.get(0));
            // Register socket session metadata attributes
            if (headers.getSessionAttributes() != null) {
                headers.getSessionAttributes().put("userId", userId);
            }
            presenceService.setOnline(userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        if (headers.getSessionAttributes() != null) {
            Long userId = (Long) headers.getSessionAttributes().get("userId");
            if (userId != null) {
                presenceService.setOffline(userId);
            }
        }
    }
}
