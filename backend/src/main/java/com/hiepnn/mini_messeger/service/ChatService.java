package com.hiepnn.mini_messeger.service;

import com.hiepnn.mini_messeger.config.RedisConfig;
import com.hiepnn.mini_messeger.model.ChatMessage;
import com.hiepnn.mini_messeger.repository.ChatMessageRepository;
import com.hiepnn.mini_messeger.util.SnowflakeIdGenerator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatService {
    private final ChatMessageRepository messageRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisMessageListenerContainer redisContainer;
    private final PresenceService presenceService;
    private final ObjectMapper objectMapper;

    public ChatService(ChatMessageRepository messageRepository,
                       SnowflakeIdGenerator idGenerator,
                       RedisTemplate<String, Object> redisTemplate,
                       SimpMessagingTemplate messagingTemplate,
                       RedisMessageListenerContainer redisContainer,
                       PresenceService presenceService,
                       ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.idGenerator = idGenerator;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.redisContainer = redisContainer;
        this.presenceService = presenceService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // Subscribe this server node to its corresponding Redis Channel topic
        String currentServerChannel = "server:" + RedisConfig.getServerId();
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(this, "handleRedisEvent");
        listenerAdapter.afterPropertiesSet();
        redisContainer.addMessageListener(listenerAdapter, new ChannelTopic(currentServerChannel));
    }

    public ChatMessage sendMessage(ChatMessage message) {
        // Generate Snowflake unique ID
        message.setId(idGenerator.nextId());
        message.setTimestamp(System.currentTimeMillis());
        message.setStatus(ChatMessage.MessageStatus.SENT);

        // 1. Save to MongoDB
        ChatMessage savedMessage = messageRepository.save(message);

        // 2. Query Redis routing to find target server
        String targetServerId = presenceService.getUserServer(message.getRecipientId());

        if (targetServerId != null) {
            // 3. Publish to Redis channel of the target server
            String channel = "server:" + targetServerId;
            redisTemplate.convertAndSend(channel, savedMessage);
        }

        // Return copy for sender feedback loop
        return savedMessage;
    }

    public List<ChatMessage> getChatHistory(Long senderId, Long recipientId) {
        return messageRepository.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByIdAsc(
                senderId, recipientId, recipientId, senderId
        );
    }

    public void handleSessionConnect(Long userId) {
        if (userId != null) {
            presenceService.setOnline(userId);
        }
    }

    public void handleSessionDisconnect(Long userId) {
        if (userId != null) {
            presenceService.setOffline(userId);
        }
    }

    // Dynamic listener method matching adaptation reflection signature
    @SuppressWarnings("unused")
    public void handleRedisEvent(String messageString) {
        try {
            // Deserialize back to ChatMessage payload
            ChatMessage chatMessage = objectMapper.readValue(messageString, ChatMessage.class);
            // Push via STOMP subscription queue directly to the recipient client
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getRecipientId().toString(),
                    "/queue/messages",
                    chatMessage
            );
        } catch (IOException e) {
            throw new RuntimeException("Error parsing chat message payload from Redis", e);
        }
    }
}
